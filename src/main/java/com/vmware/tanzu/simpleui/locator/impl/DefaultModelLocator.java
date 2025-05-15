package com.vmware.tanzu.simpleui.locator.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.simpleui.locator.ModelLocator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.ai.document.MetadataMode;

import java.util.List;
import java.util.Map;

@Service
public class DefaultModelLocator implements ModelLocator {

    private final VcapService genaiService;

    public DefaultModelLocator() {
        genaiService = getGenAiService(System.getenv("VCAP_SERVICES"));
    }

    public DefaultModelLocator(String vcapServices) {
        genaiService = getGenAiService(vcapServices);
    }

    @Override
    public List<String> getModelNames() {
        ConfigEndpoint endpoint = getEndpoint();

        return endpoint.advertisedModels
                .stream()
                .map(a -> a.name)
                .toList();
    }

    @Override
    public List<String> getModelNamesByCapability(String capability) {
        ConfigEndpoint endpoint = getEndpoint();

        return endpoint.advertisedModels
                .stream()
                .filter(a -> a.capabilities().contains(capability))
                .map(a -> a.name)
                .toList();
    }

    @Override
    public ChatModel getChatModelByName(String name) {
        ConfigEndpoint endpoint = getEndpoint();

        List<String> availableChatModels = endpoint.advertisedModels
                .stream()
                .filter(a -> a.capabilities().contains("CHAT"))
                .map(a -> a.name)
                .toList();

        if (availableChatModels.contains(name)) {
            OpenAiApi api = OpenAiApi
                    .builder()
                    .apiKey(genaiService.credentials().endpoint().apiKey())
                    .baseUrl(genaiService.credentials.endpoint().apiBase() + "/" + endpoint.wireFormat().toLowerCase())
                    .build();

            return OpenAiChatModel
                    .builder()
                    .defaultOptions(OpenAiChatOptions.builder().model(name).build())
                    .openAiApi(api)
                    .build();
        }

        throw new RuntimeException("Unable to find chat model with name '" + name + "'");
    }

    @Override
    public ChatModel getFirstAvailableChatModel() {
        ConfigEndpoint endpoint = getEndpoint();

        String firstChatModel = endpoint.advertisedModels
                .stream()
                .filter(a -> a.capabilities().contains("CHAT"))
                .map(a -> a.name)
                .toList().getFirst();

        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(genaiService.credentials().endpoint().apiKey())
                .baseUrl(genaiService.credentials.endpoint().apiBase() + "/" + endpoint.wireFormat().toLowerCase())
                .build();

        return OpenAiChatModel
                .builder()
                .defaultOptions(OpenAiChatOptions.builder().model(firstChatModel).build())
                .openAiApi(api)
                .build();
    }

    @Override
    public ChatModel getFirstAvailableToolModel() {
        ConfigEndpoint endpoint = getEndpoint();

        String firstChatModel = endpoint.advertisedModels
                .stream()
                .filter(a -> a.capabilities().contains("TOOLS"))
                .map(a -> a.name)
                .toList().getFirst();

        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(genaiService.credentials().endpoint().apiKey())
                .baseUrl(genaiService.credentials.endpoint().apiBase() + "/" + endpoint.wireFormat().toLowerCase())
                .build();

        return OpenAiChatModel
                .builder()
                .defaultOptions(OpenAiChatOptions.builder().model(firstChatModel).build())
                .openAiApi(api)
                .build();
    }

    @Override
    public EmbeddingModel getEmbeddingModelByName(String name) {
        ConfigEndpoint endpoint = getEndpoint();

        List<String> availableChatModels = endpoint.advertisedModels
                .stream()
                .filter(a -> a.capabilities().contains("EMBEDDING"))
                .map(a -> a.name)
                .toList();

        if (availableChatModels.contains(name)) {
            OpenAiApi api = OpenAiApi
                    .builder()
                    .apiKey(genaiService.credentials().endpoint().apiKey())
                    .baseUrl(genaiService.credentials.endpoint().apiBase() + "/" + endpoint.wireFormat().toLowerCase())
                    .build();

            return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, OpenAiEmbeddingOptions.builder().model(name).build());
        }

        throw new RuntimeException("Unable to find embedding model with name '" + name + "'");
    }

    @Override
    public EmbeddingModel getFirstAvailableEmbeddingModel() {
        ConfigEndpoint endpoint = getEndpoint();

        String firstChatModel = endpoint.advertisedModels
                .stream()
                .filter(a -> a.capabilities().contains("EMBEDDING"))
                .map(a -> a.name)
                .toList().getFirst();

        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(genaiService.credentials().endpoint().apiKey())
                .baseUrl(genaiService.credentials.endpoint().apiBase() + "/" + endpoint.wireFormat().toLowerCase())
                .build();

        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, OpenAiEmbeddingOptions.builder().model(firstChatModel).build());
    }

    private Map<String, List<VcapService>> parse(String vcapServices) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(vcapServices, new TypeReference<Map<String, List<VcapService>>>() {});
    }

    private VcapService getGenAiService(String vcapServices) {
        try {
            Map<String, List<VcapService>> parsed = parse(vcapServices);
            if (parsed.containsKey("genai")) {
                return parsed.get("genai").get(0);
            }
            throw new RuntimeException("Unable to find genai service, found '" + parsed.keySet() + "'");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ConfigEndpoint getEndpoint() {
        RestClient client = RestClient.builder().build();
        return client.get()
                .uri(genaiService.credentials().endpoint().configUrl())
                .header("Authorization", "Bearer " + genaiService.credentials().endpoint().apiKey())
                .retrieve()
                .body(ConfigEndpoint.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VcapService(
            @JsonProperty("name") String name,
            @JsonProperty("label") String label,
            @JsonProperty("plan") String plan,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("credentials") VcapCredentials credentials) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VcapCredentials(
            @JsonProperty("endpoint") VcapEndpoint endpoint) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VcapEndpoint(
            @JsonProperty("name") String name,
            @JsonProperty("dashboard_url") String dashboardUrl,
            @JsonProperty("api_key") String apiKey,
            @JsonProperty("api_base") String apiBase,
            @JsonProperty("config_url") String configUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConfigEndpoint(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("wireFormat") String wireFormat,
            @JsonProperty("advertisedModels") List<ConfigAdvertisedModel> advertisedModels) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConfigAdvertisedModel(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("capabilities") List<String> capabilities) {}
}
