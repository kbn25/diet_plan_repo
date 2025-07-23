package com.ninja.service;

import java.util.Arrays;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class ChatServiceImpl 
{

	private final ChatClient chatClient;
	
	@Autowired
	ToolCallbackProvider tools;

	public ChatServiceImpl(ChatClient.Builder chatclientBuilder, ToolCallbackProvider tools)
	{
		this.chatClient = chatclientBuilder
							.defaultAdvisors(MessageChatMemoryAdvisor.builder(getChatMemory()).build())
							.build();
	}

	public ResponseEntity<?> getChatResponse(@ToolParam String query) 
	{	
		ChatClient.CallResponseSpec response = null;
		try {
			PromptTemplate promptTemplate = new PromptTemplate(query);
			Prompt prompt = promptTemplate.create();
			ToolCallback[] toolsToCall = getRequiredTools(query);
			if (toolsToCall != null && toolsToCall.length !=0 ) {
				response = this.chatClient.prompt(prompt)
						.toolCallbacks(tools.getToolCallbacks())							
						.call();
			}
			else
			{
				response = this.chatClient.prompt(prompt).call();
			}
			String responseText = response.content();
				return ResponseEntity.ok(Map.of("response", responseText));
		} 
		catch (Exception e) {
		    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
	    }	
	}

	public ChatMemory getChatMemory()
	{
		return MessageWindowChatMemory.builder()
				.maxMessages(10)
				.build();
	}
	
	public ToolCallback[] getRequiredTools(String prompt) 
	{
		ToolCallback[] toolList = Arrays.stream(this.tools.getToolCallbacks())
								.filter(tool -> tool.getToolDefinition().description().toLowerCase().contains(prompt.toLowerCase()) 
												|| prompt.toLowerCase().contains(tool.getToolDefinition().name().toLowerCase()))
								.toArray(ToolCallback[] :: new);
    	return toolList;
	}
	
}
