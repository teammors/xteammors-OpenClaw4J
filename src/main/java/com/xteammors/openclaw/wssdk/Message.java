package com.xteammors.openclaw.wssdk;

import com.alibaba.fastjson.JSON;

import java.util.Map;

public class Message {
    private String id;
    private String text;
    private boolean isMe = false;
    private String timestamp;
    private boolean isEdited = false;
    private String editTime;
    private boolean isFavorite;
    private boolean isForwarded;
    private String forwardedFrom;
    private String replyTo;
    private Object messageType;
    private String[] mediaUrls;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private String location;
    private String locationName;
    private int status;
    private Integer sendProgress;
    private String[] imageSizes;
    private String audioUrl;
    private Long audioDuration;
    private Long videoDuration;
    private Float videoAspectRatio;
    private Integer videoBubbleWidth;
    private Integer videoBubbleHeight;
    private boolean isGroup;
    private String senderUid;
    private String senderName;
    private String senderAvatar;
    private Map<String, Object> mentions;
    private boolean isSended;
    private boolean isSystemMessage;
    private Object systemData;
    private String bubbleEmoji;
    private Long callDuration;
    private Integer callResult;
    private Integer callType;
    private String chatId;

    // Getters and Setters (same as before)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean getIsMe() {
        return isMe;
    }

    public void setIsMe(boolean me) {
        isMe = me;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getIsEdited() {
        return isEdited;
    }

    public void setIsEdited(boolean edited) {
        isEdited = edited;
    }

    public String getEditTime() {
        return editTime;
    }

    public void setEditTime(String editTime) {
        this.editTime = editTime;
    }

    public boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(boolean favorite) {
        isFavorite =  favorite;
    }

    public boolean getIsForwarded() {
        return isForwarded;
    }

    public void setForwarded(boolean forwarded) {
        isForwarded = forwarded;
    }

    public String getForwardedFrom() {
        return forwardedFrom;
    }

    public void setForwardedFrom(String forwardedFrom) {
        this.forwardedFrom = forwardedFrom;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public int getMessageType() {
        if (messageType == null) {
            return 0;
        }
        if (messageType instanceof Number) {
            return ((Number) messageType).intValue();
        }
        if (messageType instanceof String) {
            String s = (String) messageType;
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                if ("MessageType.text".equals(s)) {
                    return 0;
                }
                System.err.println("Unknown messageType string: " + s);
                return 0;
            }
        }
        return 0;
    }

    public void setMessageType(Object messageType) {
        this.messageType = messageType;
    }

    public String[] getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(String[] mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Integer getSendProgress() {
        return sendProgress;
    }

    public void setSendProgress(Integer sendProgress) {
        this.sendProgress = sendProgress;
    }

    public String[] getImageSizes() {
        return imageSizes;
    }

    public void setImageSizes(String[] imageSizes) {
        this.imageSizes = imageSizes;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Long getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(Long audioDuration) {
        this.audioDuration = audioDuration;
    }

    public Long getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(Long videoDuration) {
        this.videoDuration = videoDuration;
    }

    public Float getVideoAspectRatio() {
        return videoAspectRatio;
    }

    public void setVideoAspectRatio(Float videoAspectRatio) {
        this.videoAspectRatio = videoAspectRatio;
    }

    public Integer getVideoBubbleWidth() {
        return videoBubbleWidth;
    }

    public void setVideoBubbleWidth(Integer videoBubbleWidth) {
        this.videoBubbleWidth = videoBubbleWidth;
    }

    public Integer getVideoBubbleHeight() {
        return videoBubbleHeight;
    }

    public void setVideoBubbleHeight(Integer videoBubbleHeight) {
        this.videoBubbleHeight = videoBubbleHeight;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public Map<String, Object> getMentions() {
        return mentions;
    }

    public void setMentions(Map<String, Object> mentions) {
        this.mentions = mentions;
    }

    public boolean isSended() {
        return isSended;
    }

    public void setSended(boolean sended) {
        isSended = sended;
    }

    public boolean isSystemMessage() {
        return isSystemMessage;
    }

    public void setSystemMessage(boolean systemMessage) {
        isSystemMessage = systemMessage;
    }

    public Object getSystemData() {
        return systemData;
    }

    public void setSystemData(Object systemData) {
        this.systemData = systemData;
    }

    public String getBubbleEmoji() {
        return bubbleEmoji;
    }

    public void setBubbleEmoji(String bubbleEmoji) {
        this.bubbleEmoji = bubbleEmoji;
    }

    public Long getCallDuration() {
        return callDuration;
    }

    public void setCallDuration(Long callDuration) {
        this.callDuration = callDuration;
    }

    public Integer getCallResult() {
        return callResult;
    }

    public void setCallResult(Integer callResult) {
        this.callResult = callResult;
    }

    public Integer getCallType() {
        return callType;
    }

    public void setCallType(Integer callType) {
        this.callType = callType;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    // toJson method using FastJSON
    public String toJson() {
        return JSON.toJSONString(this);
    }

    // Optional: fromJson method to parse JSON back to Message object
    public static Message fromJson(String json) {
        return JSON.parseObject(json, Message.class);
    }
}
