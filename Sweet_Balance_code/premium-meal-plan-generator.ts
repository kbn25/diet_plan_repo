
import { UserAllergy, UserMedicalCondition, UserProfile, UserCuisinePreference } from "@shared/schema";
import { GoogleGenerativeAI } from "@google/generative-ai";
import { db } from "../db";
import { sql } from "drizzle-orm";

// Set up Google Generative AI with API key
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY!);
const model = genAI.getGenerativeModel({ model: "gemini-2.0-flash" }); // Using Gemini 2.0 Flash model

function calculateBMI(height: number, weight: number): number {
  // Height in meters, weight in kg
  const heightInMeters = height / 100;
  return weight / (heightInMeters * heightInMeters);
}

// Get recent meal plans to avoid repetition
async function getRecentMealPlans(userId: number, mealType: string, days: number = 3) {
  try {
    // Temporarily disable recent meal plan checking to resolve database query issues
    // This feature will be re-enabled once database query syntax is resolved
    console.log("Recent meal plan checking temporarily disabled");
    return [];
  } catch (error) {
    console.error("Error fetching recent meal plans:", error);
    return [];
  }
}

// Generate enhanced personalized meal plan for premium users
export const generatePremiumMealPlan = async function(
  userId: number,
  profile: UserProfile,
  medicalCondition: UserMedicalCondition,
  allergies: UserAllergy[],
  cuisinePreferences: UserCuisinePreference[],
  bloodSugarReadings?: { average: number, pattern: string }, // Optional blood sugar data for premium users
  planDate?: Date // Optional date parameter to create unique plans for different days
) {
  try {
    // Calculate BMI
    const bmi = calculateBMI(profile.heightValue, profile.weightValue);

    // Get diet type from profile
    const dietType = profile.dietType || 'All-inclusive';

    // Get diabetes type from medical condition
    const diabetesType = medicalCondition.diabetesType || 'Type 2';

    // Extract cuisine preferences
    const cuisineList = cuisinePreferences.map(pref => pref.cuisine);

    // Extract allergies
    const allergyList = allergies.map(allergy => allergy.allergyType);

    // Generate premium meal plan using AI with additional parameters
    // Parse medicalConditions if it exists and extract medications
    let medications = [];
    try {
      if (medicalCondition.medicalConditions) {
        const parsedConditions = JSON.parse(medicalCondition.medicalConditions);
        // Try to extract medications if they exist in the parsed data
        if (parsedConditions && Array.isArray(parsedConditions)) {
          medications = parsedConditions
            .filter(condition => condition && condition.name)
            .map(condition => condition.name);
        }
      }
    } catch (error) {
      console.log("Error parsing medical conditions, continuing without medications:", error);
      // Continue without medications if parsing fails
    }
    
    // Extract date information if available to create a day-specific plan
    let daySpecificInfo = "";
    if (planDate) {
      const formattedDate = planDate.toLocaleDateString('en-US', { 
        weekday: 'long', 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
      });
      daySpecificInfo = `Generate this meal plan specifically for ${formattedDate}. `;
      console.log(`Generating meal plan for specific date: ${formattedDate}`);
    }
    
    return await generatePremiumMealPlanWithAI(
      userId,
      dietType, 
      bmi, 
      diabetesType, 
      allergies, 
      cuisineList, 
      profile.age,
      bloodSugarReadings,
      medications,
      daySpecificInfo  // Pass the day-specific information
    );
  } catch (error) {
    console.error("Error generating premium meal plan:", error);
    try {
      // Fall back to dynamic scientifically-based meal plan generation
      const { generateDynamicMealPlanPrompt, generateDynamicMealPlanWithAI } =
        await import("./dynamic-meal-plan-generator.ts");
      
      const prompt = generateDynamicMealPlanPrompt(
        profile,
        medicalCondition,
        allergies,
        cuisinePreferences,
        medicalCondition.diabetesType || 'Type 2'
      );
      
      return await generateDynamicMealPlanWithAI(prompt, userId, profile);
    } catch (fallbackError) {
      console.error("Fallback dynamic meal plan generation also failed:", fallbackError);
      throw new Error("Failed to generate premium personalized meal plan");
    }
  }
}

