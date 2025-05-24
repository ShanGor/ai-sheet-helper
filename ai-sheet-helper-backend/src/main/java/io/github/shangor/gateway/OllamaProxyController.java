package io.github.shangor.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Slf4j
public class OllamaProxyController {

    private final Map<String, Disposable> requestPool = new ConcurrentHashMap<>();

    private final ChatModel chatModel;
    private final ToolCallbackProvider toolCallbackProvider;


    public OllamaProxyController(ChatModel chatModel,
                                 ToolCallbackProvider toolCallbackProvider) {
        this.chatModel = chatModel;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @PostMapping("/api/sheet-chat")
    public Flux<ServerSentEvent<?>> chat(@RequestBody String requestText) {
        var toolCallingManager = DefaultToolCallingManager.builder().build();
        var chatMemory = MessageWindowChatMemory.builder().build();
        String conversationId = UUID.randomUUID().toString();
        var sysPrompt = new SystemMessage("/no_think");
        var chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt(
                List.of(sysPrompt, new UserMessage(requestText)),
                chatOptions);
        return Flux.create(sink -> Thread.ofVirtual().start(() -> {
            try {
                chatMemory.add(conversationId, prompt.getInstructions());
                Prompt promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
                ChatResponse chatResponse = chatModel.call(promptWithMemory);
                chatMemory.add(conversationId, chatResponse.getResult().getOutput());

                while (chatResponse.hasToolCalls()) {
                    ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(promptWithMemory, chatResponse);
                    ToolResponseMessage output = (ToolResponseMessage) toolExecutionResult.conversationHistory().getLast();
                    String outputText = output.getResponses().getLast().responseData();
                    log.info("Done the tool call!");

                    sink.next(ServerSentEvent.builder(outputText).event("tool").build());
                    if (toolExecutionResult.returnDirect() || outputText.startsWith("Error")) {
                        sink.complete();
                    }

                    chatMemory.add(conversationId, output);
                    promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
                    chatResponse = chatModel.call(promptWithMemory);
                    chatMemory.add(conversationId, chatResponse.getResult().getOutput());
                }
                sink.next(ServerSentEvent.builder(chatResponse.getResult().getOutput().getText()).event("llm").build());
                sink.next(ServerSentEvent.builder("").event("done").build());
                sink.complete();
            } catch (Exception e) {
                sink.next(ServerSentEvent.builder("Error: " + e.getMessage()).event("error").build());
                sink.complete();
            }

        }));
    }

    private void clearRequest(String requestId) {
        requestPool.computeIfPresent(requestId, (k, v) -> {
            try {
                requestPool.remove(requestId);
                v.dispose();
                log.info("Successfully cleared request {}", requestId);
            } catch (Exception ignored) { }
            return null;
        });

    }

    @GetMapping(value = "/api/cancel/{requestId}")
    public String cancel(@PathVariable String requestId) {
        clearRequest(requestId);
        return "ok";
    }

}
