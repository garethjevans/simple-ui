package com.vmware.tanzu.simpleui;

import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.openai.autoconfigure.OpenAIAutoConfigurationUtil;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.ai.model.openai.autoconfigure.OpenAIAutoConfigurationUtil.resolveConnectionProperties;

@Configuration
public class UiConfiguration {

    @Bean
    public ChatModel chatModel(GenaiLocator locator) {
        return locator.getFirstAvailableChatModel();
    }

//    @Bean
//    public OpenAiApi openAiApi(OpenAiConnectionProperties commonProperties, OpenAiChatProperties chatProperties,
//                               ObjectProvider<RestClient.Builder> restClientBuilderProvider,
//                               ObjectProvider<WebClient.Builder> webClientBuilderProvider, ResponseErrorHandler responseErrorHandler) {
//
//        return OpenAiApi.builder()
//                .baseUrl(resolved.baseUrl())
//                .apiKey(new SimpleApiKey(resolved.apiKey()))
//                .completionsPath(chatProperties.getCompletionsPath())
//                .embeddingsPath(OpenAiEmbeddingProperties.DEFAULT_EMBEDDINGS_PATH)
//                .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
//                .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
//                .responseErrorHandler(responseErrorHandler)
//                .build();
//    }

//    @Bean
//    public EmbeddingModel embeddingModel(GenaiLocator locator) {
//        return locator.getFirstAvailableEmbeddingModel();
//    }
}
