package com.ninja.service;

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
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
	private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

	private final ChatClient chatClient;
	
	@Autowired
	ToolCallbackProvider tools;

	public ChatServiceImpl(ChatClient.Builder chatclientBuilder, ToolCallbackProvider tools) throws Exception
	{
		this.chatClient = chatclientBuilder
							.defaultAdvisors(MessageChatMemoryAdvisor.builder(getChatMemory()).build())
							.build();
	}

	public ResponseEntity<?> getChatResponse(@ToolParam String query) throws Exception
	{	
		logger.debug("Inside Service.....");
//		ChatClient.CallResponseSpec response = null;
		ChatResponse response = null;
		try {
			PromptTemplate promptTemplate = new PromptTemplate(query);
			Prompt prompt = promptTemplate.create();
			ToolCallback[] toolsToCall = getRequiredTools(query);
			logger.debug("Tools to be called....." + toolsToCall + "tools lenth " + toolsToCall.length);
			
			if (toolsToCall != null && toolsToCall.length !=0 ) {
				logger.debug("Calling tools.... " );
				response = this.chatClient.prompt(prompt)						
//						.toolCallbacks(tools.getToolCallbacks())	
						.toolCallbacks(toolsToCall)
						.call().chatResponse();
			}
			else
			{
				logger.debug("Calling Generic LLM.....");
				response = this.chatClient.prompt(prompt).call().chatResponse();
			}
//			String responseText = response. content();
			
			
			logger.debug("Token Distribution.... ");
			logger.debug("Prompt Tokens " + response.getMetadata().getUsage().getPromptTokens());
//			logger.debug("Prompt Tokens " + response.chatResponse().getMetadata().getUsage().getCompletionTokens());
//			logger.debug("Prompt Tokens " + response.chatResponse().getMetadata().getUsage().getTotalTokens());

			return ResponseEntity.ok(Map.of("response", response));
		} 
		catch (Exception e) {
			throw new Exception("Exception in Service.... " + e.getMessage());
//		    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); 
	    }	
	}

	public ChatMemory getChatMemory() throws Exception
	{
		return MessageWindowChatMemory.builder()
				.maxMessages(10)
				.build();
	}
	
	public ToolCallback[] getRequiredTools(String prompt) throws Exception
	{
		logger.debug("Inside getRequiredTools....");
		ToolCallback[] toolList = Arrays.stream(this.tools.getToolCallbacks())
								.filter(tool -> tool.getToolDefinition().description().toLowerCase().contains(prompt.toLowerCase()) 
												|| prompt.toLowerCase().contains(tool.getToolDefinition().name().toLowerCase()))
								.toArray(ToolCallback[] :: new);
		logger.debug("toolList " + toolList + "toollist length " +  toolList.length);
    	return toolList;
	}	
	
//	public ToolCallback[] getRequiredTools(String prompt) 
//	{
//		logger.debug("Inside getRequiredTools....");
//		    ToolCallback[] toolList = Arrays.stream(this.tools.getToolCallbacks())
//		        .filter(tool -> {
//		            String desc = tool.getToolDefinition().description().toLowerCase();
//		            String promptLower = prompt.toLowerCase();
//
//		            if (promptLower.contains("lfv")) {
//		                return desc.toLowerCase().contains("lfv") && desc.toLowerCase().contains("ok and moderation");
//		            }
//
//		            else if (promptLower.contains("lchf")) {
//		                return desc.toLowerCase().contains("lchf") && desc.toLowerCase().contains("ok and recommended");
//		            }
//		           
//		            
//		            return false;
//		        })
//		        .toArray(ToolCallback[]::new); 
//			logger.debug("toolList " + toolList + "toollist length " +  toolList.length);
//
//		    return toolList;
//	}
}
