package dev.cosgy.niconicoSearchAPI;

import org.json.JSONObject;
import org.json.XML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class nicoVideoSearchResult {
    private String contentId;
    private String title;
    private String description;
    private String watchUrl;
    private String[] tags;
    private String[] categoryTags;
    private int viewCount;
    private int mylistCount;
    private int commentCount;
    private String startTime;
    private String thumbnailUrl;
    private nicoVideoInfo videoInfo;

    public nicoVideoSearchResult(String contentId, String title, String description, String[] tags, String[] categoryTags, int viewCount, int mylistCount, int commentCount, String startTime, String thumbnailUrl, boolean getVideoInfo) {
        this.contentId = contentId;
        this.title = title;
        this.description = description;
        this.watchUrl = "https://www.nicovideo.jp/watch/" + this.contentId;
        this.tags = tags;
        this.categoryTags = categoryTags;
        this.viewCount = viewCount;
        this.mylistCount = mylistCount;
        this.commentCount = commentCount;
        this.startTime = startTime;
        this.thumbnailUrl = thumbnailUrl;
        if (getVideoInfo) videoInfo = getInfo();
    }

    public nicoVideoSearchResult(String contentId, String title, String description, String[] tags, String[] categoryTags, int viewCount, int mylistCount, int commentCount, String startTime, String thumbnailUrl) {
        this.contentId = contentId;
        this.title = title;
        this.description = description;
        this.watchUrl = "https://www.nicovideo.jp/watch/" + this.contentId;
        this.tags = tags;
        this.categoryTags = categoryTags;
        this.viewCount = viewCount;
        this.mylistCount = mylistCount;
        this.commentCount = commentCount;
        this.startTime = startTime;
        this.thumbnailUrl = thumbnailUrl;
    }

    public nicoVideoSearchResult(boolean getVideoInfo) {
        if (getVideoInfo) videoInfo = getInfo();
    }

    public nicoVideoSearchResult() {

    }

    //get
    public String getContentId() {
        return contentId;
    }

    //set
    public nicoVideoSearchResult setContentId(String contentId) {
        this.contentId = contentId;
        this.watchUrl = "https://www.nicovideo.jp/watch/" + this.contentId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public nicoVideoSearchResult setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public nicoVideoSearchResult setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getWatchUrl() {
        return watchUrl;
    }

    public nicoVideoSearchResult setWatchUrl(String watchUrl) {
        this.watchUrl = watchUrl;
        return this;
    }

    public String[] getTags() {
        return tags;
    }

    public nicoVideoSearchResult setTags(String[] tags) {
        this.tags = tags;
        return this;
    }

    public String[] getCategoryTags() {
        return categoryTags;
    }

    public nicoVideoSearchResult setCategoryTags(String[] categoryTags) {
        this.categoryTags = categoryTags;
        return this;
    }

    public int getViewCount() {
        return viewCount;
    }

    public nicoVideoSearchResult setViewCount(int viewCount) {
        this.viewCount = viewCount;
        return this;
    }

    public int getMylistCount() {
        return mylistCount;
    }

    public nicoVideoSearchResult setMylistCount(int mylistCount) {
        this.mylistCount = mylistCount;
        return this;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public nicoVideoSearchResult setCommentCount(int commentCount) {
        this.commentCount = commentCount;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public nicoVideoSearchResult setStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public nicoVideoSearchResult setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }

    public nicoVideoInfo getInfo() {
        if (videoInfo == null) {
            HTTPUtil requester = new HTTPUtil("GET", "https://ext.nicovideo.jp/api/getthumbinfo/" + contentId);
            JSONObject object = XML.toJSONObject(requester.request());
            object = object.getJSONObject("nicovideo_thumb_response").getJSONObject("thumb");

            nicoVideoInfo nvi = new nicoVideoInfo();
            nvi.setCommentCount(object.getInt("comment_num"));
            nvi.setMylistCount(object.getInt("mylist_counter"));
            nvi.setLengthFormatted(object.getString("length"));
            nvi.setDescription(object.getString("description"));
            nvi.setTitle(object.getString("title"));
            nvi.setThumbnailUrl(object.getString("thumbnail_url"));

            List<String> tags = new ArrayList<>();
            object.getJSONObject("tags").getJSONArray("tag").forEach(obj -> {
                if (obj instanceof String) tags.add(obj.toString());
                else if (obj instanceof JSONObject) tags.add(((JSONObject) obj).getString("content"));
                else {
                    System.out.println("Err: " + obj.toString());
                    tags.add(obj.toString());
                }
            });

            nvi.setTags(tags.toArray(new String[]{}));
            // Bug Fixes: [object#has(String key)] == In the case of "up" formula, user-related information cannot be retrieved, so "has" should be inserted.
            if (object.has("user_icon_url")) nvi.setUploadUserIconUrl(object.getString("user_icon_url"));
            nvi.setWatchUrl(object.getString("watch_url"));
            if (object.has("user_id")) nvi.setUploadUserId(object.getInt("user_id"));
            if (object.has("user_nickname")) nvi.setUploadUserName(object.getString("user_nickname"));
            nvi.setViewCount(object.getInt("view_counter"));
            nvi.setVideoId(object.getString("video_id"));
            return nvi;
        } else return videoInfo;
    }

    public nicoVideoSearchResult setInfo(nicoVideoInfo videoInfo) {
        this.videoInfo = videoInfo;
        return this;
    }

    public String toString() {
        return "contentId=" + contentId + ", title=" + title + ", description=" + description + ", watchUrl=" + watchUrl + ", tags=" + Arrays.toString(tags) + ", categoryTags=" + Arrays.toString(categoryTags) +
                ", viewCount=" + viewCount + ", mylistCount=" + mylistCount + ", commentCount=" + commentCount + ", startTime=" + startTime + ", thumbnailUrl=" + thumbnailUrl + ", videoInfo=\"" + getInfo().toString() + "\"";
    }
}
