package com.xteammors.openclaw.wssdk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.utils.PositiveIntegerValidator;
import com.xteammors.openclaw.utils.TimeUtils;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class XMessageClient extends WebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(XMessageClient.class);
    private static final long RECONNECT_INTERVAL = 3000; // 3 seconds
    private static final long PING_INTERVAL = 5; // 5 seconds
    private static final long HEARTBEAT_TIMEOUT = PING_INTERVAL * 3 * 1000; // 15 seconds timeout

    public String fromUid;
    private String token;
    private String deviceId;
    private String encryptionKey;

    private long lastActiveTime = 0;

    public String mId = "";
    private String robotUid = "";
    private String robotId = "";
    private String robotName = "";
    private String robotAvatar = "";
    private String privateKey = "";
    private String publicKey = "";
    private String apiServer = "";
    private String apiToken = "";
    private String apiStartId = "";
    private String serverIp = "";
    private String groupId = "";

    String robotToken = "";
    
    // Use XMessageManagerSubject to manage multiple observers
    private final XMessageManagerSubject messageSubject = new XMessageManagerSubject();

    private OkHttpClient client;
    private WebSocket webSocket;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> pingTask;
    private ScheduledFuture<?> reconnectTask;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isLoggedIn = new AtomicBoolean(false);
    private boolean isReconnecting = false;
    private int reconnectAttempts = 0;

    private static XMessageClient xMessageClient;

    public static XMessageClient instance() {
        if (null == xMessageClient) {
            xMessageClient = new XMessageClient();
        }
        return xMessageClient;
    }

    public void init(String botToken) {
        this.robotToken = botToken;
        fetchDataFromApi();
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public XMessageClient() {}
    
    public void addObserver(XMessageObserver observer) {
        messageSubject.addObserver(observer);
    }
    
    public void removeObserver(XMessageObserver observer) {
        messageSubject.deleteObserver(observer);
    }

    public void connect() {
        if (isConnected.get()) return;

        Request request = new Request.Builder()
                .url(serverIp)
                .build();
        messageSubject.publish("Connecting to "+serverIp);
        webSocket = client.newWebSocket(request, this);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        lastActiveTime = System.currentTimeMillis();
        messageSubject.publish("Connected to server");
        isConnected.set(true);
        isReconnecting = false;
        reconnectAttempts = 0;
        // Cancel any pending reconnect tasks
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
        // Auto-login on connect
        login();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        lastActiveTime = System.currentTimeMillis();
        try {
            String jsonStr;
            if (XJSONUtils.isJsonFast(text)) {
                jsonStr = text;
            } else {
                try {
                    jsonStr = SecurityUtil.decrypt(encryptionKey, text);
                } catch (Exception e) {
                    messageSubject.publishError("Decryption failed: " + e.getMessage());
                    return;
                }
            }
            
            // Notify observers about the incoming message
            messageSubject.publish(jsonStr);

            MessageBody messageBody = JSON.parseObject(jsonStr, MessageBody.class); // Use FastJson
            handleEvent(messageBody);
        } catch (Exception e) {
            messageSubject.publishError("Failed to process message: " + e.getMessage());
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        messageSubject.publish("Closing:" +code + "/" + reason);
        webSocket.close(1000, null);
        handleDisconnect();
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        messageSubject.publish("Closed:" +code + "/" + reason);
        handleDisconnect();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        messageSubject.publishError("Connection failed: " + t.getMessage());
        handleDisconnect();
    }

    private synchronized void handleDisconnect() {
        isConnected.set(false);
        isLoggedIn.set(false);
        stopPing();
        // Always try to schedule a reconnect, even if isReconnecting is true.
        // The scheduleReconnect method will prevent duplicate tasks.
        isReconnecting = true;
        scheduleReconnect();
    }

    private synchronized void scheduleReconnect() {
        if (reconnectTask != null && !reconnectTask.isDone()) {
            return; // Already scheduled
        }

        reconnectTask = executorService.schedule(() -> {
            try {
                // Check again before connecting
                if (isConnected.get()) return;

                reconnectAttempts++;
                if (reconnectAttempts > 10) {
                     messageSubject.publish("Too many reconnect failures (" + reconnectAttempts + "), refreshing server info and resetting client...");
                     reconnectAttempts = 0; // Reset
                     
                     // Close existing client resources if possible
                     try {
                         if (client != null) {
                             client.dispatcher().executorService().shutdown();
                             client.connectionPool().evictAll();
                         }
                     } catch (Exception ignored) {}
                     
                     // Re-initialize client to clear connection pool and DNS cache
                     client = new OkHttpClient.Builder()
                            .readTimeout(0, TimeUnit.MILLISECONDS)
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .build();
                            
                     fetchDataFromApi(); // Full restart
                     return;
                }

                messageSubject.publish("Reconnecting to " + serverIp + " (Attempt " + reconnectAttempts + ")...");
                connect();
            } catch (Exception e) {
                logger.error("Error during reconnection", e);
            }
        }, RECONNECT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void sendMessage(MessageBody messageBody) {
        if (webSocket != null && isConnected.get()) {
            messageBody.setcTimest(String.valueOf(System.currentTimeMillis()));
            String jsonStr = JSON.toJSONString(messageBody); // Use FastJson

            String payload;
            // Login message (1000000) is NOT encrypted
            if ("1000000".equals(messageBody.getEventId())) {
                payload = jsonStr;
            } else {
                // Other messages ARE encrypted
                payload = SecurityUtil.encrypt(encryptionKey, jsonStr);
            }
            webSocket.send(payload);
        } else {
            messageSubject.publishError("Cannot send message, not connected");
        }
    }

    private void login() {
        MessageBody msg = new MessageBody();
        msg.setEventId("1000000");
        msg.setFromUid(fromUid);
        msg.setToken(token); // Token is required for login
        msg.setDeviceId(deviceId);
        msg.setDataBody("{}");
        sendMessage(msg);
    }

    private void startPing() {
        if (pingTask != null && !pingTask.isCancelled()) return;

        pingTask = executorService.scheduleAtFixedRate(() -> {
            try {
                if (isConnected.get() && isLoggedIn.get()) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastActiveTime > HEARTBEAT_TIMEOUT) {
                        messageSubject.publishError("Heartbeat timeout, reconnecting...");
                        if (webSocket != null) {
                            webSocket.close(1000, "Heartbeat timeout");
                        }
                        handleDisconnect();
                        return;
                    }
                    sendPing();
                }
            } catch (Exception e) {
                logger.error("Error in ping task", e);
            }
        }, 0, PING_INTERVAL, TimeUnit.SECONDS);
    }

    private void stopPing() {
        if (pingTask != null) {
            pingTask.cancel(true);
            pingTask = null;
        }
    }

    private void sendPing() {
        MessageBody msg = new MessageBody();
        msg.setEventId("9000000");
        msg.setFromUid(fromUid);
        msg.setToken(""); // Token is empty for non-login messages
        msg.setDeviceId(deviceId);
        sendMessage(msg);
    }
    
    // --- New Functionality Methods ---

    public boolean sendSingleUserTxtMessage(String waitSendMessage,String userId,int isCache) {

        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setText(waitSendMessage);
        message.setTimestamp(TimeUtils.getTimeSt());
        message.setChatId(robotUid);
        message.setSenderUid(robotUid);
        message.setSenderName(robotName);
        message.setSenderAvatar(robotAvatar);

        System.out.println("message.toJson()="+message.toJson());

        return sendPrivateMessage(userId,message.toJson(),isCache+"");
    }


    public boolean sendToGroupTxtMessage(String waitSendMessage,String groupId,int isCache) {

        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setText(waitSendMessage);
        message.setTimestamp(TimeUtils.getTimeSt());
        message.setChatId(groupId);
        message.setSenderUid(robotUid);
        message.setSenderName(robotName);
        message.setSenderAvatar(robotAvatar);

        return sendGroupMessage(groupId,message.toJson(),isCache+"");
    }

    public boolean sendPrivateMessage(String toUid, String content, String isCache) {
        MessageBody msg = new MessageBody();
        msg.setEventId("1000001");
        msg.setFromUid(fromUid);
        msg.setToUid(toUid);
        msg.setToken(""); // Token is empty for non-login messages
        msg.setDeviceId(deviceId);
        msg.setIsGroup("0");
        msg.setIsCache(isCache);
        msg.setDataBody(content);
        sendMessage(msg);
        return true;
    }

    private void sendAck(String toUid, String messageId) {
        MessageBody msg = new MessageBody();
        msg.setEventId("1000002");
        msg.setFromUid(fromUid);
        msg.setToUid(toUid); // Send ACK back to sender (or server depending on protocol)
        msg.setToken(""); // Token is empty for non-login messages
        msg.setDeviceId(deviceId);
        
        // Format: ["sTimest"]
        List<String> ids = Collections.singletonList(messageId);
        msg.setDataBody(JSON.toJSONString(ids));
        
        sendMessage(msg);
    }

    private void autoSendReceive(Message message) {

        String chatId = message.getChatId();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("action", "receive");
        jsonObject.put("messageType", message.getMessageType());
        jsonObject.put("id", message.getId());

        if(PositiveIntegerValidator.isPositiveInteger(chatId)) {
            jsonObject.put("chatId", robotUid);
        }else {
            jsonObject.put("chatId", chatId);
            jsonObject.put("groupId", chatId);
            jsonObject.put("isGroup", "1");

        }
        jsonObject.put("receiveUid", robotUid);

        String data = jsonObject.toJSONString();

        String userId = "";
        userId = mId + "_" + message.getSenderUid();
        sendPrivateMessage(userId,data,1+"");

    }


    public boolean sendGroupMessage(String groupId, String content, String isCache) {
        MessageBody msg = new MessageBody();
        msg.setEventId("5000004");
        msg.setFromUid(fromUid);
        msg.setToken(""); // Token is empty for non-login messages
        msg.setDeviceId(deviceId);
        msg.setIsGroup("1");
        msg.setGroupId(groupId);
        msg.setIsCache(isCache);
        msg.setDataBody(content);
        sendMessage(msg);
        return true;
    }

    private void handleEvent(MessageBody messageBody) {
        String eventId = messageBody.getEventId();
        if (eventId == null) return;

        switch (eventId) {
            case "1000000": // Login Response
                if ("Success".equals(messageBody.getDataBody())) {
                    messageSubject.publish("Login Successful!");
                    isLoggedIn.set(true);
                    startPing();
                } else {
                    messageSubject.publish("Login Failed: "+ messageBody.getDataBody());
                    isLoggedIn.set(false);
                    stopPing();
                    if (webSocket != null) {
                        webSocket.close(1000, "Login Failed");
                    }
                    // Trigger full reconnect flow to refresh token/server info
                    fetchDataFromApi();
                }
                break;
            case "1000001": // Receive Private Message
                // Automatically reply with ACK 1000002
                String msgId = messageBody.getsTimest();
                if (msgId != null && !msgId.isEmpty()) {
                    sendAck(messageBody.getFromUid(), msgId);
                    Message message = JSON.parseObject(messageBody.getDataBody(), Message.class);
                    autoSendReceive(message);
                } else {
                    messageSubject.publishError("Received Private Message without sTimest, cannot send ACK");
                }
                break;

        }
    }

    private void fetchDataFromApi() {

        System.out.println("进入：fetchDataFromApi");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://www.teammors.top/json/api.json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("Failed to fetch data from API: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();

                        // 检查响应是否为空或无效
                        if (responseData == null || responseData.trim().isEmpty()) {
                            System.err.println("API response is empty");
                            return;
                        }

                        // 检查是否是有效的JSON开始
                        String trimmedResponse = responseData.trim();
                        if (!trimmedResponse.startsWith("{") && !trimmedResponse.startsWith("[")) {
                            System.err.println("API response doesn't appear to be valid JSON: " + responseData);
                            return;
                        }

                        // 尝试解析 JSON
                        JSONObject jsonObject = JSONObject.parseObject(responseData);

                        if (jsonObject.containsKey("data")) {
                            // 检查 data 字段的类型
                            Object dataValue = jsonObject.get("data");
                            if (dataValue instanceof String) {
                                // 如果 data 是字符串
                                String dataString = (String) dataValue;
                                // 存储 API Server 地址
                                apiServer = dataString;

                                // 调用请求 webKey 的方法
                                requestWebKey();
                            } else if (dataValue instanceof JSONObject) {
                                // 如果 data 是对象
                                JSONObject dataObject = (JSONObject) dataValue;
                            }
                        } else {
                            System.err.println("API response does not contain 'data' field");
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse API response: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        response.close();
                    }
                } else {
                    System.err.println("Failed to fetch data from API. Response code: " + response.code());
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    private void requestWebKey() {
        try {
            // 拼接地址
            String webKeyAddress = apiServer + "/getWebKey";

            // 生成随机 userId
            String userId = String.valueOf((int) (Math.random() * 1000000));

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("userId", userId);
            String jsonBody = requestBody.toJSONString();

            // 创建 OkHttpClient 实例
            OkHttpClient client = new OkHttpClient();

            // 创建请求
            Request request = new Request.Builder()
                    .url(webKeyAddress)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            // 发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.err.println("Failed to request webKey: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            // 从响应头中读取 start-id
                            String startId = response.header("start-id");
                            if (startId != null) {
                                apiStartId = startId;
                            }

                            // 解析响应体
                            String responseData = response.body().string();

                            JSONObject responseObject = JSONObject.parseObject(responseData);
                            if (responseObject.containsKey("status") &&
                                    "success".equals(responseObject.getString("status"))) {

                                // 提取 token
                                if (responseObject.containsKey("token")) {
                                    apiToken = responseObject.getString("token");
                                    // 提取 userId
                                    if (responseObject.containsKey("userId")) {
                                        String responseUserId = responseObject.getString("userId");
                                    }

                                    // 调用请求机器人信息的方法
                                    requestRobotInfo();
                                }
                            } else {
                                System.err.println("WebKey request failed: " + responseData);
                            }
                        } else {
                            System.err.println("WebKey request failed with code: " + response.code());
                        }
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error in requestWebKey: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void requestRobotInfo() {
        try {
            // 拼接地址
            String robotInfoAddress = apiServer + "/getRobotInfo";

            // 构建包含 robotToken 的 JSON
            JSONObject tokenJson = new JSONObject();
            tokenJson.put("token", robotToken); // 使用已写死在文件中的 robotToken
            String tokenJsonString = tokenJson.toJSONString();

            // 加密数据
            String secRKey = SecurityUtil.getUidKey(apiStartId);
            String encryptedData = SecurityUtil.encrypt(secRKey, tokenJsonString);

            // 构建请求参数
            JSONObject requestData = new JSONObject();
            requestData.put("data", encryptedData);
            String requestJson = requestData.toJSONString();

            // 创建 OkHttpClient 实例
            OkHttpClient client = new OkHttpClient();

            // 创建请求
            Request request = new Request.Builder()
                    .url(robotInfoAddress)
                    .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Accept", "*/*")
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("startId", apiStartId)
                    .build();

            // 发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.err.println("Failed to request robot info: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            // 获取响应内容
                            String responseData = response.body().string();
                            JSONObject responseObject = JSONObject.parseObject(responseData);
                            String rsString = SecurityUtil.decrypt(secRKey, responseObject.getString("extData"));


                            JSONObject rsObject = JSONObject.parseObject(rsString);
                            String data = rsObject.getString("data");
                            JSONObject responseDataObject = JSONObject.parseObject(data);

                            fromUid = responseDataObject.get("account").toString();
                            token =  responseDataObject.get("token").toString();
                            serverIp = responseDataObject.get("server").toString();
                            publicKey = responseDataObject.get("publicKey").toString();
                            privateKey = responseDataObject.get("privateKey").toString();
                            robotId = responseDataObject.get("id").toString();
                            robotName = responseDataObject.get("name").toString();
                            robotAvatar = responseDataObject.get("avatar").toString();
                            robotUid = fromUid.split("_")[1];
                            mId = fromUid.split("_")[0];
                            groupId = "robot_"+robotId+"_user_list";
                            deviceId = "bot_"+token;
                            encryptionKey = SecurityUtil.getUidKey(fromUid);

                            connect();

                        } else {
                            System.err.println("Robot info request failed with code: " + response.code());
                        }
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error in requestRobotInfo: " + e.getMessage());
            e.printStackTrace();
        }
    }




}
