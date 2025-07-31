package com.ninja.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


@Service
public class ChatServiceImpl 
{

	private final ChatClient chatClient;	
	
	@Autowired
	ToolCallbackProvider tools;

	private static final Logger logger=LoggerFactory.getLogger(ChatServiceImpl.class); 
	
	public ChatServiceImpl(ChatClient.Builder chatclientBuilder, ToolCallbackProvider tools)
	{
		this.chatClient = chatclientBuilder
							.defaultAdvisors(MessageChatMemoryAdvisor.builder(getChatMemory()).build())
							.build();
	}

	public ResponseEntity<?> getChatResponse(@ToolParam String query) 
	{	
		ChatResponse response;
		logger.debug("Inside Service.....");
		try {
			PromptTemplate promptTemplate = new PromptTemplate(query);
			Prompt prompt = promptTemplate.create();
			
//			Calling the method to find the tool and returns the tool callback
			ToolCallback[] toolsToCall = getRequiredTools(query);			
			logger.debug("Tools to be called.....{} tools Length :{}" + toolsToCall,toolsToCall.length);
			
			if (toolsToCall != null && toolsToCall.length !=0 ) {
				logger.debug("Calling tools...." );
		         response = this.chatClient.prompt(prompt)
		                .toolCallbacks(toolsToCall)
		                .call()
		                .chatResponse();    
			}
			else
			{
				logger.debug("Calling Generic LLM.....");
				response = this.chatClient.prompt(prompt).call().chatResponse();
			}
			
				logger.debug("Token Distribution.... ");
				logger.debug("Prompt Tokens " + response.getMetadata().getUsage().getPromptTokens());
				
				return ResponseEntity.ok(Map.of("response", response));
		} 
		catch (Exception e) {
		    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
	    }	
	}

	public ResponseEntity<?> getSevenDayMealPlan(		
			@RequestParam int age,
			@RequestParam float bmi,
			@RequestParam String dietType,
			@RequestParam String diabeticType,
			@RequestParam String cusineType,
			@RequestParam String allergens,
			@RequestParam String otherFoodRestrictions,
			@RequestParam String otherMedicalConditions) {

	    try {
//	        Generate the dynamic prompt
	        String query = createPromptQuery(age, bmi, dietType, diabeticType, allergens, cusineType, otherMedicalConditions, otherFoodRestrictions);

	        Prompt prompt = new PromptTemplate(query).create();	    
	        logger.debug("Genreic LLM Call.... ");	        
	        ChatResponse response = this.chatClient
	        	    								.prompt(prompt)   
	        	    								.call()
	        	    								.chatResponse();
	        
	       logger.debug("Token Distribution.... ");
	      
		   logger.debug("Tokens - Prompt: {}, Completion: {}", 	        		
	  	        	    response.getMetadata().getUsage().getPromptTokens(),
	  	        	    response.getMetadata().getUsage().getCompletionTokens());

			return ResponseEntity.ok(Map.of("response", response)); 	     

	    } catch (Exception e) {
	        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
	    }
	}


	public ChatMemory getChatMemory()
	{
		logger.debug("Attaching the mempory to the model");
		return MessageWindowChatMemory.builder()
				.maxMessages(10)
				.build();
	}
	
	public ToolCallback[] getRequiredTools(String prompt) 
	{
		logger.debug("Filter the requeired tool and returns the tool List");
		ToolCallback[] toolList = Arrays.stream(this.tools.getToolCallbacks())
								.filter(tool -> tool.getToolDefinition().description().toLowerCase().contains(prompt.toLowerCase()) 
												|| prompt.toLowerCase().contains(tool.getToolDefinition().name().toLowerCase()))
								.toArray(ToolCallback[] :: new);
    	return toolList;
	}
	
	public String createPromptQuery(int age, float bmi, String dietType,
            String diabeticType, String allergens, String cuisineType,
            String otherMedicalConditions, String otherFoodRestrictions) throws JsonMappingException, JsonProcessingException {
			
			// Filter the tool
		    Optional<ToolCallback> optTool = Arrays.stream(this.tools.getToolCallbacks())
        	    .filter(t -> t.getToolDefinition().description().contains("Find Foods Suitable for LCHF Diet Excluding Allergies"))
        	    .findFirst();
        	
		    // toolSchema stores the tool parameter values
		    ObjectMapper objMapper = new ObjectMapper();
        	ObjectNode toolSchema = objMapper.createObjectNode();
        	toolSchema.put("allergens", allergens);  
    	  
        	String toolResponse=null,toolResponseString=null;
        
        	if (optTool.isPresent()) {
        		ToolCallback tool  = optTool.get();   
//				Calling the tool with allergen parameter        	   
        	    toolResponse  = tool.call(toolSchema.toString());
        	    	
        	}
        	
//        	Convert the return reponse string into list of food items
            List<Map<String, String>> wrapperList = objMapper.readValue(toolResponse, new TypeReference<List<Map<String, String>>>() {});           
            String textJsonArray = wrapperList.get(0).get("text");
            List<String> food_items = objMapper.readValue(textJsonArray, new TypeReference<List<String>>() {});
            String finalJsonArray = objMapper.writeValueAsString(food_items);
            
//            Generating Dynamic Prompt
            StringBuilder prompt = new StringBuilder();
        	prompt.append("Generate a personalized diabetes-friendly seven-day meal plan with detailed recipes for a ")
        	      .append(age).append("-year-old person with ")
        	      .append(diabeticType).append(" diabetes and a BMI of ").append(bmi).append(". ")
        	      .append("Cuisine preference: ").append(cuisineType).append(".\n\n")
        	      .append("MEDICAL CONDITION SAFETY REQUIREMENTS:\n")
        	      .append(otherMedicalConditions).append(". Completely exclude foods that may worsen or aggravate these conditions.\n\n")
        	      .append("ADDITIONAL DIETARY RESTRICTIONS:\n")
        	      .append(otherFoodRestrictions).append(".\n\n")
        	      .append("This meal plan must strictly follow a ").append(dietType).append(" diet. Refer the food from the below list:\n")
        	      .append(finalJsonArray).append("\n")
        	      .append("For each day, create:\n")
        	      .append("- Pre-meal salads or appetizers\n")
        	      .append("- Main meals (breakfast, lunch, dinner)\n")
        	      .append("- Snacks\n\n")
        	      .append("Each meal must include:\n")
        	      .append("- Exact portion sizes\n")
        	      .append("- Calories\n")
        	      .append("- Suggested meal timing\n")
        	      .append("- Nutritional breakdown (carbohydrates, protein, fat, and fiber)\n\n")
        	      .append("STRICTLY follow the dietary guidelines and restrictions mentioned above.\n")
        	      .append("Ensure that the same main ingredient is NOT used more than once across all meals in a single day.\n\n")
        	      .append("Format the response as a JSON object with the specified structure.");
			return prompt.toString();
}

	
}
