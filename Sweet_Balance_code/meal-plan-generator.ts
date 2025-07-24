import {
  UserAllergy,
  UserMedicalCondition,
  UserProfile,
  UserCuisinePreference,
} from "@shared/schema";
import { GoogleGenerativeAI } from "@google/generative-ai";
import { db } from "../db";

// Set up Google Generative AI with API key
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY!);
const model = genAI.getGenerativeModel({ model: "gemini-2.0-flash" }); // Changed model here

function calculateBMI(height: number, weight: number): number {
  // Height in meters, weight in kg
  const heightInMeters = height / 100;
  return weight / (heightInMeters * heightInMeters);
}

// Get recent meal plans to avoid repetition
async function getRecentMealPlans(
  userId: number,
  mealType: string,
  days: number = 3,
) {
  try {
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - days);

    const query = `
      SELECT ai_generated_plan, date 
      FROM free_meal_plans 
      WHERE user_id = $1 
        AND meal_type = $2 
        AND date >= $3 
      ORDER BY date DESC
    `;

    const result = await db.query(query, [
      userId,
      mealType,
      cutoffDate.toISOString().split("T")[0],
    ]);

    const recentMeals = [];
    for (const row of result.rows) {
      try {
        const planData =
          typeof row.ai_generated_plan === "string"
            ? JSON.parse(row.ai_generated_plan)
            : row.ai_generated_plan;

        if (planData?.mainMealName) {
          recentMeals.push({
            mainMealName: planData.mainMealName,
            date: row.date,
          });
        }
      } catch (parseError) {
        console.log(
          "Error parsing meal plan for repetition check:",
          parseError,
        );
      }
    }

    return recentMeals;
  } catch (error) {
    console.error("Error fetching recent meal plans:", error);
    return [];
  }
}

// Generate personalized meal plan based on user profile and preferences
export async function generatePersonalizedMealPlan(
  userId: number,
  profile: UserProfile,
  medicalCondition: UserMedicalCondition,
  allergies: UserAllergy[],
  cuisinePreferences: UserCuisinePreference[],
) {
  try {
    // Calculate BMI
    const bmi = calculateBMI(profile.heightValue, profile.weightValue);

    // Get diet type from profile
    const dietType = profile.dietType || "LCHF";

    // Get diabetes type from medical condition
    const diabetesType = medicalCondition.diabetesType || "Type 2";

    // Extract cuisine preferences
    const cuisineList = cuisinePreferences.map((pref) => pref.cuisine);

    // Extract allergies
    const allergyList = allergies.map((allergy) => allergy.allergyType);

    // Try RAG meal plan generator first, fall back to dynamic generation if it fails
    try {
      const { generateRAGMealPlan } = await import(
        "./rag-meal-plan-generator.ts"
      );
      return await generateRAGMealPlan(
        userId,
        profile,
        medicalCondition,
        allergies,
        cuisinePreferences,
      );
    } catch (ragError: any) {
      console.log(
        "RAG meal plan generation failed, falling back to dynamic generation:",
        ragError.message,
      );

      // Fall back to dynamic scientifically-based meal plan generation
      const { generateDynamicMealPlanPrompt, generateDynamicMealPlanWithAI } =
        await import("./dynamic-meal-plan-generator.ts");

      // Generate scientifically-based prompt considering all user factors
      const dynamicPrompt = generateDynamicMealPlanPrompt(
        profile,
        medicalCondition,
        allergies,
        cuisinePreferences,
        diabetesType,
      );

      return await generateDynamicMealPlanWithAI(
        dynamicPrompt,
        userId,
        profile,
      );
    }
  } catch (error) {
    console.error("Error generating meal plan:", error);
    throw new Error("Failed to generate personalized meal plan");
  }
}

