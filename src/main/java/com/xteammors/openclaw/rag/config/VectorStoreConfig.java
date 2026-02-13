package com.xteammors.openclaw.rag.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Bean
    @Primary
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        log.info("Using SimpleVectorStore (In-Memory) with File Persistence as local Redis lacks RediSearch module.");
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        
        java.io.File storeFile = new java.io.File("vector_store.json");
        if (storeFile.exists()) {
            log.info("Loading existing vector store from file: {}", storeFile.getAbsolutePath());
            vectorStore.load(storeFile);
        }
        
        return vectorStore;
    }

}
