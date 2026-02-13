package com.xteammors.openclaw.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekClientConfig {

    @Bean
    public OpenAiApi openAiApi(@Value("${spring.ai.openai.base-url}") String baseUrl,
                               @Value("${spring.ai.openai.api-key}") String apiKey) {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    public OpenAiChatOptions openAiChatOptions(@Value("${spring.ai.openai.chat.options.model}") String model,
                                               @Value("${spring.ai.openai.chat.options.temperature}") Double temperature,
                                               @Value("${spring.ai.openai.chat.options.max-tokens}") Integer maxTokens) {
        return OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiApi openAiApi, OpenAiChatOptions chatOptions) {
        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, chatOptions);
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }


}
