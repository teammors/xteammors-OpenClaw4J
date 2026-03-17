package com.xteammors.openclaw.wssdk;

public class MessageBody {

    private String eventId; // Event ID
    private String fromUid; // Sender ID
    private String toUid; // Receiver ID
    private String token; // Sender token
    private String deviceId = ""; // Sender token unique device ID

    private String type; // Message type
    private String cTimest; // Client send timestamp
    private String sTimest; // Server receive timestamp
    private String dataBody; // Message body, JSON string format {}

    private String isGroup = "0"; // 1-Group, 0-Individual
    private String groupId = ""; // Group ID

    private String isCache = "1"; // 1-Need offline cache, 0-No need

    public MessageBody() {
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getFromUid() { return fromUid; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }

    public String getToUid() { return toUid; }
    public void setToUid(String toUid) { this.toUid = toUid; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getcTimest() { return cTimest; }
    public void setcTimest(String cTimest) { this.cTimest = cTimest; }

    public String getsTimest() { return sTimest; }
    public void setsTimest(String sTimest) { this.sTimest = sTimest; }

    public String getDataBody() { return dataBody; }
    public void setDataBody(String dataBody) { this.dataBody = dataBody; }

    public String getIsGroup() { return isGroup; }
    public void setIsGroup(String isGroup) { this.isGroup = isGroup; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getIsCache() { return isCache; }
    public void setIsCache(String isCache) { this.isCache = isCache; }

    @Override
    public String toString() {
        return "Message{" +
                "eventId='" + eventId + '\'' +
                ", fromUid='" + fromUid + '\'' +
                ", toUid='" + toUid + '\'' +
                ", dataBody='" + dataBody + '\'' +
                '}';
    }
}
