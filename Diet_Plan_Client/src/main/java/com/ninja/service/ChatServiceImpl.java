package com.ninja.service;

import java.util.Arrays;
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
		ChatResponse chatResponse;
		logger.debug("Inside Service.....");
		try {
			PromptTemplate promptTemplate = new PromptTemplate(query);
			Prompt prompt = promptTemplate.create();
			
//			Calling the method to find the tool and returns the tool callback
			ToolCallback[] toolsToCall = getRequiredTools(query);			
			logger.debug("Tools to be called.....{} tools Length :{}", toolsToCall, toolsToCall.length);
			
			if (toolsToCall != null && toolsToCall.length !=0 ) {
				logger.debug("Calling tools...." );
				chatResponse = this.chatClient.prompt(prompt)
		                .toolCallbacks(toolsToCall)
		                .call()
		                .chatResponse();    
			}
			else
			{
				logger.debug("Calling Generic LLM.....");
				chatResponse = this.chatClient.prompt(prompt).call().chatResponse();
			}
			
			logger.debug("Token Distribution.... ");
			logger.debug("Prompt Tokens: {}", chatResponse.getMetadata().getUsage().getPromptTokens());
			 // Extract the clean response content
	        String content = chatResponse.getResult().getOutput().getText();        
	      
				
			return ResponseEntity.ok(Map.of("response", chatResponse.getResult()));
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
	        System.out.println("\nbefore prompt" + query);

//	        Prompt prompt = new PromptTemplate(query).create();	    
	        Prompt prompt = new Prompt(query);
	        System.out.println("after prompt");
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

//        	List<Map<String, Object>> foodList = objMapper.readValue(
//        		    toolResponse,
//        		    new TypeReference<List<Map<String, Object>>>() {}
//        		);
//
//        	// Now you can use foodList directly
//        	foodList.forEach(System.out::println);
        	
            StringBuilder prompt = new StringBuilder();
 //           prompt.append(finalJsonArray);
//        	prompt.append("Generate a personalized diabetes-friendly seven-day meal plan with detailed recipes for a ")
//        	      .append(age).append("-year-old person with ")
//        	      .append(diabeticType).append(" diabetes and a BMI of ").append(bmi).append(". ")
//        	      .append("Cuisine preference: ").append(cuisineType).append(".\n\n")
//        	      .append("MEDICAL CONDITION SAFETY REQUIREMENTS:\n")
//        	      .append(otherMedicalConditions).append(". Completely exclude foods that may worsen or aggravate these conditions.\n\n")
//        	      .append("ADDITIONAL DIETARY RESTRICTIONS:\n")
//        	      .append(otherFoodRestrictions).append(".\n\n")
//        	      .append("This meal plan must strictly follow a ").append(dietType).append(" diet. Refer the food and nutrients from the below list:\n")
//        	      .append(foodList).append("\n")
//        	      .append("For each day, create:\n")
//        	      .append("- Pre-meal salads or appetizers\n")
//        	      .append("- Main meals (breakfast, lunch, dinner)\n")
//        	      .append("- Snacks\n\n")
//        	      .append("Each meal must include:\n")
//        	      .append("- Exact portion sizes\n")
//        	      .append("- Calories per day\n")
//        	      .append("- Suggested meal timing\n")
//        	      .append("- Nutritional breakdown (carbohydrates, protein, fat, and fiber)\n\n")
//        	      .append("STRICTLY follow the dietary guidelines and restrictions mentioned above.\n")
//        	      .append("Ensure that the same main ingredient is NOT used more than once across all meals in a single day.\n\n")
//        	      .append("Format the response as a JSON object with the specified structure.");
//        	
  
   
            prompt.append("Generate a personalized diabetes-friendly seven-day meal plan with detailed recipes for a ")
                .append(age).append("-year-old person with ").append(diabeticType).append(" diabetes and a BMI of ").append(bmi).append(". ")
                .append(" (maintenance/weight loss/weight gain).\n")
                .append("Cuisine preference: ").append(cuisineType).append(" with authentic flavors and traditional cooking methods.\n\n")
                
                .append("=== CRITICAL MEDICAL SAFETY REQUIREMENTS ===\n")
                .append("Primary condition: ").append(diabeticType).append(" diabetes\n")
                .append("Additional medical conditions: ").append(otherMedicalConditions).append("\n")
                .append("MANDATORY: Completely exclude ALL foods that may worsen diabetes control or interact with diabetes medications. ")         
                
                .append("=== DIETARY RESTRICTIONS & ALLERGENS ===\n")
                .append("Food allergies: ").append(allergens).append("\n")
                .append("Food restrictions: ").append(otherFoodRestrictions).append("\n")
                .append("STRICT COMPLIANCE: Every ingredient must be verified against these restrictions.\n\n")
                
                .append("=== LCHF NUTRITIONAL REQUIREMENTS ===\n")
                .append("**STRICT LCHF MACRONUTRIENT TARGETS:**\n")
                .append("- **Net Carbohydrates: 20-50g daily MAX** (Total carbs minus fiber)\n")
                .append("- **Fat: 70-80% of total calories** (prioritize healthy fats)\n")
                .append("- **Protein: 15-25% of total calories** (adequate for muscle maintenance)\n")
                .append("- **Fiber: 25-35g daily** (from low-carb vegetables)\n")
                .append("- **Total daily calories:** Calculate based on age, BMI, and weight goal\n")
                .append("- **Sodium: <2300mg daily** (unless medical conditions require lower)\n\n")
                
                .append("**LCHF-APPROVED FOOD PRIORITIES (OIL-FREE):**\n")     
                .append(" **STRICTLY AVOID:** All oils, grains, legumes, high-carb fruits, starchy vegetables, sugars\n\n")
                
                .append("=== INGREDIENT CONSTRAINTS ===\n")
                .append("ONLY use ingredients from this approved list:\n")
                .append(toolResponse).append("\n") 
 //               .append(foodList).append("\n")
                .append("**LCHF Verification:** Each ingredient must have <5g net carbs per 100g serving (except for small amounts of herbs/spices).\n")
                .append("**OIL-FREE REQUIREMENT:** Absolutely NO cooking oils, vegetable oils, or processed fats allowed. All fats must come from whole food sources only.\n\n")
                
                .append("=== LCHF MEAL STRUCTURE REQUIREMENTS ===\n")
                .append("**Daily Structure:** 2-3 main meals + 1-2 fat-rich snacks\n")
                .append("**Intermittent Fasting Compatible:** Allow 12-16 hour fasting windows if beneficial\n\n")
                
                .append("**BREAKFAST (8:00 AM)** - Fat-Rich Start\n")
                .append("- High-fat, moderate protein, minimal carbs (<5g net)\n")
                .append("- Examples: Avocado-based dishes, nut/seed combinations, fatty fish\n")
                .append("- Give a detailed preparation method \n")
                .append("- Target: 400-500 calories, 30-40g fat, 15-20g protein\n\n")
                
                .append("**PRE-LUNCH APPETIZER (12:30 PM)** - Vegetable Focus\n")
                .append("- Raw or lightly cooked low-carb vegetables with healthy fats\n")
                .append("- Give a detailed preparation method \n")
                .append("- Target: <3g net carbs, high fiber\n\n")
                
                .append("**LUNCH (1:00 PM)** - Protein + Fat Main\n")
                .append("- Substantial protein with healthy fats and low-carb vegetables\n")
                .append("- Give a detailed preparation method \n")
                .append("- Traditional ").append(cuisineType).append(" preparation with LCHF adaptations\n")
                .append("- Target: 500-600 calories, 35-45g fat, 30-40g protein, <8g net carbs\n\n")
                
                .append("**AFTERNOON SNACK (4:00 PM)** - Whole Food Fat Sources\n")
                .append("- Give a detailed preparation method \n")
                .append("- Target: 200-300 calories, 25-30g fat, <3g net carbs\n\n")
                
                .append("**PRE-DINNER APPETIZER (7:00 PM)** - Veggie + Fat\n")
                .append("- Low-carb vegetables with fat-rich dressing/preparation\n")
                .append("- Give a detailed preparation method \n")
                .append("- Target: <2g net carbs, high fiber\n\n")                
                
                .append("**DINNER (7:30 PM)** - Light Protein + Vegetables\n")
                .append("- Lighter protein portion with abundant low-carb vegetables\n")
                .append("- Generous healthy fats for satiety\n")
                .append("- Give a detailed preparation method \n")
                
                .append("- Target: 400-500 calories, 30-35g fat, 25-30g protein, <6g net carbs\n\n")
                
                .append("=== LCHF QUALITY STANDARDS ===\n")
                .append("1. **Ketosis Support:** Each meal should support or maintain ketosis\n")
                .append("2. **Fat Quality:** Prioritize omega-3 from whole foods, monounsaturated fats from avocados/nuts, saturated fats from coconut meat\n")
                .append("3. **Carb Sources:** Only from fibrous vegetables and small amounts of berries\n")
                .append("4. **Protein Quality:** Complete amino acid profiles from whole food sources\n")
                .append("5. **Satiety Focus:** High-fat content to reduce hunger and cravings\n")
                .append("6. **Blood Sugar Stability:** Minimal glucose response from all meals\n\n")
                
                .append("=== REQUIRED NUTRITIONAL DATA PER MEAL ===\n")
                .append("- Calories (kcal)\n")
                .append("- Total carbohydrates (g)\n")
                .append("- **NET CARBOHYDRATES (g)** [CRITICAL - highlight this]\n")
                .append("- Fiber (g)\n")
                .append("- Protein (g)\n")
                .append("- Total fat (g)\n")
                .append("- Saturated fat (g)\n")
                .append("- Omega-3 fatty acids (g) [when available]\n")
                .append("- **Ketogenic ratio** [Fat:(Protein+Carbs) ratio]\n")
                .append("- Estimated blood glucose impact (Low/Very Low)\n\n")
                
                .append("=== OIL-FREE LCHF COOKING METHODS ===\n")
                .append("**APPROVED COOKING TECHNIQUES:**\n")
                .append("- **Water sautéing:** Use small amounts of water or broth instead of oil\n")
                .append("- **Dry roasting:** Toast nuts, seeds, and spices without oil\n")
                .append("- **Steaming:** Preserve nutrients in vegetables\n")
                .append("- **Grilling/Broiling:** Use natural fat from meats, no added oils\n")
                .append("- **Baking in parchment:** Oil-free baking method\n")           
                                
                .append("=== OUTPUT FORMAT ===\n")
                .append("Structure as JSON with LCHF-specific tracking:\n")
                .append("```json\n")
                .append("{\n")
                .append("  \"lchfMealPlan\": {\n")
                .append("    \"overview\": {\n")
                .append("      \"targetProfile\": \"...\",\n")
                .append("      \"lchfApproach\": \"Therapeutic ketogenic diet for diabetes management\",\n")
                .append("      \"weeklyNutritionSummary\": {\n")
                .append("        \"avgDailyNetCarbs\": \"X.Xg\",\n")
                .append("        \"avgDailyFat\": \"XX.Xg (XX%)\",\n")
                .append("        \"avgDailyProtein\": \"XX.Xg (XX%)\",\n")
                .append("        \"ketogenicRatio\": \"X.X:1\"\n")
                .append("      }\n")
                .append("    },\n")
                .append("    \"days\": [\n")
                .append("      {\n")
                .append("        \"day\": 1,\n")
                .append("        \"dailyNetCarbTotal\": \"XXg\",\n")
                .append("        \"dailyKetogenicRatio\": \"X.X:1\",\n")
                .append("        \"meals\": {...}\n")
                .append("      }\n")
                .append("    ],\n")
                .append("  }\n")
                .append("}\n")
                .append("```\n\n")
                
                .append("CRITICAL LCHF VERIFICATION: Every single ingredient and recipe must be validated to ensure:\n")
                .append("1. Net carbs per serving are within LCHF limits\n")
                .append("2. Total daily net carbs stay under 50g (preferably 20-30g)\n")
                .append("3. Fat content is optimized using ONLY whole food sources (no oils)\n")
                .append("4. No hidden carbs, processed oils, or anti-ketogenic ingredients\n")
                .append("5. Blood glucose impact is minimal for diabetes management\n")
                .append("6. All cooking methods are oil-free and use whole food fat sources");
			return prompt.toString();

}

	
}
