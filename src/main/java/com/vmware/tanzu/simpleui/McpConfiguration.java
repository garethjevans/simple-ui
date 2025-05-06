package com.vmware.tanzu.simpleui;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfiguration {

    @Bean
    @ConditionalOnClass(ToolCallbackProvider.class)
    public ToolCallbackProvider toolCallbackProvider() {
        return () -> new ToolCallback[0];
    }
}
