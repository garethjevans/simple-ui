package com.vmware.tanzu.simpleui.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultModelResolver implements ModelResolver {

    private final String apiKey;
    private final String chatApiKey;
    private final String baseUrl;

    public DefaultModelResolver(@Value("${spring.ai.openai.api-key:}") String apiKey,
                                @Value("${spring.ai.openai.chat.api-key:}") String chatApiKey,
                                @Value("${spring.ai.openai.chat.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.chatApiKey = chatApiKey;
        this.baseUrl = baseUrl;
    }

    private String apiKey() {
        if (!chatApiKey.isEmpty()) {
            return chatApiKey;
        }
        return apiKey;
    }


    @Override
    public List<String> availableModels() {
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();
        Response models = client.get()
                .uri("/v1/models")
                .headers(httpHeaders -> httpHeaders.add("Authorization", "Bearer " + apiKey()))
                .retrieve()
                .body(Response.class);
        return convert(models);
    }

    private List<String> convert(Response models) {
        List<String> modelList = new ArrayList<>();
        for (Item i : models.data()) {
            modelList.add(i.id());
        }
        return modelList;
    }

    public record Response(List<Item> data, String type, String object) {};

    public record Item(String id, String object, Long created, String ownedBy) {}

}
