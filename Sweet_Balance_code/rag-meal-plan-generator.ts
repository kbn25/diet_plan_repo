import { GoogleGenerativeAI, GoogleAIFileManager } from "@google/generative-ai";
import type { UserProfile } from "@shared/schema";
import type {
  UserAllergy,
  UserMedicalCondition,
  UserCuisinePreference,
} from "@shared/schema";
import * as fs from "fs";
import * as path from "path";
import { db } from "../db";
import { generateDynamicDietaryRecommendations } from "./dynamic-meal-plan-generator";

// Initialize the Google Generative AI client
if (!process.env.GOOGLE_GEMINI_API_KEY) {
  throw new Error(
    "GOOGLE_GEMINI_API_KEY is not defined. Please check your environment variables.",
  );
}

const genAI = new GoogleGenerativeAI(process.env.GOOGLE_GEMINI_API_KEY);
const fileManager = new GoogleAIFileManager(process.env.GOOGLE_GEMINI_API_KEY!);
const model = genAI.getGenerativeModel({ model: "gemini-2.0-flash" });

// Store uploaded file references
let uploadedPdfFiles: { lchf?: any; lfv?: any } = {};

// Upload PDF files to Gemini
async function uploadPdfFiles() {
  try {
    console.log("Uploading PDF files to Gemini...");

    const lchfPath = path.join(
      process.cwd(),
      "client/src/MealPDF's/LCHF Eradicate Diabetes .pdf",
    );
    const lfvPath = path.join(
      process.cwd(),
      "client/src/MealPDF's/LFV Eradicate Diabetes.pdf",
    );

    // Check if files exist
    if (!fs.existsSync(lchfPath)) {
      console.error(`LCHF PDF not found at: ${lchfPath}`);
      return false;
    }

    if (!fs.existsSync(lfvPath)) {
      console.error(`LFV PDF not found at: ${lfvPath}`);
      return false;
    }

    // Upload LCHF PDF
    const lchfFileData = fs.readFileSync(lchfPath);
    const uploadResultLCHF = await fileManager.uploadFile(lchfPath, {
      mimeType: "application/pdf",
      displayName: "LCHF Eradicate Diabetes",
    });
    uploadedPdfFiles.lchf = uploadResultLCHF.file;
    console.log("LCHF PDF uploaded successfully");

    // Upload LFV PDF
    const lfvFileData = fs.readFileSync(lfvPath);
    const uploadResultLFV = await fileManager.uploadFile(lfvPath, {
      mimeType: "application/pdf",
      displayName: "LFV Eradicate Diabetes",
    });
    uploadedPdfFiles.lfv = uploadResultLFV.file;
    console.log("LFV PDF uploaded successfully");

    return true;
  } catch (error) {
    console.error("Error uploading PDF files:", error);
    return false;
  }
}