// Generate premium meal plan using AI with enhanced features
async function generatePremiumMealPlanWithAI(
  userId: number,
  dietType: string, 
  bmi: number, 
  diabetesType: string, 
  allergies: UserAllergy[], 
  cuisineList: string[],
  age: number,
  bloodSugarReadings?: { average: number, pattern: string },
  medications?: string[],
  daySpecificInfo?: string // Add this parameter for day-specific meal plans
) {
  try {
    // Extract allergy types for prompt
    const allergyStrings = allergies.map(a => a.allergyType).join(", ");

    // Use selected cuisine preferences or default to Continental if none selected
    const cuisine = cuisineList.length > 0 ? cuisineList.join(", ") : "Continental";

    // Get recent meal plans to avoid repetition
    const recentBreakfasts = await getRecentMealPlans(userId, 'breakfast', 3);
    const recentLunches = await getRecentMealPlans(userId, 'lunch', 3);
    const recentDinners = await getRecentMealPlans(userId, 'dinner', 3);
    const recentSnacks = await getRecentMealPlans(userId, 'snacks', 3);

    // Build context for avoiding repetition
    let avoidanceContext = "";
    if (recentBreakfasts.length > 0 || recentLunches.length > 0 || recentDinners.length > 0 || recentSnacks.length > 0) {
      avoidanceContext = "\n\nIMPORTANT: AVOID REPEATING THESE RECENT MEALS:\n";
      
      if (recentBreakfasts.length > 0) {
        avoidanceContext += `Recent breakfast meals (avoid these): ${recentBreakfasts.map(m => m.mainMealName).join(', ')}\n`;
      }
      if (recentLunches.length > 0) {
        avoidanceContext += `Recent lunch meals (avoid these): ${recentLunches.map(m => m.mainMealName).join(', ')}\n`;
      }
      if (recentDinners.length > 0) {
        avoidanceContext += `Recent dinner meals (avoid these): ${recentDinners.map(m => m.mainMealName).join(', ')}\n`;
      }
      if (recentSnacks.length > 0) {
        avoidanceContext += `Recent snack meals (avoid these): ${recentSnacks.map(m => m.mainMealName).join(', ')}\n`;
      }
      
      avoidanceContext += "Create completely different meal options that haven't been used in the last 3 days.\n";
    }

    // Add blood sugar and medication context for premium users
    const bloodSugarContext = bloodSugarReadings 
      ? `Their average blood sugar is ${bloodSugarReadings.average} mg/dL with a ${bloodSugarReadings.pattern} pattern.` 
      : "";
    
    const medicationContext = medications && medications.length > 0
      ? `They are currently taking: ${medications.join(", ")}.`
      : "They are not currently taking any medications.";

    // Build dietary guidelines based on diet type
    let dietaryGuidelines = "";
    const normalizedDietType = dietType.toLowerCase();
    
    if (normalizedDietType === "vegan") {
      dietaryGuidelines = `
      IMPORTANT DIETARY GUIDELINES FOR VEGAN:
      - Follow Low Fat Vegan (LFV) principles for diabetes management
      - ALL vegetables are allowed including root vegetables, sweet potatoes, yams
      - ALL fruits are allowed but limit avocados (max 1/4 medium), coconuts (max 1/8 medium), olives (1-2 pieces) to once daily
      - ALL whole grains and millets are allowed (amaranth, barnyard millet, buckwheat, finger millet, foxtail millet, etc.)
      - Dal & pulses in MODERATION: limit to once daily, preferably sprouted (moong, masoor, toor, urad, lobia, rajma, matar, chana) - avoid soy products
      - Nuts & seeds in MODERATION: one palm-sized serving daily, 2 tbsp for chia/hemp seeds
      
      COMPLETELY RESTRICTED ITEMS - NEVER INCLUDE:
      - All dairy products (ghee, butter, paneer, cheese, curd, yogurt, ice cream)
      - All seafood (fish, prawns, shellfish)
      - All meat and processed meat products
      - All eggs
      - All cooking oils (olive, coconut, soybean, corn, safflower, sunflower, rapeseed, peanut, cottonseed, canola, mustard oil)
      - All added sugars, jaggery, glucose, fructose, high fructose corn syrup, cane sugar, aspartame, corn syrup, maltose, dextrose, sorbitol, mannitol, xylitol, maltodextrin, molasses, brown rice syrup, splenda, nutrasweet, stevia, barley malt
      - Oats and oat-based products`;
    } else if (normalizedDietType === "vegetarian") {
      dietaryGuidelines = `
      IMPORTANT DIETARY GUIDELINES FOR VEGETARIAN:
      - Follow Low Fat Vegan (LFV) principles for diabetes management
      - ALL vegetables are allowed including root vegetables, sweet potatoes, yams
      - ALL fruits are allowed but limit avocados (max 1/4 medium), coconuts (max 1/8 medium), olives (1-2 pieces) to once daily
      - ALL whole grains and millets are allowed (amaranth, barnyard millet, buckwheat, finger millet, foxtail millet, etc.)
      - Dal & pulses in MODERATION: limit to once daily, preferably sprouted (moong, masoor, toor, urad, lobia, rajma, matar, chana) - avoid soy products
      - Nuts & seeds in MODERATION: one palm-sized serving daily, 2 tbsp for chia/hemp seeds
      
      COMPLETELY RESTRICTED ITEMS - NEVER INCLUDE:
      - All dairy products (ghee, butter, paneer, cheese, curd, yogurt, ice cream)
      - All seafood (fish, prawns, shellfish)
      - All meat and processed meat products
      - All eggs
      - All cooking oils (olive, coconut, soybean, corn, safflower, sunflower, rapeseed, peanut, cottonseed, canola, mustard oil)
      - All added sugars, jaggery, glucose, fructose, high fructose corn syrup, cane sugar, aspartame, corn syrup, maltose, dextrose, sorbitol, mannitol, xylitol, maltodextrin, molasses, brown rice syrup, splenda, nutrasweet, stevia, barley malt
      - Oats and oat-based products`;
    } else if (normalizedDietType === "meat-based" || normalizedDietType === "all-inclusive") {
      dietaryGuidelines = `
      IMPORTANT DIETARY GUIDELINES FOR MEAT-BASED:
      - Follow Low Carb High Fat (LCHF) principles for diabetes management
      - Vegetables: ALL leafy greens allowed (lettuce, kale, spinach, cabbage, etc.), above-ground vegetables (broccoli, cauliflower, bell peppers, mushrooms, tomatoes, eggplant)
      - RESTRICT root vegetables (yams, beets, parsnips, turnips, carrots, yuca, etc.) and pumpkin/squash
      - Use onion, garlic, turmeric, ginger only as spices in limited quantities
      - Meat & Poultry: chicken, turkey, lamb, beef, pork (grass-fed/organic preferred)
      - Seafood: fish, prawns, shellfish allowed
      - Eggs: whole eggs allowed and encouraged
      - Dairy: butter, ghee, hard cheese, paneer, cottage cheese, sour cream, Greek yogurt allowed
      - RESTRICT whole milk, low-fat milk, curd, buttermilk, ice cream, flavored milk, soft cheese
      - Nuts & seeds: ALL allowed (almonds, pistachios, brazil nuts, walnuts, pine nuts, hazelnuts, macadamia, pecans, hemp seeds, sunflower seeds, sesame seeds, chia seeds, flax seeds)
      
      COMPLETELY RESTRICTED ITEMS - NEVER INCLUDE:
      - All grains (rice, wheat, millets, jowar, bajra, corn)
      - All dal/lentils
      - All fruits except blueberry, blackberry, and limited strawberries
      - All cooking oils (soybean, corn, safflower, sunflower, rapeseed, peanut, rice bran, cottonseed, canola, mustard oil)
      - All added sugars, jaggery, glucose, fructose, high fructose corn syrup, cane sugar, aspartame, corn syrup, maltose, dextrose, sorbitol, mannitol, xylitol, maltodextrin, molasses, brown rice syrup, splenda, nutrasweet, stevia, barley malt
      - Oats and oat-based products`;
    }

    const prompt = `Generate a premium personalized diabetes-friendly meal plan for a ${age}-year-old person with ${diabetesType} diabetes with BMI ${bmi.toFixed(1)}. 
    Their diet preference is ${dietType} and cuisine preference is ${cuisine}.
    ${allergyStrings ? `They have allergies to: ${allergyStrings}.` : "They have no known food allergies."}
    ${bloodSugarContext}
    ${medicationContext}
    ${daySpecificInfo || ""}
    ${avoidanceContext}

    CRITICAL ALLERGY SAFETY REQUIREMENTS:
    ${allergyStrings ? `STRICTLY AVOID ALL FOODS CONTAINING: ${allergyStrings}. This includes any dishes, ingredients, preparations, or cooking methods that contain or may contain ${allergyStrings}. DO NOT include any meal that contains these allergens under any circumstances.` : ""}

    MEDICAL CONDITION SAFETY REQUIREMENTS:
    If the user has any medical conditions, completely exclude foods that may worsen or aggravate those conditions.

    ${dietaryGuidelines}

    ADDITIONAL DIETARY RESTRICTIONS:
    - Avoid processed food completely
    - Allow less processed cheese like goat cheese and grass-fed cheese only
    - Use less oil, dry roast spices and cook
    - Include raw fruits and raw salad (with minimal dressings) in the diet plan
    - Main course recipes should be mostly grilled, avoid adding any Indian gravy to the meal plan
    - Ensure ingredients do not include high carbs or oily recipes for the diet
    
    SPECIFIC DIETARY RATIOS:
    - For vegetarian diets (Low Fat Vegan - LFV): Fat content should not exceed 5% of total calories
    - For non-vegetarian diets (Low Carb High Fat - LCHF): Carbohydrate content should not exceed 20% of total calories

    Create a comprehensive full day's meal plan with:
    1. Pre-meal salads/appetizers specifically designed to manage blood sugar spikes
    2. Main meals with exact Indian-style measurements (katori, etc.)
    3. Healthy snacks timed to maintain steady glucose levels
    4. Detailed preparation instructions
    5. Specific timing recommendations based on typical blood sugar patterns

    Include exact portion sizes (in grams/Indian measurements), complete calories breakdown, and macronutrient information.
    STRICTLY follow the dietary guidelines and restrictions mentioned above.
    Ensure that the same main ingredient is NOT used more than once across all meals in a single day (breakfast, lunch, dinner, and snacks should have different primary ingredients).

    Format the response as a JSON object with the following structure:
    {
      "breakfast": {
        "preMealName": "Pre-breakfast item name",
        "preMealDescription": "Detailed description with ingredients and benefits",
        "preMealTime": "7:00 AM",
        "preMealCalories": 80,
        "preMealNutrients": { "carbs": "10g", "protein": "2g", "fat": "1g", "fiber": "3g" },
        "preMealPreparation": "Step-by-step preparation instructions",
        "mainMealName": "Main breakfast item name",
        "mainMealDescription": "Detailed description with complete ingredients list",
        "mainMealPortionSize": "Detailed portion size using Indian measurements",
        "mainMealTime": "7:30 AM",
        "mainMealCalories": 300,
        "mainMealNutrients": { "carbs": "40g", "protein": "15g", "fat": "10g", "fiber": "5g" },
        "mainMealPreparation": "Step-by-step preparation instructions",
        "totalCalories": 380,
        "glycemicImpact": "Low/Medium/High",
        "diabetesManagementTips": "Specific advice for this meal"
      },
      "lunch": { similar structure },
      "dinner": { similar structure },
      "snacks": {
        "preMealName": "Pre-snack name", 
        "preMealDescription": "Detailed description with ingredients and benefits",
        "preMealTime": "4:00 PM",
        "preMealCalories": 50,
        "preMealNutrients": { "carbs": "5g", "protein": "2g", "fat": "1g", "fiber": "2g" },
        "preMealPreparation": "Step-by-step preparation instructions",
        "mainMealName": "Snack name",
        "mainMealDescription": "Detailed description with complete ingredients list",
        "mainMealPortionSize": "Portion size description using Indian measurements",
        "mainMealTime": "4:30 PM",
        "mainMealCalories": 150,
        "mainMealNutrients": { "carbs": "15g", "protein": "8g", "fat": "5g", "fiber": "3g" },
        "mainMealPreparation": "Step-by-step preparation instructions",
        "totalCalories": 200,
        "glycemicImpact": "Low/Medium/High",
        "diabetesManagementTips": "Specific advice for this snack"
      }
    }

    Ensure that meals are suitable for diabetes management, with appropriate portion sizes based on BMI. For high BMI (>25), suggest smaller portions. Include regional Indian preparations and spices known to help with blood sugar management (like fenugreek, cinnamon, etc.).
    `;

    const result = await model.generateContent(prompt);
    const responseText = result.response.text();
    console.log("Gemini API response for premium meal plan:", responseText);

    // Extract JSON from response text (in case it contains additional text)
    const jsonMatch = responseText.match(/(\{[\s\S]*\})/);
    if (jsonMatch && jsonMatch[0]) {
      try {
        const mealPlan = JSON.parse(jsonMatch[0]);
        return mealPlan;
      } catch (parseError) {
        console.error("Error parsing JSON from Gemini response for premium meal plan:", parseError);
        // Fall back to static premium meal plan since we don't have all required parameters here
        return getPremiumFallbackMealPlan();
      }
    } else {
      console.error("No valid JSON found in Gemini response for premium meal plan");
      return getPremiumFallbackMealPlan();
    }
  } catch (aiError) {
    console.error("Error calling Gemini API for premium meal plan:", aiError);
    return getPremiumFallbackMealPlan();
  }
}

