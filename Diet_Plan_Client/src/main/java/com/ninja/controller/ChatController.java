package com.ninja.controller;

import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
//import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.WebSearchOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ninja.service.ChatServiceImpl;
import com.ninja.service.CustomGeminiService;

import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/mcp")
public class ChatController {
	
	@Autowired
	private ChatServiceImpl chatServiceImpl;
	
	@Autowired
	CustomGeminiService customGeminiService;
	
	@GetMapping("/chat")
	public  ResponseEntity<?> chat(@RequestParam String query) throws JsonMappingException, JsonProcessingException
	{
		return chatServiceImpl.getChatResponse(query);
	}
	
	@GetMapping("/process")
	public  ResponseEntity<?> processPrompt(@RequestParam String query) throws JsonMappingException, JsonProcessingException
	{
		return customGeminiService.processPrompt(query);
	}

	@Autowired
	OpenAiChatModel chatModel;
	
	@Autowired
	ToolCallbackProvider tools;
	
	@GetMapping("/gemini/chat")
	public String geminiChat(@RequestParam String query) 
	{
		OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
										.model("gemini-2.0-flash")
										.toolCallbacks(tools.getToolCallbacks())
//										.toolChoice("auto")
										.build();
		Prompt prompt = new Prompt(query, chatOptions);
//		String response = chatModel
//							.call(prompt).getResult().
//							getOutput().getText();
		AssistantMessage response = chatModel.call(prompt).getResult().getOutput();
		
		boolean toolsUsed = response.getMetadata().containsKey("toolCalls");
		String toolLLM = toolsUsed ? "MCP Tool" : "LLM";
		System.out.println ("tool or LLM  " + toolLLM);
        return response.getText();
    }
}