// Calculate BMI
function calculateBMI(height: number, weight: number): number {
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
      FROM premium_meal_plans 
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

// Generate RAG-based meal plan
export async function generateRAGMealPlan(
  userId: number,
  profile: UserProfile,
  medicalCondition: UserMedicalCondition,
  allergies: UserAllergy[],
  cuisinePreferences: UserCuisinePreference[],
  bloodSugarReadings?: { average: number; pattern: string },
  planDate?: Date,
) {
  try {
    // Upload PDF files if not already uploaded
    if (!uploadedPdfFiles.lchf || !uploadedPdfFiles.lfv) {
      const uploadSuccess = await uploadPdfFiles();
      if (!uploadSuccess) {
        throw new Error("Failed to upload PDF files for RAG");
      }
    }

    // Calculate BMI
    const bmi = calculateBMI(profile.heightValue, profile.weightValue);

    // Get diet type from profile
    const dietType = profile.dietType || "LCHF";

    // Get diabetes type from medical condition
    const diabetesType = medicalCondition.diabetesType || "Type 2";

    // Extract cuisine preferences
    const cuisineList = cuisinePreferences.map((pref) => pref.cuisine);

    // Generate meal plan using RAG approach
    return await generateRAGMealPlanWithGemini(
      userId,
      profile,
      dietType,
      bmi,
      diabetesType,
      allergies,
      cuisineList,
      profile.age,
      bloodSugarReadings,
      medicalCondition,
      planDate,
    );
  } catch (error: any) {
    console.error("Error generating RAG meal plan:", error);
    throw new Error(
      `Failed to generate RAG meal plan: ${error?.message || "Unknown error"}`,
    );
  }
}

// Generate meal plan using RAG approach with uploaded PDFs
async function generateRAGMealPlanWithGemini(
  userId: number,
  profile: UserProfile,
  dietType: string,
  bmi: number,
  diabetesType: string,
  allergies: UserAllergy[],
  cuisineList: string[],
  age: number,
  bloodSugarReadings?: { average: number; pattern: string },
  medicalCondition?: UserMedicalCondition,
  planDate?: Date,
) {
  try {
    // Extract allergy types for prompt
    const allergyStrings = allergies.map((a) => a.allergyType).join(", ");

    // Default to Indian cuisine if no preferences
    const cuisine = cuisineList.length > 0 ? cuisineList.join(", ") : "Indian";

    // Parse medical conditions and extract medications if available
    let medications = [];
    try {
      if (medicalCondition?.medicalConditions) {
        const parsedConditions = JSON.parse(medicalCondition.medicalConditions);
        if (parsedConditions && Array.isArray(parsedConditions)) {
          medications = parsedConditions
            .filter((condition) => condition && condition.name)
            .map((condition) => condition.name);
        }
      }
    } catch (error) {
      console.log("Error parsing medical conditions:", error);
    }

    // Add blood sugar and medication context
    const bloodSugarContext = bloodSugarReadings
      ? `Their average blood sugar is ${bloodSugarReadings.average} mg/dL with a ${bloodSugarReadings.pattern} pattern.`
      : "";

    const medicationContext =
      medications && medications.length > 0
        ? `They are currently taking: ${medications.join(", ")}.`
        : "They are not currently taking any medications.";

    // Add date-specific context if provided
    let daySpecificInfo = "";
    if (planDate) {
      const formattedDate = planDate.toLocaleDateString("en-US", {
        weekday: "long",
        year: "numeric",
        month: "long",
        day: "numeric",
      });
      daySpecificInfo = `Generate this meal plan specifically for ${formattedDate}. `;
    }

    // Get recent meal plans to avoid repetition
    const recentBreakfasts = await getRecentMealPlans(userId, "breakfast", 3);
    const recentLunches = await getRecentMealPlans(userId, "lunch", 3);
    const recentDinners = await getRecentMealPlans(userId, "dinner", 3);
    const recentSnacks = await getRecentMealPlans(userId, "snacks", 3);

    // Build context for avoiding repetition
    let avoidanceContext =
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

    // Generate dynamic dietary guidelines based on user profile
    const bmi = calculateBMI(profile.heightValue, profile.weightValue);
    const dynamicRecommendations = generateDynamicDietaryRecommendations(
      profile,
      medicalCondition,
      allergies,
      bmi,
      age,
      diabetesType,
    );

    // Build dietary guidelines based on diet type with user-specific modifications
    let dietaryGuidelines = "";
    let relevantPdf = null;

    const normalizedDietType = dietType.toLowerCase();
    
    if (normalizedDietType === "vegan") {
      dietaryGuidelines = `
      IMPORTANT DIETARY GUIDELINES FOR VEGAN:
      - Follow Low Fat Vegan (LFV) principles for diabetes management
      - ALL vegetables are allowed including root vegetables, sweet potatoes, yams
      - ALL fruits are allowed but limit avocados (max 1/4 medium), coconuts (max 1/8 medium), olives (1-2 pieces) to once daily
      - ALL whole grains and millets are allowed (amaranth, barnyard millet, buckwheat, finger millet, foxtail millet, etc.)
      - Dal & pulses in MODERATION: limit to once daily, preferably sprouted (moong, masoor, toor, urad, lobia, rajma, matar, chana)
      - Nuts & seeds in MODERATION: one palm-sized serving daily, 2 tbsp for chia/hemp seeds

      COMPLETELY RESTRICTED ITEMS - NEVER INCLUDE:
      - All dairy products (ghee, butter, paneer, cheese, curd, yogurt, ice cream)
      - All seafood (fish, prawns, shellfish)
      - All meat and processed meat products
      - All eggs
      - All cooking oils (olive, coconut, soybean, corn, safflower, sunflower, rapeseed, peanut, cottonseed, canola, mustard oil)
      - All added sugars, jaggery, glucose, fructose, high fructose corn syrup, cane sugar, aspartame, corn syrup, maltose, dextrose, sorbitol, mannitol, xylitol, maltodextrin, molasses, brown rice syrup, splenda, nutrasweet, stevia, barley malt
      - Oats and oat-based products
      USER-SPECIFIC DIETARY RESTRICTIONS:
      ${
        dynamicRecommendations.allergenExclusions.length > 0
          ? dynamicRecommendations.allergenExclusions
              .map((exclusion) => `- ${exclusion}`)
              .join("\n")
          : "- No specific allergen restrictions for this user"
      }
      
      PORTION ADJUSTMENTS FOR THIS USER:
      ${dynamicRecommendations.portionAdjustments}`;
      relevantPdf = uploadedPdfFiles.lfv;
    } else if (normalizedDietType === "vegetarian") {
      dietaryGuidelines = `
      IMPORTANT DIETARY GUIDELINES FOR VEGETARIAN:
      - Follow Low Fat Vegan (LFV) principles for diabetes management
      - ALL vegetables are allowed including root vegetables, sweet potatoes, yams
      - ALL fruits are allowed but limit avocados (max 1/4 medium), coconuts (max 1/8 medium), olives (1-2 pieces) to once daily
      - ALL whole grains and millets are allowed (amaranth, barnyard millet, buckwheat, finger millet, foxtail millet, etc.)
      - Dal & pulses in MODERATION: limit to once daily, preferably sprouted (moong, masoor, toor, urad, lobia, rajma, matar, chana)
      - Nuts & seeds in MODERATION: one palm-sized serving daily, 2 tbsp for chia/hemp seeds

      COMPLETELY RESTRICTED ITEMS - NEVER INCLUDE:
      - All dairy products (ghee, butter, paneer, cheese, curd, yogurt, ice cream)
      - All seafood (fish, prawns, shellfish)
      - All meat and processed meat products
      - All eggs
      - All cooking oils (olive, coconut, soybean, corn, safflower, sunflower, rapeseed, peanut, cottonseed, canola, mustard oil)
      - All added sugars, jaggery, glucose, fructose, high fructose corn syrup, cane sugar, aspartame, corn syrup, maltose, dextrose, sorbitol, mannitol, xylitol, maltodextrin, molasses, brown rice syrup, splenda, nutrasweet, stevia, barley malt
      - Oats and oat-based products
      USER-SPECIFIC DIETARY RESTRICTIONS:
      ${
        dynamicRecommendations.allergenExclusions.length > 0
          ? dynamicRecommendations.allergenExclusions
              .map((exclusion) => `- ${exclusion}`)
              .join("\n")
          : "- No specific allergen restrictions for this user"
      }
      
      PORTION ADJUSTMENTS FOR THIS USER:
      ${dynamicRecommendations.portionAdjustments}`;
      relevantPdf = uploadedPdfFiles.lfv;
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
      - Oats and oat-based products
      
      USER-SPECIFIC DIETARY RESTRICTIONS:
      ${
        dynamicRecommendations.allergenExclusions.length > 0
          ? dynamicRecommendations.allergenExclusions
              .map((exclusion) => `- ${exclusion}`)
              .join("\n")
          : "- No specific allergen restrictions for this user"
      }
      
      PORTION ADJUSTMENTS FOR THIS USER:
      ${dynamicRecommendations.portionAdjustments}`;
      relevantPdf = uploadedPdfFiles.lchf;
    }

    // Build the prompt for Gemini with RAG
    const basePrompt = `${daySpecificInfo}Generate a personalized premium diabetes-friendly daily meal plan for a ${age}-year-old person with ${diabetesType} diabetes with BMI ${bmi.toFixed(1)}. 
    Their diet preference is ${dietType} and cuisine preference is ${cuisine}.
    
    🚨 CRITICAL ALLERGY SAFETY FOR THIS USER 🚨
    ${
      allergies.length > 0
        ? `THIS USER HAS LIFE-THREATENING ALLERGIES - ZERO TOLERANCE:\n` +
          allergies
            .map(
              (allergy) => `🚨 FORBIDDEN: ${allergy.allergyType.toUpperCase()}`,
            )
            .join("\n") +
          `\n\nDETAILED SAFETY REQUIREMENTS:\n` +
          dynamicRecommendations.allergenExclusions
            .map((exclusion) => `🚨 ${exclusion}`)
            .join("\n") +
          `\n\n⚠️ SAFETY VERIFICATION: Before suggesting ANY ingredient, meal, or cooking method, verify it does NOT contain the forbidden allergens above.`
        : "✅ No known food allergies - all ingredients may be considered"
    }
    
    ${bloodSugarContext}
    ${medicationContext}
    ${avoidanceContext}

    ${dietaryGuidelines}

    ADDITIONAL DIETARY RESTRICTIONS:
    - Avoid processed food completely
    - Allow less processed cheese like goat cheese and grass-fed cheese only
    - Use less oil, dry roast spices and cook
    - Prefer raw fruits and raw salad (with minimal dressings) as pre-meal to the diet plan
    - Main course recipes should be mostly grilled, avoid adding any Indian gravy to the meal plan
    - Ensure ingredients do not include high carbs or oily recipes for the diet

    STRICTLY FOLLOW THE GUIDELINES AND MEAL PLANS FROM THE PROVIDED PDF DOCUMENTS. Use the PDF content as your primary reference for creating diabetes-friendly meal plans that align with the specified diet type.

    Create a COMPLETE day's meal plan with breakfast, lunch, dinner, and snacks. 

    For each meal, include:
    1. A pre-meal appetizer or small dish
    2. A main meal with preparation instructions 
    3. Exact portion sizes and serving recommendations
    4. Detailed nutritional information including calories, carbs, protein, fat
    5. Ingredients list
    6. Preparation time and difficulty level
    7. Any specific diabetes management tips for that meal

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

    Ensure meals are:
    - Nutrient-dense with complex carbohydrates
    - Rich in fiber to slow sugar absorption
    - Contain quality protein and healthy fats
    - Include anti-inflammatory foods
    - Appropriate portion sizes to maintain stable blood sugar
    - STRICTLY follow the dietary guidelines and restrictions mentioned above
    - Ensure that the same main ingredient is NOT used more than once across all meals in a single day (breakfast, lunch, dinner, and snacks should have different primary ingredients)

    Format the response as a JSON object with the following structure:
    {
      "breakfast": {
        "preMealName": "Name of pre-meal",
        "preMealDescription": "Description",
        "preMealTime": "Recommended time",
        "preMealCalories": number,
        "mainMealName": "Name of main meal",
        "mainMealDescription": "Description",
        "mainMealPortionSize": "Portion size",
        "mainMealTime": "Recommended time",
        "mainMealCalories": number,
        "totalCalories": number,
        "preparation": "Detailed preparation instructions",
        "preparationTime": number (in minutes),
        "difficultyLevel": "easy|medium|hard",
        "glycemicImpact": "Low|Medium|High",
        "diabetesManagementTips": "Tips specific to this meal",
        "nutrients": { "carbs": number, "protein": number, "fat": number, "fiber": number },
        "ingredients": ["ingredient1", "ingredient2"],
        "tags": ["tag1", "tag2"]
      },
      "lunch": { same structure as breakfast },
      "dinner": { same structure as breakfast },
      "snacks": { same structure as breakfast }
    }`;

    // Prepare content array with prompt and relevant PDFs
    let content = [basePrompt];

    if (Array.isArray(relevantPdf)) {
      // Add both PDFs for all-inclusive diet
      content.push(relevantPdf[0], relevantPdf[1]);
    } else if (relevantPdf) {
      // Add specific PDF for vegan or vegetarian diet
      content.push(relevantPdf);
    }

    // Call the Gemini API with RAG
    const result = await model.generateContent(content);
    const response = await result.response;
    const text = response.text();

    // Extract the JSON object from the response
    let mealPlan;
    try {
      // Find JSON in the response text - it should be between curly braces
      const jsonMatch = text.match(/(\{[\s\S]*\})/);
      if (jsonMatch && jsonMatch[0]) {
        mealPlan = JSON.parse(jsonMatch[0]);
      } else {
        throw new Error("No valid JSON found in response");
      }
    } catch (parseError) {
      console.error("Error parsing Gemini RAG response as JSON:", parseError);
      console.log("Gemini RAG response:", text);

      // Return a fallback error meal plan with user data for proper generation
      return await getRAGFallbackMealPlan(
        userId,
        profile,
        medicalCondition,
        allergies,
        cuisineList,
      );
    }

    return mealPlan;
  } catch (error: any) {
    console.error("Error generating RAG meal plan with Gemini:", error);

    // Try fallback meal generation before throwing error
    try {
      console.log("Attempting fallback meal generation due to RAG error");
      return await getRAGFallbackMealPlan(
        userId,
        profile,
        medicalCondition,
        allergies,
        cuisineList,
      );
    } catch (fallbackError) {
      console.error("Fallback meal generation also failed:", fallbackError);
      throw new Error(
        `Failed to generate RAG meal plan with Gemini: ${error?.message || "Unknown error"}`,
      );
    }
  }
}

// Fallback meal plan for RAG errors
function getRAGFallbackMealPlan() {
  return {
    breakfast: {
      preMealName: "Unable to generate RAG meal plan",
      mainMealName: "Error in RAG meal plan generation",
      totalCalories: 0,
      preparation: "Please try again later - PDF upload or processing failed",
    },
    lunch: {
      preMealName: "Unable to generate RAG meal plan",
      mainMealName: "Error in RAG meal plan generation",
      totalCalories: 0,
      preparation: "Please try again later - PDF upload or processing failed",
    },
    dinner: {
      preMealName: "Unable to generate RAG meal plan",
      mainMealName: "Error in RAG meal plan generation",
      totalCalories: 0,
      preparation: "Please try again later - PDF upload or processing failed",
    },
    snacks: {
      preMealName: "Unable to generate RAG meal plan",
      mainMealName: "Error in RAG meal plan generation",
      totalCalories: 0,
      preparation: "Please try again later - PDF upload or processing failed",
    },
  };
}
