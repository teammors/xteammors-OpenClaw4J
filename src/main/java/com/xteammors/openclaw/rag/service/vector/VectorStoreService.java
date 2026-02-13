package com.xteammors.openclaw.rag.service.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPooled;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {
    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final VectorStore vectorStore;
    private final JedisPooled jedis; // 仅用于文档持久化，非向量检索

    public VectorStoreService(VectorStore vectorStore, RedisProperties redisProperties) {
        this.vectorStore = vectorStore;
        this.jedis = createJedis(redisProperties);
        try {
            String pong = this.jedis.ping();
            log.info("Redis 文档持久化连接可用: {}", pong);
        } catch (Exception e) {
            log.warn("Redis 文档持久化不可用，将仅使用内存向量库: {}", e.getMessage());
        }
    }

    public void addDocument(String content, Map<String, Object> metadata) {
        Document document = new Document(content, metadata);
        try {
            vectorStore.add(List.of(document));
            log.info("文档已添加到向量数据库: {}", metadata.get("filename"));
            saveVectorStore();
        } catch (Exception e) {
            log.warn("添加向量失败，改为仅持久化文档: {}", e.getMessage());
        }

        persistToRedis(document);
    }

    public void addDocuments(List<String> contents, List<Map<String, Object>> metadataList) {
        if (contents.size() != metadataList.size()) {
            throw new IllegalArgumentException("Contents and metadata list must have the same size");
        }
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            documents.add(new Document(contents.get(i), metadataList.get(i)));
        }
        try {
            vectorStore.add(documents);
            log.info("批量添加文档到向量数据库: {} 个文档", documents.size());
            saveVectorStore();
        } catch (Exception e) {
            log.warn("批量添加向量失败，改为仅持久化文档: {}", e.getMessage());
        }

        int persisted = 0;
        for (Document doc : documents) {
            try {
                persistToRedis(doc);
                persisted++;
            } catch (Exception e) {
                log.warn("持久化至Redis失败: {}", e.getMessage());
            }
        }
        log.info("已将 {} 个文档持久化到Redis文档库", persisted);
    }

    public List<Document> similaritySearch(String query, int topK) {
        try {
            return vectorStore.similaritySearch(query);
        } catch (Exception e) {
            log.warn("向量检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Document> keywordSearch(String query, int topK) {
        try {
            List<String> tokens = tokenize(query);
            List<String> boosted = new ArrayList<>(tokens);
            boosted.addAll(synonymsForQuery(query));
            Map<Document, Integer> scores = new HashMap<>();

            List<String> keys = scanKeys("rag:docs:*");
            for (String key : keys) {
                Map<String, String> hash = jedis.hgetAll(key);
                if (hash == null || hash.isEmpty()) continue;
                String content = hash.getOrDefault("content", "");
                int score = scoreContent(content, boosted, query);
                if (score > 0) {
                    Map<String, Object> md = new HashMap<>();
                    for (Map.Entry<String, String> e : hash.entrySet()) {
                        md.put(e.getKey(), e.getValue());
                    }
                    Document doc = new Document(content, md);
                    scores.put(doc, score);
                }
            }

            List<Document> ranked = scores.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(topK)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            log.info("关键字检索命中 {} 个文档", ranked.size());
            return ranked;
        } catch (Exception e) {
            log.warn("关键字检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteAll() {
        // Clear Redis data
        try {
            List<String> keys = scanKeys("rag:docs:*");
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
                log.info("Cleared {} documents from Redis.", keys.size());
            }
        } catch (Exception e) {
            log.warn("Failed to clear Redis data: {}", e.getMessage());
        }

        // Clear VectorStore
        if (vectorStore instanceof SimpleVectorStore) {
            try {
                // Use reflection to clear the internal map of SimpleVectorStore
                java.lang.reflect.Field storeField = SimpleVectorStore.class.getDeclaredField("store");
                storeField.setAccessible(true);
                Map<?, ?> store = (Map<?, ?>) storeField.get(vectorStore);
                store.clear();
                
                // Also delete the file
                java.io.File storeFile = new java.io.File("vector_store.json");
                if (storeFile.exists()) {
                    if (storeFile.delete()) {
                        log.info("Deleted vector_store.json file.");
                    } else {
                        log.warn("Failed to delete vector_store.json file.");
                    }
                }
                log.info("Cleared SimpleVectorStore (in-memory and file).");
            } catch (Exception e) {
                log.error("Failed to clear SimpleVectorStore using reflection", e);
            }
        } else {
            log.warn("deleteAll not implemented for this VectorStore type.");
        }
    }

    public void saveVectorStore() {
        if (vectorStore instanceof SimpleVectorStore) {
            try {
                java.io.File storeFile = new java.io.File("vector_store.json");
                ((SimpleVectorStore) vectorStore).save(storeFile);
                log.info("向量数据库已保存到文件: {}", storeFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存向量数据库失败", e);
            }
        }
    }

    public void deleteByIds(List<String> ids) {
        vectorStore.delete(ids);
        log.info("Deleted {} documents from vector store", ids.size());
    }

    private JedisPooled createJedis(RedisProperties redisProperties) {
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();
        String uri;
        if (StringUtils.hasText(password)) {
            String encoded = java.net.URLEncoder.encode(password, java.nio.charset.StandardCharsets.UTF_8);
            uri = "redis://:" + encoded + "@" + host + ":" + port + "/" + redisProperties.getDatabase();
        } else {
            uri = "redis://" + host + ":" + port + "/" + redisProperties.getDatabase();
        }
        return new JedisPooled(java.net.URI.create(uri));
    }

    private void persistToRedis(Document document) {
        Map<String, Object> m = document.getMetadata();
        String filename = String.valueOf(m.getOrDefault("filename", "unknown"));
        String chunkIndex = String.valueOf(m.getOrDefault("chunk_index", "0"));
        String key = "rag:docs:" + filename + ":" + chunkIndex;

        Map<String, String> hash = new HashMap<>();
        hash.put("filename", filename);
        hash.put("chunk_index", chunkIndex);
        hash.put("content", document.getText());
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            if (entry.getValue() != null) {
                hash.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        try {
            jedis.hset(key, hash);
            log.debug("已持久化文档到Redis: {}", key);
        } catch (Exception e) {
            log.warn("写入Redis失败: {}", e.getMessage());
        }
    }

    private List<String> scanKeys(String pattern) {
        try {
            Set<String> set = jedis.keys(pattern);
            return new ArrayList<>(set);
        } catch (Exception e) {
            log.warn("扫描Redis键失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        String lower = text.toLowerCase();
        if (containsCjk(lower)) {
            List<String> ngrams = new ArrayList<>();
            for (int n = 2; n <= 3; n++) {
                for (int i = 0; i + n <= lower.length(); i++) {
                    String gram = lower.substring(i, i + n);
                    if (!gram.isBlank()) ngrams.add(gram);
                }
            }
            return ngrams;
        } else {
            String[] parts = lower.split("[^\\p{L}\\p{N}]+");
            List<String> tokens = new ArrayList<>();
            for (String p : parts) {
                if (!p.isEmpty()) tokens.add(p);
            }
            return tokens;
        }
    }

    private int scoreContent(String content, List<String> tokens, String query) {
        if (content == null || content.isEmpty() || tokens.isEmpty()) return 0;
        String lower = content.toLowerCase();
        int score = 0;
        for (String t : tokens) {
            int idx = 0;
            while ((idx = lower.indexOf(t, idx)) != -1) {
                score++;
                idx += Math.max(1, t.length());
            }
        }
        if (query.contains("最大") || query.contains("多少")) {
            if (lower.matches(".*\\d+.*")) score += 5;
            if (lower.contains("20000")) score += 3;
            if (lower.contains("20000+")) score += 4;
        }
        return score;
    }

    private boolean containsCjk(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT) {
                return true;
            }
        }
        return false;
    }

    private List<String> synonymsForQuery(String query) {
        List<String> s = new ArrayList<>();
        if (query == null) return s;
        if (query.contains("群组") || query.contains("群")) {
            s.add("群组");
            s.add("群");
            s.add("大群");
        }
        if (query.contains("人数") || query.contains("人") || query.contains("支持")) {
            s.add("人数");
            s.add("人");
            s.add("支持");
            s.add("订阅");
        }
        if (query.contains("最大") || query.contains("多少")) {
            s.add("最大");
            s.add("多少");
        }
        return s;
    }
}