// Enhanced fallback meal plan for premium users
function getPremiumFallbackMealPlan() {
  return {
    breakfast: {
      preMealName: "Fresh fruit salad with fenugreek seeds",
      preMealDescription: "A mix of low-glycemic fruits with blood sugar regulating fenugreek seeds",
      preMealTime: "7:00 AM",
      preMealCalories: 60,
      preMealNutrients: { carbs: "12g", protein: "2g", fat: "0g", fiber: "4g" },
      preMealPreparation: "Mix diced apple, berries, and sprinkle with 1 tsp roasted fenugreek powder",
      mainMealName: "Unable to generate custom meal plan",
      mainMealDescription: "Please try again later",
      mainMealPortionSize: "Please try again later",
      mainMealTime: "7:30 AM",
      mainMealCalories: 250,
      mainMealNutrients: { carbs: "30g", protein: "12g", fat: "8g", fiber: "5g" },
      mainMealPreparation: "Please try again later",
      totalCalories: 310,
      glycemicImpact: "Medium",
      diabetesManagementTips: "Have fenugreek seeds 15 minutes before the main breakfast"
    },
    lunch: {
      preMealName: "Cucumber raita with roasted cumin",
      preMealDescription: "Cooling yogurt-based appetizer that helps regulate post-meal glucose spikes",
      preMealTime: "12:30 PM",
      preMealCalories: 70,
      preMealNutrients: { carbs: "6g", protein: "4g", fat: "3g", fiber: "1g" },
      preMealPreparation: "Mix 1/2 cup yogurt with grated cucumber, add roasted cumin and a pinch of salt",
      mainMealName: "Unable to generate custom meal plan",
      mainMealDescription: "Please try again later",
      mainMealPortionSize: "Please try again later",
      mainMealTime: "1:00 PM",
      mainMealCalories: 350,
      mainMealNutrients: { carbs: "45g", protein: "15g", fat: "10g", fiber: "7g" },
      mainMealPreparation: "Please try again later",
      totalCalories: 420,
      glycemicImpact: "Medium",
      diabetesManagementTips: "Have the raita 15 minutes before your main meal to slow carbohydrate absorption"
    },
    dinner: {
      preMealName: "Mixed vegetable soup with cinnamon",
      preMealDescription: "Warm, spiced vegetable soup with blood sugar regulating cinnamon",
      preMealTime: "7:30 PM",
      preMealCalories: 60,
      preMealNutrients: { carbs: "10g", protein: "2g", fat: "1g", fiber: "4g" },
      preMealPreparation: "Simmer mixed vegetables in light vegetable broth with a cinnamon stick and black pepper",
      mainMealName: "Unable to generate custom meal plan",
      mainMealDescription: "Please try again later",
      mainMealPortionSize: "Please try again later",
      mainMealTime: "8:00 PM",
      mainMealCalories: 320,
      mainMealNutrients: { carbs: "35g", protein: "18g", fat: "12g", fiber: "8g" },
      mainMealPreparation: "Please try again later",
      totalCalories: 380,
      glycemicImpact: "Low",
      diabetesManagementTips: "Keep dinner lighter than lunch to improve overnight glucose control"
    },
    snacks: {
      preMealName: "Fenugreek tea",
      preMealDescription: "Herbal tea with blood sugar regulating properties",
      preMealTime: "4:00 PM",
      preMealCalories: 5,
      preMealNutrients: { carbs: "1g", protein: "0g", fat: "0g", fiber: "0g" },
      preMealPreparation: "Steep 1 tsp fenugreek seeds in hot water for 5 minutes, strain and drink",
      mainMealName: "Roasted chana with spices",
      mainMealDescription: "Protein-rich roasted chickpeas with diabetes-friendly spices",
      mainMealPortionSize: "1 small katori (approximately 30g)",
      mainMealTime: "4:30 PM",
      mainMealCalories: 150,
      mainMealNutrients: { carbs: "18g", protein: "8g", fat: "5g", fiber: "5g" },
      mainMealPreparation: "Roast chana with a pinch of turmeric, cumin and black pepper",
      totalCalories: 155,
      glycemicImpact: "Low",
      diabetesManagementTips: "This snack helps prevent pre-dinner blood sugar drops"
    }
  };
}
