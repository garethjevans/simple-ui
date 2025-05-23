package com.vmware.tanzu.simpleui.locator;

import com.vmware.tanzu.simpleui.locator.impl.DefaultModelLocator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

import java.util.List;

public interface ModelLocator {

    List<String> getModelNames();

    List<String> getModelNamesByCapability(String capability);

    ChatModel getChatModelByName(String name);

    ChatModel getFirstAvailableChatModel();

    ChatModel getFirstAvailableToolModel();

    EmbeddingModel getEmbeddingModelByName(String name);

    EmbeddingModel getFirstAvailableEmbeddingModel();

    List<DefaultModelLocator.McpConnectivity> getMcpServers();

}
