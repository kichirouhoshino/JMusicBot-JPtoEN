package com.jagrosh.jmusicbot.gui;

import com.jagrosh.jmusicbot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class PlaylistManagerPanel extends JPanel {
    private final Bot bot;
    private final List<ScopeEditorPanel> scopedPanels = new ArrayList<>();

    public PlaylistManagerPanel(Bot bot) {
        super(new BorderLayout(10, 10));
        this.bot = bot;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTabbedPane tabs = new JTabbedPane();
        ScopeEditorPanel playlistPanel = new ScopeEditorPanel(ScopeType.PLAYLIST);
        ScopeEditorPanel mylistPanel = new ScopeEditorPanel(ScopeType.MYLIST);
        ScopeEditorPanel publicPanel = new ScopeEditorPanel(ScopeType.PUBLIC);
        scopedPanels.add(playlistPanel);
        scopedPanels.add(mylistPanel);
        scopedPanels.add(publicPanel);

        tabs.addTab("Playlist", playlistPanel);
        tabs.addTab("Mylist", mylistPanel);
        tabs.addTab("Public List", publicPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void onBotConnected() {
        for (ScopeEditorPanel panel : scopedPanels) {
            panel.handleBotConnected();
        }
    }

    private enum ScopeType {
        PLAYLIST,
        MYLIST,
        PUBLIC
    }

    private class ScopeEditorPanel extends JPanel {
        private final ScopeType scopeType;
        private final DefaultListModel<String> nameModel;
        private final JList<String> nameList;
        private final JTextArea editor;
        private final JLabel statusLabel;
        private final JProgressBar progressBar;
        private final JButton refreshButton;
        private final JButton createButton;
        private final JButton deleteButton;
        private final JButton saveButton;
        private final JLabel selectedLabel;
        private final JComboBox<TargetOption> targetCombo;
        private final JTextField targetSearchField;
        private final JButton clearSearchButton;
        private final JLabel selectedTargetLabel;
        private final List<TargetOption> allTargetOptions = new ArrayList<>();
        private final Map<String, String> resolvedUserNameCache = new HashMap<>();

        private boolean updatingList;
        private boolean updatingTargetCombo;

        ScopeEditorPanel(ScopeType scopeType) {
            super(new BorderLayout(10, 10));
            this.scopeType = scopeType;
            this.nameModel = new DefaultListModel<>();
            this.nameList = new JList<>(nameModel);
            this.editor = new JTextArea();
            this.statusLabel = new JLabel("Ready");
            this.progressBar = new JProgressBar();
            this.refreshButton = new JButton(scopeListLoadButtonLabel(scopeType));
            this.createButton = new JButton("Create New");
            this.deleteButton = new JButton("Delete");
            this.saveButton = new JButton("Save");
            this.selectedLabel = new JLabel("Not Selected");
            this.targetCombo = new JComboBox<>();
            this.targetSearchField = new JTextField(16);
            this.clearSearchButton = new JButton("Clear Filter");
            this.selectedTargetLabel = new JLabel("Target Not Selected");

            buildUI();
            installListeners();
            if (scopeType == ScopeType.PUBLIC) {
                refreshNamesAsync();
            } else {
                statusLabel.setText("Candidates will be loaded after bot connection.");
            }
        }

        private void buildUI() {
            JPanel top = new JPanel(new BorderLayout(8, 8));
            top.add(createScopeHeader(), BorderLayout.NORTH);
            top.add(createToolbar(), BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createListPane(), createEditorPane());
            splitPane.setResizeWeight(0.32);

            JPanel bottom = new JPanel(new BorderLayout(8, 8));
            progressBar.setIndeterminate(true);
            progressBar.setVisible(false);
            progressBar.setStringPainted(true);
            progressBar.setString("Processing...");
            progressBar.setPreferredSize(new Dimension(220, 28));
            bottom.add(statusLabel, BorderLayout.WEST);
            bottom.add(progressBar, BorderLayout.EAST);

            add(top, BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        private JPanel createScopeHeader() {
            JPanel header = new JPanel(new BorderLayout(8, 8));
            header.setBorder(createSectionBorder());
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            switch (scopeType) {
                case PLAYLIST -> {
                    controls.add(new JLabel("Target Server"));
                    targetCombo.setEditable(false);
                    targetCombo.setPreferredSize(new Dimension(380, 40));
                    targetCombo.setFont(targetCombo.getFont().deriveFont(Font.PLAIN, 14f));
                    targetCombo.setMaximumRowCount(18);
                    targetCombo.setRenderer(createTargetRenderer());
                    controls.add(targetCombo);
                    controls.add(new JLabel("Search"));
                    styleFilterField(targetSearchField);
                    controls.add(targetSearchField);
                    styleActionButton(clearSearchButton);
                    controls.add(clearSearchButton);
                    JButton loadGuilds = new JButton("Load Connected Server Candidates");
                    styleActionButton(loadGuilds);
                    loadGuilds.addActionListener(e -> refreshGuildCandidates());
                    controls.add(loadGuilds);
                }
                case MYLIST -> {
                    controls.add(new JLabel("Target User"));
                    targetCombo.setEditable(false);
                    targetCombo.setPreferredSize(new Dimension(380, 40));
                    targetCombo.setFont(targetCombo.getFont().deriveFont(Font.PLAIN, 14f));
                    targetCombo.setMaximumRowCount(18);
                    targetCombo.setRenderer(createTargetRenderer());
                    controls.add(targetCombo);
                    controls.add(new JLabel("Search"));
                    styleFilterField(targetSearchField);
                    controls.add(targetSearchField);
                    styleActionButton(clearSearchButton);
                    controls.add(clearSearchButton);
                    JButton loadUsers = new JButton("Load User List");
                    styleActionButton(loadUsers);
                    loadUsers.addActionListener(e -> refreshUserCandidatesAsync());
                    controls.add(loadUsers);
                }
                case PUBLIC -> controls.add(new JLabel("Public lists are shared with everyone"));
            }
            selectedTargetLabel.setFont(selectedTargetLabel.getFont().deriveFont(Font.PLAIN, 13f));
            selectedTargetLabel.setForeground(new Color(80, 80, 80));
            selectedTargetLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
            header.add(controls, BorderLayout.NORTH);
            if (scopeType != ScopeType.PUBLIC) {
                header.add(selectedTargetLabel, BorderLayout.SOUTH);
            }
            return header;
        }

        private JPanel createToolbar() {
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            toolbar.setBorder(createSectionBorder());
            styleActionButton(refreshButton);
            styleActionButton(createButton);
            styleActionButton(deleteButton);
            toolbar.add(refreshButton);
            toolbar.add(createButton);
            toolbar.add(deleteButton);
            return toolbar;
        }

        private JPanel createListPane() {
            JPanel pane = new JPanel(new BorderLayout(6, 6));
            pane.setBorder(createSectionBorder());
            nameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            nameList.setFont(nameList.getFont().deriveFont(Font.PLAIN, 14f));
            pane.add(new JScrollPane(nameList), BorderLayout.CENTER);
            return pane;
        }

        private JPanel createEditorPane() {
            JPanel pane = new JPanel(new BorderLayout(6, 6));
            pane.setBorder(createSectionBorder());

            JPanel top = new JPanel(new BorderLayout(8, 0));
            selectedLabel.setFont(selectedLabel.getFont().deriveFont(Font.BOLD, 14f));
            stylePrimaryButton(saveButton);
            top.add(new JLabel("Editing"), BorderLayout.WEST);
            top.add(selectedLabel, BorderLayout.CENTER);
            top.add(saveButton, BorderLayout.EAST);

            editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            editor.setTabSize(2);
            editor.setLineWrap(false);
            editor.setWrapStyleWord(false);
            editor.setText("# Write one URL or search query per line\n# Add #shuffle at the top to load shuffled\n");

            pane.add(top, BorderLayout.NORTH);
            pane.add(new JScrollPane(editor), BorderLayout.CENTER);
            return pane;
        }

        private void installListeners() {
            refreshButton.addActionListener(e -> refreshNamesAsync());
            createButton.addActionListener(e -> createPlaylistAsync());
            deleteButton.addActionListener(e -> deleteSelectedAsync());
            saveButton.addActionListener(e -> saveSelectedAsync());
            nameList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && !updatingList) {
                    loadSelectedAsync();
                }
            });
            if (scopeType != ScopeType.PUBLIC) {
                targetCombo.addActionListener((ActionEvent e) -> {
                    if (!updatingTargetCombo) {
                        updateSelectedTargetLabel();
                        refreshNamesAsync();
                    }
                });
                clearSearchButton.addActionListener(e -> {
                    targetSearchField.setText("");
                    applyTargetFilter();
                });
                targetSearchField.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        applyTargetFilter();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        applyTargetFilter();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        applyTargetFilter();
                    }
                });
            }
        }

        private void handleBotConnected() {
            if (scopeType == ScopeType.PLAYLIST) {
                refreshGuildCandidates();
                refreshNamesAsync();
            } else if (scopeType == ScopeType.MYLIST) {
                refreshUserCandidatesAsync();
            } else {
                refreshNamesAsync();
            }
        }

        private void refreshGuildCandidates() {
            List<TargetOption> candidates = new ArrayList<>();
            if (bot.getJDA() != null) {
                for (Guild guild : bot.getJDA().getGuilds()) {
                    candidates.add(new TargetOption(guild.getId(), guild.getName(), false));
                }
            }
            candidates.sort(Comparator.comparing(TargetOption::label, String.CASE_INSENSITIVE_ORDER));
            allTargetOptions.clear();
            allTargetOptions.addAll(candidates);
            applyTargetFilter();
            if (!allTargetOptions.isEmpty()) {
                statusLabel.setText("Please select a target server.");
            } else {
                statusLabel.setText("No joined servers found.");
            }
            updateSelectedTargetLabel();
        }

        private void refreshUserCandidatesAsync() {
            setLoading(true, "Loading user list...");
            new SwingWorker<List<TargetOption>, Void>() {
                private String ownerId;

                @Override
                protected List<TargetOption> doInBackground() {
                    ownerId = String.valueOf(bot.getConfig().getOwnerId());
                    return loadUserTargetOptions(ownerId);
                }

                @Override
                protected void done() {
                    boolean shouldLoadNames = false;
                    try {
                        List<TargetOption> options = get();
                        allTargetOptions.clear();
                        allTargetOptions.addAll(options);
                        applyTargetFilter();

                        if (!allTargetOptions.isEmpty()) {
                            TargetOption ownerOption = allTargetOptions.stream()
                                    .filter(o -> o.id().equals(ownerId))
                                    .findFirst()
                                    .orElse(allTargetOptions.get(0));
                            targetCombo.setSelectedItem(ownerOption);
                            statusLabel.setText("Please select a target user.");
                            shouldLoadNames = true;
                        } else {
                            statusLabel.setText("No user candidates found.");
                        }
                        updateSelectedTargetLabel();
                    } catch (Exception ex) {
                        statusLabel.setText("Failed to load user candidates: " + ex.getMessage());
                    } finally {
                        setLoading(false, null);
                        if (shouldLoadNames) {
                            refreshNamesAsync();
                        }
                    }
                }
            }.execute();
        }

        private List<TargetOption> loadUserTargetOptions(String ownerId) {
            Set<String> mylistOwnerIds = getExistingMylistOwnerIds();
            Map<String, TargetOption> optionsById = new LinkedHashMap<>();

            // 1) Prioritize existing mylist owners as candidates (maintain candidates even if Discord cache is insufficient)
            for (String userId : mylistOwnerIds) {
                optionsById.put(userId, new TargetOption(userId, formatUserLabel(userId, null, true), true));
            }

            // 2) Integrate users already cached by Discord
            if (bot.getJDA() != null) {
                for (User user : bot.getJDA().getUsers()) {
                    boolean hasMylist = mylistOwnerIds.contains(user.getId());
                    resolvedUserNameCache.put(user.getId(), user.getName());
                    optionsById.put(user.getId(), new TargetOption(
                            user.getId(),
                            formatUserLabel(user.getId(), user.getName(), hasMylist),
                            hasMylist
                    ));
                }
            }

            // 3) API supplement: Get usernames for IDs not in cache (mylist owners + Owner)
            Set<String> unresolvedIds = new HashSet<>(mylistOwnerIds);
            unresolvedIds.add(ownerId);
            unresolvedIds.removeIf(optionsById::containsKey);
            if (bot.getJDA() != null) {
                for (String unresolvedId : unresolvedIds) {
                    String resolved = resolvedUserNameCache.get(unresolvedId);
                    if (resolved == null) {
                        try {
                            User user = bot.getJDA().retrieveUserById(unresolvedId).complete();
                            if (user != null) {
                                resolved = user.getName();
                                resolvedUserNameCache.put(unresolvedId, resolved);
                            }
                        } catch (Exception ignored) {
                            // If username cannot be obtained, continue displaying based on ID
                        }
                    }
                    boolean hasMylist = mylistOwnerIds.contains(unresolvedId);
                    optionsById.put(unresolvedId, new TargetOption(
                            unresolvedId,
                            formatUserLabel(unresolvedId, resolved, hasMylist),
                            hasMylist
                    ));
                }
            }

            // 4) Always keep Owner as a candidate
            if (!optionsById.containsKey(ownerId)) {
                boolean hasMylist = mylistOwnerIds.contains(ownerId);
                String ownerName = resolvedUserNameCache.get(ownerId);
                optionsById.put(ownerId, new TargetOption(
                        ownerId,
                        formatUserLabel(ownerId, ownerName, hasMylist),
                        hasMylist
                ));
            }

            List<TargetOption> options = new ArrayList<>();
            options.addAll(optionsById.values());
            options.sort(
                    Comparator.comparing(TargetOption::hasMylist).reversed()
                            .thenComparing(TargetOption::label, String.CASE_INSENSITIVE_ORDER)
            );
            return options;
        }

        private void applyTargetFilter() {
            if (scopeType == ScopeType.PUBLIC) {
                return;
            }
            String filter = targetSearchField.getText() == null ? "" : targetSearchField.getText().trim().toLowerCase();
            TargetOption selected = (TargetOption) targetCombo.getSelectedItem();
            updatingTargetCombo = true;
            try {
                targetCombo.removeAllItems();
                for (TargetOption option : allTargetOptions) {
                    if (filter.isEmpty() || option.label().toLowerCase().contains(filter)) {
                        targetCombo.addItem(option);
                    }
                }
                if (targetCombo.getItemCount() == 0) {
                    statusLabel.setText("No targets match the search criteria.");
                    updateSelectedTargetLabel();
                    return;
                }
                if (selected != null) {
                    for (int i = 0; i < targetCombo.getItemCount(); i++) {
                        TargetOption option = targetCombo.getItemAt(i);
                        if (option.id().equals(selected.id())) {
                            targetCombo.setSelectedItem(option);
                            return;
                        }
                    }
                }
                targetCombo.setSelectedIndex(0);
            } finally {
                updatingTargetCombo = false;
            }
            updateSelectedTargetLabel();
        }

        private String currentTargetIdOrEmpty() {
            if (scopeType == ScopeType.PUBLIC) {
                return "";
            }
            TargetOption selected = (TargetOption) targetCombo.getSelectedItem();
            if (selected == null) {
                return "";
            }
            return selected.id();
        }

        private void refreshNamesAsync() {
            if (!validateTargetIfNeeded()) {
                return;
            }
            setLoading(true, "Loading list...");
            new SwingWorker<List<String>, Void>() {
                @Override
                protected List<String> doInBackground() {
                    return listNames();
                }

                @Override
                protected void done() {
                    try {
                        List<String> names = get();
                        applyNameList(names);
                        statusLabel.setText("List updated: " + names.size() + " items");
                    } catch (Exception ex) {
                        statusLabel.setText("Failed to update list: " + ex.getMessage());
                    } finally {
                        setLoading(false, null);
                    }
                }
            }.execute();
        }

        private List<String> listNames() {
            String targetId = currentTargetIdOrEmpty();
            List<String> names;
            switch (scopeType) {
                case PLAYLIST -> names = bot.getPlaylistLoader().getPlaylistNames(targetId);
                case MYLIST -> names = bot.getMylistLoader().getPlaylistNames(targetId);
                case PUBLIC -> names = bot.getPublistLoader().getPlaylistNames();
                default -> names = Collections.emptyList();
            }
            names.sort(String::compareToIgnoreCase);
            return names;
        }

        private void applyNameList(List<String> names) {
            boolean shouldLoadSelected = false;
            updatingList = true;
            try {
                String prev = nameList.getSelectedValue();
                nameModel.clear();
                names.forEach(nameModel::addElement);
                if (prev != null && names.contains(prev)) {
                    nameList.setSelectedValue(prev, true);
                    shouldLoadSelected = true;
                } else if (!names.isEmpty()) {
                    nameList.setSelectedIndex(0);
                    shouldLoadSelected = true;
                } else {
                    selectedLabel.setText("未選択");
                    editor.setText("");
                }
            } finally {
                updatingList = false;
            }
            if (shouldLoadSelected && nameList.getSelectedValue() != null) {
                SwingUtilities.invokeLater(this::loadSelectedAsync);
            }
        }

        private void loadSelectedAsync() {
            String name = nameList.getSelectedValue();
            if (name == null) {
                selectedLabel.setText("未選択");
                return;
            }
            setLoading(true, "Loading content...");
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    Path path = filePathOf(name);
                    if (Files.exists(path)) {
                        return Files.readString(path, StandardCharsets.UTF_8);
                    }
                    return "";
                }

                @Override
                protected void done() {
                    try {
                        editor.setText(get());
                        editor.setCaretPosition(0);
                        selectedLabel.setText(name);
                        statusLabel.setText("Content loaded: " + name);
                    } catch (Exception ex) {
                        statusLabel.setText("Failed to load: " + ex.getMessage());
                    } finally {
                        setLoading(false, null);
                    }
                }
            }.execute();
        }

        private void createPlaylistAsync() {
            if (!validateTargetIfNeeded()) {
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Enter a new list name");
            if (name == null) {
                return;
            }
            String normalized = normalizeName(name);
            if (normalized.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a valid list name.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            setLoading(true, "Creating new...");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String targetId = currentTargetIdOrEmpty();
                    switch (scopeType) {
                        case PLAYLIST -> bot.getPlaylistLoader().createPlaylist(targetId, normalized);
                        case MYLIST -> bot.getMylistLoader().createPlaylist(targetId, normalized);
                        case PUBLIC -> bot.getPublistLoader().createPlaylist(normalized);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusLabel.setText("Created: " + normalized);
                        refreshNamesAsync();
                        SwingUtilities.invokeLater(() -> nameList.setSelectedValue(normalized, true));
                    } catch (Exception ex) {
                        statusLabel.setText("Failed to create: " + ex.getMessage());
                    } finally {
                        setLoading(false, null);
                    }
                }
            }.execute();
        }

        private void deleteSelectedAsync() {
            String name = nameList.getSelectedValue();
            if (name == null) {
                JOptionPane.showMessageDialog(this, "Please select a list to delete.", "Not Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this, "Delete '" + name + "'? Are you sure?", "Delete List", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
            setLoading(true, "Deleting...");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String targetId = currentTargetIdOrEmpty();
                    switch (scopeType) {
                        case PLAYLIST -> bot.getPlaylistLoader().deletePlaylist(targetId, name);
                        case MYLIST -> bot.getMylistLoader().deletePlaylist(targetId, name);
                        case PUBLIC -> bot.getPublistLoader().deletePlaylist(name);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusLabel.setText("Deleted: " + name);
                        refreshNamesAsync();
                    } catch (Exception ex) {
                        statusLabel.setText("Failed to delete: " + ex.getMessage());
                    } finally {
                        setLoading(false, null);
                    }
                }
            }.execute();
        }

        private void saveSelectedAsync() {
            String name = nameList.getSelectedValue();
            if (name == null) {
                JOptionPane.showMessageDialog(this, "Please select a list to save.", "Not Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            setLoading(true, "Saving...");
            String text = editor.getText();
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String targetId = currentTargetIdOrEmpty();
                    switch (scopeType) {
                        case PLAYLIST -> bot.getPlaylistLoader().writePlaylist(targetId, name, text);
                        case MYLIST -> bot.getMylistLoader().writePlaylist(targetId, name, text);
                        case PUBLIC -> bot.getPublistLoader().writePlaylist(name, text);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusLabel.setText("Saved: " + name);
                    } catch (Exception ex) {
                        statusLabel.setText("Failed to save: " + ex.getMessage());
                    } finally {
                        setLoading(false, null);
                    }
                }
            }.execute();
        }

        private Path filePathOf(String name) {
            String targetId = currentTargetIdOrEmpty();
            return switch (scopeType) {
                case PLAYLIST -> Paths.get(bot.getConfig().getPlaylistsFolder(), targetId, name + ".txt");
                case MYLIST -> Paths.get(bot.getConfig().getMylistfolder(), targetId, name + ".txt");
                case PUBLIC -> Paths.get(bot.getConfig().getPublistFolder(), name + ".txt");
            };
        }

        private String normalizeName(String input) {
            String trimmed = input == null ? "" : input.trim();
            if (trimmed.isEmpty()) {
                return "";
            }
            return trimmed.replace("/", "_").replace("\\", "_").replace(":", "_");
        }

        private boolean validateTargetIfNeeded() {
            if (scopeType == ScopeType.PUBLIC) {
                return true;
            }
            String targetId = currentTargetIdOrEmpty();
            if (targetId.isBlank()) {
                if (scopeType == ScopeType.PLAYLIST) {
                    statusLabel.setText("No joined servers, cannot select a target.");
                } else {
                    statusLabel.setText("No user candidates, cannot select a target.");
                }
                return false;
            }
            return true;
        }

        private void setLoading(boolean loading, String status) {
            progressBar.setVisible(loading);
            refreshButton.setEnabled(!loading);
            createButton.setEnabled(!loading);
            deleteButton.setEnabled(!loading);
            saveButton.setEnabled(!loading);
            nameList.setEnabled(!loading);
            editor.setEnabled(!loading);
            if (scopeType != ScopeType.PUBLIC) {
                targetCombo.setEnabled(!loading);
            }
            if (status != null) {
                statusLabel.setText(status);
            }
        }

        private void styleActionButton(AbstractButton button) {
            button.setFont(button.getFont().deriveFont(Font.PLAIN, 14f));
            button.setMargin(new Insets(10, 14, 10, 14));
            button.setPreferredSize(new Dimension(Math.max(button.getPreferredSize().width, 130), 40));
        }

        private void stylePrimaryButton(AbstractButton button) {
            styleActionButton(button);
            button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        }

        private void styleFilterField(JTextField field) {
            field.setFont(field.getFont().deriveFont(Font.PLAIN, 14f));
            field.setPreferredSize(new Dimension(220, 40));
            field.setToolTipText("Filter by server/user name");
        }

        private void updateSelectedTargetLabel() {
            if (scopeType == ScopeType.PUBLIC) {
                return;
            }
            TargetOption selected = (TargetOption) targetCombo.getSelectedItem();
            if (selected == null) {
                selectedTargetLabel.setText("No target selected");
                return;
            }
            selectedTargetLabel.setText("Selected: " + selected.label() + "  (ID: " + selected.id() + ")");
        }

        private ListCellRenderer<? super TargetOption> createTargetRenderer() {
            return new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof TargetOption option) {
                        if (index < 0) {
                            label.setText(option.label());
                            label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
                        } else {
                            label.setText(option.label() + "  [" + option.id() + "]");
                            label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
                        }
                    }
                    label.setBorder(new EmptyBorder(6, 8, 6, 8));
                    return label;
                }
            };
        }

        private Border createSectionBorder() {
            return BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 224, 230)),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            );
        }
    }

    private String formatUserLabel(String id, String userName, boolean hasMylist) {
        String base = (userName == null || userName.isBlank()) ? ("User " + id) : userName;
        return hasMylist ? (base + "  [Has Mylist]") : base;
    }

    private Set<String> getExistingMylistOwnerIds() {
        Set<String> ids = new HashSet<>();
        Path root = Paths.get(bot.getConfig().getMylistfolder());
        if (!Files.isDirectory(root)) {
            return ids;
        }
        try (Stream<Path> paths = Files.list(root)) {
            paths.filter(Files::isDirectory)
                    .filter(this::hasPlaylistFiles)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("\\d{5,30}"))
                    .forEach(ids::add);
        } catch (Exception ignored) {
            // If folder scan fails, continue with only Discord cache
        }
        return ids;
    }

    private boolean hasPlaylistFiles(Path ownerDir) {
        try (Stream<Path> files = Files.list(ownerDir)) {
            return files.anyMatch(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".txt"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String scopeListLoadButtonLabel(ScopeType scopeType) {
        return switch (scopeType) {
            case PLAYLIST -> "Load Playlist List";
            case MYLIST -> "Load Mylist List";
            case PUBLIC -> "Load Public List";
        };
    }

    private record TargetOption(String id, String label, boolean hasMylist) {
        @Override
        public String toString() {
            return label;
        }
    }
}
