package dev.cosgy.jmusicbot.util;

import java.io.*;
import java.net.http.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YtDlpManager {
    private static final Logger log = LoggerFactory.getLogger(YtDlpManager.class);

    private static final String GITHUB_LATEST_BASE =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/";
    private static final String SHA256_FILE = "SHA2-256SUMS";
    private static final int PROC_TIMEOUT_SEC = 120;

    // Default auto-update settings (24h; initial 5–30 min random delay for startup load balancing)
    private static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofDays(1);
    private static final boolean AUTO_UPDATE_ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("jmusicbot.ytdlp.autoUpdate", "true"));
    private static final String UPDATE_TO = System.getProperty("jmusicbot.ytdlp.updateTo", "").trim(); // "", "stable", "nightly", "2025.XX"

    // Ready flag and path (to prevent multiple executions)
    private static final AtomicBoolean prepared = new AtomicBoolean(false);
    private static volatile Path preparedPath = null;

    // Automatic Update Scheduler (Only one instance within the JVM)
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "yt-dlp-auto-update");
                t.setDaemon(true);
                return t;
            });
    private static volatile ScheduledFuture<?> updateFuture = null;
    private static final AtomicBoolean updatingNow = new AtomicBoolean(false);

    private final Path binDir;
    private final Path exePath;
    private final String assetName;

    public YtDlpManager(Path botDir) {
        log.debug("Initializing YtDlpManager: botDir={}", botDir);
        this.binDir = botDir.resolve("bin");
        this.assetName = pickAssetForCurrentPlatform();
        this.exePath = binDir.resolve(assetNameForLocal(assetName));
        log.debug("Executable file path: {}", exePath);
    }

    /** Prepare yt-dlp and return an executable path (download/verify/permission settings/self-update once) */
    public Path prepare() throws Exception {
        if (prepared.get() && preparedPath != null) {
            log.debug("yt-dlp is already prepared: {}", preparedPath);
            // If automatic updates are not running, start them.
            if (AUTO_UPDATE_ENABLED) startAutoUpdateIfNeeded(DEFAULT_UPDATE_INTERVAL);
            return preparedPath;
        }

        synchronized (YtDlpManager.class) {
            if (prepared.get() && preparedPath != null) {
                if (AUTO_UPDATE_ENABLED) startAutoUpdateIfNeeded(DEFAULT_UPDATE_INTERVAL);
                return preparedPath;
            }

            log.info("Begin preparing yt-dlp.");
            Files.createDirectories(binDir);
            log.debug("Create/check bin directory: {}", binDir);

            boolean needDownload = !Files.isRegularFile(exePath);
            if (!needDownload) {
                log.debug("Verifying the existing yt-dlp executable file...");
                if (!isExecutableOk(exePath)) {
                    log.warn("The existing yt-dlp is corrupted/unable to run. Attempting re-download.");
                    needDownload = true;
                }
            }

            if (needDownload) {
                log.info("Downloading yt-dlp...");
                downloadAndVerify();
                grantExecuteIfNeeded(exePath);
                log.info("Download and verification of yt-dlp have been completed.");
            } else {
                log.info("Use the existing yt-dlp.");
            }

            // Startup Verification
            log.debug("Checking the version of yt-dlp...");
            String version = runAndCapture(exePath.toString(), "--version").trim();
            if (version.isEmpty()) throw new IllegalStateException("yt-dlp launch failed");
            log.info("yt-dlp version: {}", version);

            // Manual self-update once only (continue even if it fails)
            try {
                log.info("Executing yt-dlp self-update...");
                runUpdateCommandWithTimeout(300, exePath);
                log.info("The self-update for yt-dlp has completed.");
            } catch (Exception e) {
                log.warn("Failed to update yt-dlp, but continuing: {}", e.getMessage());
            }

            preparedPath = exePath;
            prepared.set(true);

            // Start automatic update
            if (AUTO_UPDATE_ENABLED) startAutoUpdateIfNeeded(DEFAULT_UPDATE_INTERVAL);
            else log.info("yt-dlp auto-update is disabled (-Djmusicbot.ytdlp.autoUpdate=false)");

            log.info("yt-dlp setup is complete: {}", exePath);
            return exePath;
        }
    }

    // ————— Automatic Update Control —————

    /** Automatically start updating at the default interval (24 hours). If already started, do nothing. */
    public synchronized void startAutoUpdate() {
        startAutoUpdateIfNeeded(DEFAULT_UPDATE_INTERVAL);
    }

    /** Start automatic updates at specified intervals (e.g., Duration.ofHours(6)). If already running, do nothing. */
    public synchronized void startAutoUpdate(Duration interval) {
        startAutoUpdateIfNeeded(interval != null ? interval : DEFAULT_UPDATE_INTERVAL);
    }

    /** Stop automatic updates */
    public synchronized void stopAutoUpdate() {
        if (updateFuture != null) {
            updateFuture.cancel(false);
            updateFuture = null;
            log.info("yt-dlp auto-update has been stopped.");
        }
    }

    private void startAutoUpdateIfNeeded(Duration interval) {
        if (updateFuture != null && !updateFuture.isCancelled()) return;

        long periodSec = Math.max(60, interval.getSeconds()); // Minimum 60 seconds
        long initialDelaySec = ThreadLocalRandom.current().nextLong(300, 1800); // 5 to 30 minutes dispersed

        // Post-shutdown cleanup
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stopAutoUpdate, "yt-dlp-auto-update-shutdown"));
        } catch (IllegalStateException ignored) { /* Already ended */ }

        updateFuture = SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                if (!prepared.get()) {
                    // If not yet ready, prepare (without conflicting with other threads)
                    try { prepare(); } catch (Exception e) {
                        log.warn("Failed to prepare for automatic renewal: {}", e.toString());
                        return;
                    }
                }
                performSelfUpdate(); // Actual processing
            } catch (Throwable t) {
                log.warn("Error in yt-dlp auto-update loop: {}", t.toString());
            }
        }, initialDelaySec, periodSec, TimeUnit.SECONDS);

        log.info("yt-dlp auto-update started: Initial delay = {} seconds, Cycle = {} seconds", initialDelaySec, periodSec);
    }

    /** Actual self-update (with concurrency control) */
    private void performSelfUpdate() {
        if (!updatingNow.compareAndSet(false, true)) {
            log.debug("Skip because another update process is running.");
            return;
        }
        try {
            Path target = (preparedPath != null) ? preparedPath : exePath;
            if (!Files.isRegularFile(target)) {
                log.warn("The target yt-dlp for update could not be found: {}", target);
                return;
            }
            log.info("[yt-dlp] Performing automatic update check...");
            runUpdateCommandWithTimeout(600, target); // Auto-renewal is set with a buffer of up to 600 seconds.
        } catch (Exception e) {
            log.warn("yt-dlp auto-update failed: {}", e.toString());
        } finally {
            updatingNow.set(false);
        }
    }

    private void runUpdateCommandWithTimeout(int timeoutSec, Path exe) throws Exception {
        if (UPDATE_TO.isEmpty()) {
            runAndCaptureWithProgressTimeout(timeoutSec, exe.toString(), "-U");
        } else {
            runAndCaptureWithProgressTimeout(timeoutSec, exe.toString(), "--update-to", UPDATE_TO);
        }
    }

    // ————— helpers —————

    /** Select an asset name suitable for the current platform */
    private static String pickAssetForCurrentPlatform() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        log.debug("Detecting platform: OS={}, Arch={}", os, arch);
        if (os.contains("win")) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                log.debug("Select Windows ARM64 Edition");
                return "yt-dlp_arm64.exe";
            }
            log.debug("Select Windows version");
            return "yt-dlp.exe";
        } else if (os.contains("mac") || os.contains("darwin")) {
            log.debug("Select macOS version");
            return "yt-dlp_macos";
        } else {
            log.debug("Select Linux version");
            return "yt-dlp_linux";
        }
    }

    /** Local Deployment Name (Fixed) */
    private static String assetNameForLocal(String asset) {
        return asset.endsWith(".exe") ? "yt-dlp.exe" : "yt-dlp";
    }

    /** Download & SHA256 Verification & Deployment */
    private void downloadAndVerify() throws Exception {
        log.info("Start downloading yt-dlp: {}", assetName);
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

        // 1) Download the main program
        URI binUri = URI.create(GITHUB_LATEST_BASE + assetName);
        log.debug("Download URL: {}", binUri);
        Path tmp = Files.createTempFile("yt-dlp-", ".dl");
        log.debug("Temporary file: {}", tmp);

        HttpResponse<InputStream> response = client.send(
                HttpRequest.newBuilder(binUri).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );
        long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1);

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            downloadWithProgress(in, out, totalBytes);
            log.info("Download complete");
        }

        // 2) SHA256 verification
        log.info("Verifying SHA256 checksum...");
        URI sumsUri = URI.create(GITHUB_LATEST_BASE + SHA256_FILE);
        String sums = client.send(HttpRequest.newBuilder(sumsUri).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        String expected = parseSha256ForAsset(sums, assetName);

        if (expected != null) {
            log.debug("Expected SHA256: {}", expected);
            String actual = sha256Hex(tmp);
            log.debug("Actual SHA256: {}", actual);
            if (!expected.equalsIgnoreCase(actual)) {
                Files.deleteIfExists(tmp);
                log.error("SHA256 mismatch. Expected: {}, Actual: {}", expected, actual);
                throw new SecurityException("SHA-256 mismatch for " + assetName);
            }
            log.info("✓ SHA256 verification successful");
        } else {
            log.warn("The SHA256 checksum could not be found, so verification is skipped.");
        }

        // 3) Placement
        log.debug("Move yt-dlp to final location: {} -> {}", tmp, exePath);
        Files.move(tmp, exePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        grantExecuteIfNeeded(exePath);
        log.info("The deployment of yt-dlp is complete.");
    }

    /** Download with progress indicator */
    private void downloadWithProgress(InputStream in, OutputStream out, long totalBytes) throws IOException {
        byte[] buffer = new byte[8192];
        long downloadedBytes = 0;
        int bytesRead;
        long lastProgressTime = System.currentTimeMillis();
        int lastProgress = -1;

        log.info("Download started: {} (Size: {})", "yt-dlp", formatBytes(totalBytes));

        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            downloadedBytes += bytesRead;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastProgressTime >= 500) {
                if (totalBytes > 0) {
                    int progress = (int) ((downloadedBytes * 100) / totalBytes);
                    if (progress != lastProgress) {
                        String progressBar = createProgressBar(downloadedBytes, totalBytes);
                        log.info("Download progress: {} {}% ({}/{})",
                                progressBar, progress, formatBytes(downloadedBytes), formatBytes(totalBytes));
                        lastProgress = progress;
                    }
                } else {
                    log.info("Download progress: {} (Size unknown)", formatBytes(downloadedBytes));
                }
                lastProgressTime = currentTime;
            }
        }

        if (totalBytes > 0) {
            log.info("Download complete: 100% ({}/{})", formatBytes(downloadedBytes), formatBytes(totalBytes));
        } else {
            log.info("Download complete: {}", formatBytes(downloadedBytes));
        }
    }

    private String createProgressBar(long current, long total) {
        if (total <= 0) return "[??????????]";
        int barLength = 20;
        int filled = (int) ((current * barLength) / total);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) bar.append("=");
            else if (i == filled) bar.append(">");
            else bar.append(" ");
        }
        bar.append("]");
        return bar.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static String parseSha256ForAsset(String sumsText, String asset) {
        log.debug("Searching for the hash of {} from SHA256SUMS", asset);
        for (String line : sumsText.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            int sp = t.indexOf(' ');
            if (sp > 0) {
                String hash = t.substring(0, sp).trim();
                String file = t.substring(t.lastIndexOf(' ') + 1).trim();
                if (file.equals(asset)) {
                    log.debug("Matching hash found: {}", hash);
                    return hash;
                }
            }
        }
        log.debug("The hash for {} was not found.", asset);
        return null;
    }

    private static String sha256Hex(Path p) throws Exception {
        log.debug("Calculating SHA256: {}", p);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) md.update(buf, 0, r);
        }
        byte[] b = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static void grantExecuteIfNeeded(Path p) throws IOException {
        try {
            log.debug("Granting execution permissions: {}", p);
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(p);
            if (!perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(p, perms);
                log.debug("✓ Granted execution permissions");
            } else {
                log.debug("You already have execution privileges.");
            }
        } catch (UnsupportedOperationException ignored) {
            log.debug("Non-POSIX permissions support (Windows, etc.)");
        }
    }

    private static boolean isExecutableOk(Path exe) {
        log.debug("Verifying executable file operation: {}", exe);
        try {
            String out = runAndCapture(exe.toString(), "--version");
            boolean ok = !out.isBlank();
            log.debug("Operation Verification Result: {}", ok ? "OK" : "NG");
            return ok;
        } catch (Exception e) {
            log.debug("Exception occurred during operation verification: {}", e.getMessage());
            return false;
        }
    }

    private static String runAndCapture(String... cmd) throws Exception {
        log.debug("Command execution: {}", Arrays.toString(cmd));
        Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream in = proc.getInputStream()) {
            in.transferTo(bos);
        }
        if (!proc.waitFor(PROC_TIMEOUT_SEC, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            log.error("The process timed out: {}", Arrays.toString(cmd));
            throw new RuntimeException("Process timeout: " + Arrays.toString(cmd));
        }
        String output = bos.toString(StandardCharsets.UTF_8);
        log.debug("Command execution complete. Output size: {} bytes", output.length());
        return output;
    }

    /** Legacy API (default=120 seconds) */
    private static String runAndCaptureWithProgress(String... cmd) throws Exception {
        return runAndCaptureWithProgressTimeout(PROC_TIMEOUT_SEC, cmd);
    }

    /** Timeout-specified */
    private static String runAndCaptureWithProgressTimeout(int timeoutSec, String... cmd) throws Exception {
        log.debug("Command execution: {}", Arrays.toString(cmd));
        Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (!line.trim().isEmpty()) log.info("  {}", line.trim());
            }
        }

        if (!proc.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            log.error("The process timed out: {}", Arrays.toString(cmd));
            throw new RuntimeException("Process timeout: " + Arrays.toString(cmd));
        }

        int exitCode = proc.exitValue();
        if (exitCode != 0) {
            log.warn("The command ended with the exit code {}.", exitCode);
        }

        log.debug("Command execution complete. Output size: {} bytes", output.length());
        return output.toString();
    }
}
