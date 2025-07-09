package com.ninja.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninja.utilities.Meal;
import com.ninja.utilities.MealPlan;
import com.ninja.utilities.Nutrients;




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

	@Tool
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
		return MessageWindowChatMemory.builder().build();
	}
	
	public ToolCallback[] getRequiredTools(String prompt) 
	{
		ToolCallback[] toolList = Arrays.stream(this.tools.getToolCallbacks())
								.filter(tool -> tool.getToolDefinition().description().toLowerCase().contains(prompt.toLowerCase()) 
												|| prompt.toLowerCase().contains(tool.getToolDefinition().name().toLowerCase()))
								.toArray(ToolCallback[] :: new);
    	return toolList;
	}
	
//	private boolean isToolRequired(ToolCallback tool, String prompt) {
//        String description = tool.getToolDefinition().description().toLowerCase();
//        String promptLower = prompt.toLowerCase();
//        return description.contains(promptLower) || promptLower.contains(tool.getToolDefinition().name().toLowerCase());
//    }
	
	public CompletableFuture<MealPlan> convertJSONToString(String responseText) 
	{
		ObjectMapper objMapper = new ObjectMapper();
		try {
	        MealPlan mealPlan = objMapper.readValue(responseText, MealPlan.class);
	        return CompletableFuture.completedFuture(mealPlan);
	    } catch (Exception parseError) {
	        System.err.println("Error parsing JSON from AI response: " + parseError.getMessage());
	        return CompletableFuture.completedFuture(getFallbackMealPlan());
	    }
	}
	
    public MealPlan getFallbackMealPlan() {
        return new MealPlan(
                new Meal("Fresh fruit salad", "7:00 AM", 60, "Unable to generate custom meal plan",
                        "Please try again later", "7:30 AM", 250, 310,
                        new Nutrients("30g", "15g", "10g", "5g"), 30, 15, 10, 5),
                new Meal("Mixed greens salad", "12:30 PM", 70, "Unable to generate custom meal plan",
                        "Please try again later", "1:00 PM", 350, 420,
                        new Nutrients("45g", "20g", "15g", "7g"), 45, 20, 15, 7),
                new Meal("Cucumber raita", "7:30 PM", 60, "Unable to generate custom meal plan",
                        "Please try again later", "8:00 PM", 320, 380,
                        new Nutrients("40g", "18g", "15g", "6g"), 40, 18, 15, 6),
                new Meal("Herbal tea", "4:00 PM", 5, "Unable to generate custom meal plan",
                        "Please try again later", "4:30 PM", 150, 155,
                        new Nutrients("20g", "5g", "3g", "2g"), 20, 5, 3, 2)
        );
    }


}
