package com.ninja.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CustomGeminiService 
{
	private final SyncMcpToolCallbackProvider toolCallbackProvider;
	private final ChatClient chatClient;
	
	public CustomGeminiService(SyncMcpToolCallbackProvider toolCallbackProvider, ChatClient.Builder chatclientBuilder) {
		this.toolCallbackProvider = toolCallbackProvider;
		this.chatClient = chatclientBuilder
									.defaultAdvisors(MessageChatMemoryAdvisor
									.builder(MessageWindowChatMemory.builder().build())
									.build())
									.build();
	}
	
	public ResponseEntity<?> processPrompt(String userPrompt) {
        try {
        	ToolCallback[] tools = toolCallbackProvider.getToolCallbacks();
            if (tools.length == 0) {
                return queryGemini(userPrompt);
            }

            for (ToolCallback tool : tools) {
                if (isToolRelevant(tool, userPrompt)) {
                	String input = tool.getToolDefinition().name();
                	String[] parts = input.split("_");
                	String toolName = parts[parts.length - 1];
//                    Object toolResult = tool.call(tool.getToolDefinition().name());
                    Object toolResult = tool.call(toolName);
                    return ResponseEntity.ok(Map.of("response", toolResult));
                }
            }
            // ONLY THIS CALL IS WORKING and NO TOOLs are invoked as above.... Ignore above code
            return queryGemini(userPrompt);
        } catch (Exception e) {
            return queryGemini(userPrompt);
        }
    }
	
	private boolean isToolRelevant(ToolCallback tool, String prompt) {
        String description = tool.getToolDefinition().description().toLowerCase();
        String promptLower = prompt.toLowerCase();
        return description.contains(promptLower) || promptLower.contains(tool.getToolDefinition().name().toLowerCase());
    }
	
	public ResponseEntity<?> queryGemini(String userPrompt)
	{
		ChatClient.CallResponseSpec response = this.chatClient.prompt(userPrompt).call();
		String responseText = response.content();
		return ResponseEntity.ok(Map.of("response", responseText));
	}
	
}