// Generate meal plan using AI
{
  /*async function generateMealPlanWithAI(
  dietType: string,
  bmi: number,
  diabetesType: string,
  allergies: UserAllergy[],
  cuisineList: string[],
  age: number,
  userId?: number,
) {
  try {
    // Extract allergy types for prompt
    const allergyStrings = allergies.map((a) => a.allergyType).join(", ");

    // Use selected cuisine preferences or default to Continental if none selected
    const cuisine =
      cuisineList.length > 0 ? cuisineList.join(", ") : "Continental";

    // Get recent meal plans to avoid repetition (only if userId is provided)
    let avoidanceContext = "";
    if (userId) {
      const recentBreakfasts = await getRecentMealPlans(userId, "breakfast", 3);
      const recentLunches = await getRecentMealPlans(userId, "lunch", 3);
      const recentDinners = await getRecentMealPlans(userId, "dinner", 3);
      const recentSnacks = await getRecentMealPlans(userId, "snacks", 3);

      // Build context for avoiding repetition
      if (
        recentBreakfasts.length > 0 ||
        recentLunches.length > 0 ||
        recentDinners.length > 0 ||
        recentSnacks.length > 0
      ) {
        avoidanceContext =
          "\n\nIMPORTANT: AVOID REPEATING THESE RECENT MEALS:\n";

        if (recentBreakfasts.length > 0) {
          avoidanceContext += `Recent breakfast meals (avoid these): ${recentBreakfasts.map((m) => m.mainMealName).join(", ")}\n`;
        }
        if (recentLunches.length > 0) {
          avoidanceContext += `Recent lunch meals (avoid these): ${recentLunches.map((m) => m.mainMealName).join(", ")}\n`;
        }
        if (recentDinners.length > 0) {
          avoidanceContext += `Recent dinner meals (avoid these): ${recentDinners.map((m) => m.mainMealName).join(", ")}\n`;
        }
        if (recentSnacks.length > 0) {
          avoidanceContext += `Recent snack meals (avoid these): ${recentSnacks.map((m) => m.mainMealName).join(", ")}\n`;
        }

        avoidanceContext +=
          "Create completely different meal options that haven't been used in the last 3 days.\n";
      }
    }

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

    const prompt = `Generate a personalized diabetes-friendly meal plan for a ${age}-year-old person with ${diabetesType} diabetes with BMI ${bmi.toFixed(1)}. 
    Their diet preference is ${dietType} and cuisine preference is ${cuisine}.
    ${allergyStrings ? `They have allergies to: ${allergyStrings}.` : "They have no known food allergies."}
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
    - For LFV (Low Fat Vegan) diets: Fat content should not exceed 5% of total calories
    - For LCHF (Low Carb High Fat) diets: Carbohydrate content should not exceed 20% of total calories

    Create a full day's meal plan with pre-meal salads/appetizers, main meals, and snacks. Include exact portion sizes, calories, and timing.
    For each meal, provide detailed nutritional information including carbs, protein, fat, and fiber content.
    STRICTLY follow the dietary guidelines and restrictions mentioned above.
    Ensure that the same main ingredient is NOT used more than once across all meals in a single day (breakfast, lunch, dinner, and snacks should have different primary ingredients).

    Format the response as a JSON object with the following structure:
    {
      "breakfast": {
        "preMealName": "Pre-breakfast item name",
        "preMealTime": "7:00 AM",
        "preMealCalories": 80,
        "mainMealName": "Main breakfast item name",
        "mainMealPortionSize": "Detailed portion size",
        "mainMealTime": "7:30 AM",
        "mainMealCalories": 300,
        "totalCalories": 380,
        "mainMealNutrients": {
          "carbs": "30g", 
          "protein": "12g", 
          "fat": "8g", 
          "fiber": "5g"
        },
        "carbs": 30,
        "protein": 12,
        "fat": 8,
        "fiber": 5
      },
      "lunch": { similar structure },
      "dinner": { similar structure },
      "snacks": {
        "preMealName": "Pre-snack name", 
        "preMealTime": "4:00 PM",
        "preMealCalories": 50,
        "mainMealName": "Snack name",
        "mainMealPortionSize": "Portion size description",
        "mainMealTime": "4:30 PM",
        "mainMealCalories": 150,
        "totalCalories": 200,
        "mainMealNutrients": {
          "carbs": "20g", 
          "protein": "5g", 
          "fat": "3g", 
          "fiber": "2g"
        },
        "carbs": 20,
        "protein": 5,
        "fat": 3,
        "fiber": 2
      }
    }

    Ensure that meals are suitable for diabetes management, with appropriate portion sizes based on BMI. For high BMI (>25), suggest smaller portions. Always include nutritional information for each meal.
    `;

    const result = await model.generateContent(prompt);
    const responseText = result.response.text();
    console.log("Gemini API response:", responseText);

    // Extract JSON from response text (in case it contains additional text)
    const jsonMatch = responseText.match(/(\{[\s\S]*\})/);
    if (jsonMatch && jsonMatch[0]) {
      try {
        const mealPlan = JSON.parse(jsonMatch[0]);
        return mealPlan;
      } catch (parseError) {
        console.error("Error parsing JSON from Gemini response:", parseError);
        // Return a basic error meal plan
        return getFallbackMealPlan();
      }
    } else {
      console.error("No valid JSON found in Gemini response");
      return getFallbackMealPlan();
    }
  } catch (aiError) {
    console.error("Error calling Gemini API:", aiError);
    // Return a basic error meal plan
    return getFallbackMealPlan();
  }
}

// Fallback meal plan to use when AI generation fails
function getFallbackMealPlan() {
  return {
    breakfast: {
      preMealName: "Fresh fruit salad",
      preMealTime: "7:00 AM",
      preMealCalories: 60,
      mainMealName: "Unable to generate custom meal plan",
      mainMealPortionSize: "Please try again later",
      mainMealTime: "7:30 AM",
      mainMealCalories: 250,
      totalCalories: 310,
      mainMealNutrients: {
        carbs: "30g",
        protein: "15g",
        fat: "10g",
        fiber: "5g",
      },
      carbs: 30,
      protein: 15,
      fat: 10,
      fiber: 5,
    },
    lunch: {
      preMealName: "Mixed greens salad",
      preMealTime: "12:30 PM",
      preMealCalories: 70,
      mainMealName: "Unable to generate custom meal plan",
      mainMealPortionSize: "Please try again later",
      mainMealTime: "1:00 PM",
      mainMealCalories: 350,
      totalCalories: 420,
      mainMealNutrients: {
        carbs: "45g",
        protein: "20g",
        fat: "15g",
        fiber: "7g",
      },
      carbs: 45,
      protein: 20,
      fat: 15,
      fiber: 7,
    },
    dinner: {
      preMealName: "Cucumber raita",
      preMealTime: "7:30 PM",
      preMealCalories: 60,
      mainMealName: "Unable to generate custom meal plan",
      mainMealPortionSize: "Please try again later",
      mainMealTime: "8:00 PM",
      mainMealCalories: 320,
      totalCalories: 380,
      mainMealNutrients: {
        carbs: "40g",
        protein: "18g",
        fat: "15g",
        fiber: "6g",
      },
      carbs: 40,
      protein: 18,
      fat: 15,
      fiber: 6,
    },
    snacks: {
      preMealName: "Herbal tea",
      preMealTime: "4:00 PM",
      preMealCalories: 5,
      mainMealName: "Unable to generate custom meal plan",
      mainMealPortionSize: "Please try again later",
      mainMealTime: "4:30 PM",
      mainMealCalories: 150,
      totalCalories: 155,
      mainMealNutrients: {
        carbs: "20g",
        protein: "5g",
        fat: "3g",
        fiber: "2g",
      },
      carbs: 20,
      protein: 5,
      fat: 3,
      fiber: 2,
    },
  };
}*/
}
