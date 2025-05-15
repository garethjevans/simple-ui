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

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultModelLocator implements ModelLocator {

    private final List<VcapService> genaiServices;

    public DefaultModelLocator() {
        genaiServices = getGenAiServices(System.getenv("VCAP_SERVICES"));
    }

    public DefaultModelLocator(String vcapServices) {
        genaiServices = getGenAiServices(vcapServices);
    }

    @Override
    public List<String> getModelNames() {
        List<ModelConnectivity> models = getAllConnectivityDetails(genaiServices);

        return models
                .stream()
                .map(a -> a.name)
                .toList();
    }

    @Override
    public List<String> getModelNamesByCapability(String capability) {
        List<ModelConnectivity> models = getAllConnectivityDetails(genaiServices);

        return models
                .stream()
                .filter(a -> a.capabilities().contains(capability))
                .map(a -> a.name)
                .toList();
    }

    @Override
    public ChatModel getChatModelByName(String name) {
        List<ModelConnectivity> models = getAllConnectivityDetails(genaiServices);

        ModelConnectivity connectivity = models
                .stream()
                .filter(a -> a.capabilities().contains("CHAT"))
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find chat model with name '" + name + "'"));

        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(connectivity.apiKey())
                .baseUrl(connectivity.apiBase() + "/" + connectivity.wireFormat().toLowerCase())
                .build();

        return OpenAiChatModel
                .builder()
                .defaultOptions(OpenAiChatOptions.builder().model(name).build())
                .openAiApi(api)
                .build();
    }

    @Override
    public ChatModel getFirstAvailableChatModel() {
        List<ModelConnectivity> models = getAllConnectivityDetails(genaiServices);

        ModelConnectivity connectivity = models
                .stream()
                .filter(a -> a.capabilities().contains("CHAT"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find first chat model"));

        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(connectivity.apiKey())
                .baseUrl(connectivity.apiBase() + "/" + connectivity.wireFormat().toLowerCase())
                .build();

        return OpenAiChatModel
                .builder()
                .defaultOptions(OpenAiChatOptions.builder().model(connectivity.name()).build())
                .openAiApi(api)
                .build();
    }

    @Override
    public ChatModel getFirstAvailableToolModel() {
        List<ModelConnectivity> models = getAllConnectivityDetails(genaiServices);

        ModelConnectivity connectivity = models
                .stream()
                .filter(a -> a.capabilities().contains("TOOLS"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find first chat model"));

        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(connectivity.apiKey())
                .baseUrl(connectivity.apiBase() + "/" + connectivity.wireFormat().toLowerCase())
                .build();

        return OpenAiChatModel
                .builder()
                .defaultOptions(OpenAiChatOptions.builder().model(connectivity.name()).build())
                .openAiApi(api)
                .build();
    }

    @Override
    public EmbeddingModel getEmbeddingModelByName(String name) {
        List<ModelConnectivity> models = getAllConnectivityDetails(genaiServices);

        ModelConnectivity connectivity = models
                .stream()
                .filter(a -> a.capabilities().contains("EMBEDDING"))
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find embedding model with name '" + name + "'"));

        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(connectivity.apiKey())
                .baseUrl(connectivity.apiBase() + "/" + connectivity.wireFormat().toLowerCase())
                .build();

        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, OpenAiEmbeddingOptions.builder().model(name).build());
    }

    @Override
    public EmbeddingModel getFirstAvailableEmbeddingModel() {
        List<ModelConnectivity> models = getAllConnectivityDetails(genaiServices);

        ModelConnectivity connectivity = models
                .stream()
                .filter(a -> a.capabilities().contains("EMBEDDING"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find first embedding model"));

        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(connectivity.apiKey())
                .baseUrl(connectivity.apiBase() + "/" + connectivity.wireFormat().toLowerCase())
                .build();

        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, OpenAiEmbeddingOptions.builder().model(connectivity.name()).build());
    }

    private Map<String, List<VcapService>> parse(String vcapServices) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(vcapServices, new TypeReference<Map<String, List<VcapService>>>() {});
    }

    private List<ModelConnectivity> getAllConnectivityDetails(List<VcapService> services) {
        return services
                .stream()
                .map(vs -> new AbstractMap.SimpleEntry<>(vs, getEndpoint(vs)) {})
                .flatMap(e -> e.getValue().advertisedModels.stream().map( a -> {
                    if (e.getKey().credentials().endpoint() != null) {
                        return new ModelConnectivity(
                                a.name(),
                                e.getValue().wireFormat(), a.capabilities(),
                                e.getKey().credentials().endpoint().apiKey(),
                                e.getKey().credentials().endpoint().apiBase());
                    } else {
                        return new ModelConnectivity(
                                a.name(),
                                e.getValue().wireFormat(), a.capabilities(),
                                e.getKey().credentials().apiKey(),
                                e.getKey().credentials().apiBase());
                    }
                }))
                .toList();
    }

    private List<VcapService> getGenAiServices(String vcapServices) {
        try {
            Map<String, List<VcapService>> parsed = parse(vcapServices);
            List<VcapService> services = parsed
                    .values()
                    .stream()
                    .flatMap( s-> s.stream()
                            .filter( g -> (g.tags != null && g.tags.contains("genai")) || g.label.equalsIgnoreCase("genai")))
                    .toList();
            if (!services.isEmpty()) {
                return services;
            }

            throw new RuntimeException("Unable to find genai service, found '" + parsed.keySet() + "'");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ConfigEndpoint getEndpoint(VcapService service) {
        if (service.credentials().endpoint() == null) {
            return new ConfigEndpoint(
                    service.credentials().modelName(),
                    "",
                    service.credentials().wireFormat().toUpperCase(),
                    List.of(new ConfigAdvertisedModel(
                            service.credentials().modelName(),
                            "",
                            service.credentials().modelCapabilities().stream().map(String::toUpperCase).toList())));
        } else {
            RestClient client = RestClient.builder().build();
            return client.get()
                    .uri(service.credentials().endpoint().configUrl())
                    .header("Authorization", "Bearer " + service.credentials().endpoint().apiKey())
                    .retrieve()
                    .body(ConfigEndpoint.class);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ModelConnectivity(
            String name,
            String wireFormat,
            List<String> capabilities,
            String apiKey,
            String apiBase) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VcapService(
            @JsonProperty("name") String name,
            @JsonProperty("label") String label,
            @JsonProperty("plan") String plan,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("credentials") VcapCredentials credentials) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VcapCredentials(
            @JsonProperty("endpoint") VcapEndpoint endpoint,
            @Deprecated @JsonProperty("model_name") String modelName,
            @Deprecated @JsonProperty("wire_format") String wireFormat,
            @Deprecated @JsonProperty("model_capabilities") List<String> modelCapabilities,
            @Deprecated @JsonProperty("api_key") String apiKey,
            @Deprecated @JsonProperty("api_base") String apiBase) {}

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
