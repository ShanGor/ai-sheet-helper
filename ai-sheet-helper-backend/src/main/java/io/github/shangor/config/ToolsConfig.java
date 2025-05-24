package io.github.shangor.config;

import io.github.shangor.service.WebContentRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ToolsConfig {
    private final WebContentRetrievalService webContentRetrievalService;

    @Bean
    public ToolCallbackProvider tools() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(webContentRetrievalService)
                .build();
    }
}
