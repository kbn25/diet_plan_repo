# diet_plan_repo
Changes to be done in app property
. Gemini API KEY
. Server name if you are using different server

Client port: 7070
Server Port : 8090

Working Endpoints:
/api/mcp/gemini/chat --------- Will invoke server tools
/api/mcp/process   --------- Will get data from LLM weather food related or Diet Plan


Prompts--
/api/mcp/gemini/chat --- Get all allowed foods for LFV diet (OK and Moderation)

/api/mcp/process ------ Based on above data generate a personalized diabetes-friendly meal plan for a 40-year-old person with Type 2 diabetes 
with BMI 19.1f. Their diet preference is LFV (Low Fat Vegan) and cuisine preference is Vegan.  
  CRITICAL ALLERGY SAFETY REQUIREMENTS:  They have allergies to: Diary
  MEDICAL CONDITION SAFETY REQUIREMENTS: If the user has any medical conditions, completely exclude foods that may worsen or aggravate those conditions. 
  ADDITIONAL DIETARY RESTRICTIONS:  
  - For Low Fat Vegan diets: Refer above allowed food guidelines  
  Create a full day's meal plan with pre-meal salads/appetizers, main meals, and snacks. Include exact portion sizes, calories, and timing.  
  For each meal, provide detailed nutritional information including carbs, protein, fat, and fiber content.  
  STRICTLY follow the dietary guidelines and restrictions mentioned above.  
  Ensure that the same main ingredient is NOT used more than once across all meals in a single day.  
  Format the response as a JSON object with the specified structure.
