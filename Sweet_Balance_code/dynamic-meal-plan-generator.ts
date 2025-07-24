import type {
  UserProfile,
  UserAllergy,
  UserMedicalCondition,
  UserCuisinePreference,
} from "@shared/schema";
import { db } from "../db";
import { userAllergies } from "@shared/schema";
import { eq } from "drizzle-orm";

// BMI Categories for scientific recommendations
function getBMICategory(bmi: number): string {
  if (bmi < 18.5) return "Underweight";
  if (bmi < 25) return "Normal weight";
  if (bmi < 30) return "Overweight";
  return "Obese";
}

// Calculate daily calorie needs based on user profile
function calculateDailyCalories(profile: UserProfile, bmi: number): number {
  const age = profile.age;
  const gender = profile.gender?.toLowerCase();
  const activityLevel = profile.exerciseLevel?.toLowerCase();
  const weightKg = profile.weightValue;

  // Harris-Benedict Equation
  let bmr;
  if (gender === "male") {
    bmr =
      88.362 + 13.397 * weightKg + 4.799 * profile.heightValue - 5.677 * age;
  } else {
    bmr = 447.593 + 9.247 * weightKg + 3.098 * profile.heightValue - 4.33 * age;
  }

  // Activity multiplier
  let activityMultiplier = 1.2; // sedentary
  switch (activityLevel) {
    case "light":
      activityMultiplier = 1.375;
      break;
    case "moderate":
      activityMultiplier = 1.55;
      break;
    case "high":
      activityMultiplier = 1.725;
      break;
    case "very high":
      activityMultiplier = 1.9;
      break;
  }

  let dailyCalories = bmr * activityMultiplier;

  // Adjust for weight management based on BMI
  if (bmi > 25) {
    dailyCalories -= 500; // Create deficit for weight loss
  } else if (bmi < 18.5) {
    dailyCalories += 300; // Add surplus for weight gain
  }

  return Math.round(dailyCalories);
}

// Generate dynamic dietary recommendations based on user profile
function generateDynamicDietaryRecommendations(
  profile: UserProfile | undefined,
  medicalCondition: UserMedicalCondition | undefined,
  allergies: UserAllergy[],
  bmi: number,
  age: number,
  diabetesType: string,
) {
  const recommendations = {
    allergenExclusions: [] as string[],
    medicalConditionGuidelines: "",
    bmiRecommendations: "",
    ageGenderRecommendations: "",
    diabetesRecommendations: "",
    macronutrientTargets: "",
    specificRestrictions: "",
    calorieTarget: 0,
    portionAdjustments: "",
  };

  // Calculate calorie target
  if (profile) {
    recommendations.calorieTarget = calculateDailyCalories(profile, bmi);
  }

  // Dynamic allergy exclusions - completely avoid based on user's actual allergies
  if (allergies.length > 0) {
    recommendations.allergenExclusions = allergies.map((allergy) => {
      const allergenType = allergy.allergyType.toLowerCase();
      switch (allergenType) {
        case "eggs":
          return "CRITICAL SAFETY - NEVER INCLUDE: All eggs, egg-based dishes (omelettes, frittatas, quiche), mayonnaise, custards, meringues, some baked goods, pasta containing eggs, eggnog, caesar dressing, hollandaise sauce, and any foods prepared with eggs. Use egg substitutes like flax eggs, chia seeds, or commercial egg replacers when needed.";
        case "dairy":
        case "milk":
          return "CRITICAL SAFETY - NEVER INCLUDE: All milk, cheese, yogurt, butter, cream, ice cream, whey, casein, lactose, and any foods containing milk proteins. Use plant-based alternatives like almond milk, coconut milk, or oat milk.";
        case "nuts":
        case "tree nuts":
          return "CRITICAL SAFETY - NEVER INCLUDE: Almonds, walnuts, cashews, pistachios, hazelnuts, pecans, brazil nuts, macadamia nuts, pine nuts, and all nut-based products including nut butters, nut oils, nut flours, and foods processed in facilities with nuts.";
        case "peanuts":
          return "CRITICAL SAFETY - NEVER INCLUDE: Peanuts, peanut butter, peanut oil, groundnuts, and foods processed in facilities with peanuts. Check all Asian cuisine ingredients carefully.";
        case "soy":
        case "soya":
          return "CRITICAL SAFETY - NEVER INCLUDE: Soybeans, soy sauce, tofu, tempeh, edamame, soy milk, miso, soy lecithin, and foods containing soy derivatives.";
        case "shellfish":
          return "CRITICAL SAFETY - NEVER INCLUDE: Shrimp, crab, lobster, oysters, mussels, clams, scallops, and all crustaceans and mollusks.";
        case "fish":
          return "CRITICAL SAFETY - NEVER INCLUDE: All fish varieties, fish sauce, fish oil supplements, worcestershire sauce (contains fish), and foods cooked with fish.";
        case "gluten":
        case "wheat":
          return "CRITICAL SAFETY - NEVER INCLUDE: Wheat, barley, rye, oats (unless certified gluten-free), bread, pasta, flour-based products, and wheat-based seasonings.";
        default:
          return `CRITICAL SAFETY - NEVER INCLUDE: ${allergy.allergyType} and all foods containing or prepared with ${allergy.allergyType}. Ensure cross-contamination prevention.`;
      }
    });
  }

  // Parse medical conditions
  let medicalConditions: string[] = [];
  try {
    if (medicalCondition?.medicalConditions) {
      const parsedConditions = JSON.parse(medicalCondition.medicalConditions);
      if (Array.isArray(parsedConditions)) {
        medicalConditions = parsedConditions
          .map((c) => c.name?.toLowerCase())
          .filter(Boolean);
      }
    }
  } catch (e) {
    // Handle parsing errors gracefully
  }

  // Medical condition-specific guidelines
  if (medicalConditions.length > 0) {
    const conditionGuidelines = medicalConditions
      .map((condition) => {
        switch (condition) {
          case "hypertension":
          case "high blood pressure":
            return "Hypertension management: Limit sodium to <2300mg/day, emphasize potassium-rich foods (spinach, avocado), avoid processed meats and canned foods";
          case "kidney disease":
          case "chronic kidney disease":
            return "Kidney disease management: Limit protein to 0.8g/kg body weight, restrict high-phosphorus foods (dairy, nuts), control potassium intake";
          case "heart disease":
          case "cardiovascular disease":
            return "Heart disease management: Emphasize omega-3 fatty acids (salmon, sardines), limit saturated fats, increase soluble fiber";
          case "fatty liver":
          case "nafld":
            return "Fatty liver management: Avoid refined sugars and simple carbohydrates, emphasize vegetables and lean proteins, limit fructose";
          case "thyroid":
          case "hypothyroidism":
            return "Thyroid management: Include iodine-rich foods (seaweed, fish), limit goitrogenic foods (raw cruciferous vegetables), maintain consistent meal timing";
          case "celiac":
          case "celiac disease":
            return "Celiac disease management: Strict gluten-free diet, avoid wheat, barley, rye, and cross-contamination";
          default:
            return `${condition} management: Select foods that support ${condition} treatment and avoid those that may worsen symptoms`;
        }
      })
      .join(". ");
    recommendations.medicalConditionGuidelines = conditionGuidelines;
  }

  // BMI-specific recommendations
  const bmiCategory = getBMICategory(bmi);
  const gender = profile?.gender?.toLowerCase() || "";
  const weightKg = profile?.weightValue || 70;

  switch (bmiCategory) {
    case "Underweight":
      recommendations.bmiRecommendations = `Underweight management (BMI ${bmi.toFixed(1)}): Increase caloric density with healthy fats, frequent nutrient-dense meals, add protein shakes if needed`;
      recommendations.macronutrientTargets = `Protein: ${(weightKg * 1.2).toFixed(0)}g, Carbs: 45-50% calories, Fat: 30-35% calories`;
      break;
    case "Normal weight":
      recommendations.bmiRecommendations = `Normal weight maintenance (BMI ${bmi.toFixed(1)}): Maintain current weight with balanced nutrition, focus on diabetes management`;
      recommendations.macronutrientTargets = `Protein: ${(weightKg * 1.0).toFixed(0)}g, Carbs: 40-45% calories, Fat: 25-30% calories`;
      break;
    case "Overweight":
      recommendations.bmiRecommendations = `Overweight management (BMI ${bmi.toFixed(1)}): Create moderate calorie deficit, emphasize protein satiety, reduce refined carbohydrates`;
      recommendations.macronutrientTargets = `Protein: ${(weightKg * 1.2).toFixed(0)}g, Carbs: 30-35% calories, Fat: 25-30% calories`;
      break;
    case "Obese":
      recommendations.bmiRecommendations = `Obesity management (BMI ${bmi.toFixed(1)}): Significant calorie restriction, high protein to preserve muscle, very low carbohydrate approach`;
      recommendations.macronutrientTargets = `Protein: ${(weightKg * 1.6).toFixed(0)}g, Carbs: 20-25% calories, Fat: 30-35% calories`;
      break;
  }

  // Age and gender-specific recommendations with portion adjustments
  if (age >= 65) {
    recommendations.ageGenderRecommendations = `Senior nutrition (${age} years): Higher protein needs (1.2g/kg), emphasize calcium and vitamin D, ensure adequate B12 and folate. Smaller, more frequent meals may be beneficial.`;
    recommendations.portionAdjustments += `Senior portions: Reduce overall portion sizes by 10-15%, increase meal frequency to 5-6 smaller meals. `;
  } else if (age >= 50) {
    recommendations.ageGenderRecommendations = `Middle-age nutrition (${age} years): Focus on bone health, heart-protective nutrients, maintain muscle mass`;
    recommendations.portionAdjustments += `Middle-age portions: Standard portions with emphasis on protein quality. `;
  } else if (age >= 18) {
    recommendations.ageGenderRecommendations = `Adult nutrition (${age} years): Standard nutritional requirements with diabetes optimization`;
    recommendations.portionAdjustments += `Adult portions: Standard serving sizes based on BMI and activity level. `;
  } else {
    recommendations.ageGenderRecommendations = `Young adult nutrition (${age} years): Higher energy needs, focus on establishing healthy habits`;
    recommendations.portionAdjustments += `Young adult portions: Slightly larger portions to support growth and development. `;
  }

  if (gender === "female") {
    recommendations.ageGenderRecommendations +=
      ". Female considerations: Iron-rich foods, hormonal blood sugar impacts, possible increased calcium needs";
    recommendations.portionAdjustments += `Female adjustments: Slightly smaller portions, focus on iron-rich foods. `;
  } else if (gender === "male") {
    recommendations.ageGenderRecommendations +=
      ". Male considerations: Higher protein for muscle maintenance, cardiovascular health monitoring";
    recommendations.portionAdjustments += `Male adjustments: Larger protein portions, increased overall serving sizes. `;
  }

  // BMI-based portion adjustments
  if (bmi < 18.5) {
    recommendations.portionAdjustments += `Underweight BMI (${bmi.toFixed(1)}): Increase all portion sizes by 20-25%, add healthy calorie-dense foods. `;
  } else if (bmi >= 25 && bmi < 30) {
    recommendations.portionAdjustments += `Overweight BMI (${bmi.toFixed(1)}): Reduce portion sizes by 15-20%, increase vegetable portions. `;
  } else if (bmi >= 30) {
    recommendations.portionAdjustments += `Obese BMI (${bmi.toFixed(1)}): Significantly reduce portion sizes by 25-30%, focus on high-volume, low-calorie foods. `;
  } else {
    recommendations.portionAdjustments += `Normal BMI (${bmi.toFixed(1)}): Standard portion sizes appropriate for diabetes management. `;
  }

  // Diabetes type-specific recommendations
  switch (diabetesType?.toLowerCase()) {
    case "type1":
    case "type 1":
      recommendations.diabetesRecommendations =
        "Type 1 diabetes: Carbohydrate counting essential, consistent meal timing, balance with insulin doses, prevent hypoglycemia with appropriate snacks";
      break;
    case "type2":
    case "type 2":
      recommendations.diabetesRecommendations =
        "Type 2 diabetes: Focus on insulin sensitivity, weight management, low glycemic index foods, portion control, meal timing";
      break;
    case "gestational":
      recommendations.diabetesRecommendations =
        "Gestational diabetes: Controlled carbohydrate distribution, frequent small meals, avoid ketosis, adequate nutrition for fetal development";
      break;
    default:
      recommendations.diabetesRecommendations =
        "Diabetes management: Stable blood glucose, low glycemic foods, balanced macronutrients, consistent meal timing";
  }

  // Specific dietary restrictions based on diet type
  const dietType = profile?.dietType?.toLowerCase() || "";
  if (dietType === "vegan") {
    recommendations.specificRestrictions =
      "Vegan requirements: No animal products, focus on plant proteins, B12 consideration, iron absorption optimization, complete amino acids. Follow Low Fat Vegan (LFV) principles.";
  } else if (dietType === "vegetarian") {
    recommendations.specificRestrictions =
      "Vegetarian requirements: No meat, poultry, fish, eggs, or dairy products. Focus on plant proteins, B12 consideration, iron absorption optimization. Follow Low Fat Vegan (LFV) principles.";
  } else if (dietType === "meat-based" || dietType === "all-inclusive") {
    recommendations.specificRestrictions =
      "Meat-based diet: Include variety of protein sources including meat, poultry, fish, and eggs. Follow Low Carb High Fat (LCHF) principles for diabetes management.";
  } else {
    recommendations.specificRestrictions =
      "Omnivorous diet: Include variety of protein sources based on preferences and health needs";
  }

  return recommendations;
}

// HARD CONSTRAINT: Deterministic ingredient exclusion rules
const DIET_TYPE_CONSTRAINTS = {
  vegan: {
    forbidden: [
      // Meat & Poultry
      "chicken", "mutton", "lamb", "beef", "pork", "bacon", "ham", "turkey", "duck", "goat", "venison", "meat", "poultry",
      // Seafood  
      "fish", "salmon", "tuna", "prawn", "shrimp", "crab", "lobster", "shellfish", "cod", "mackerel", "seafood",
      // Eggs
      "egg", "eggs", "egg white", "egg yolk", "omelette", "scrambled egg", "boiled egg", "fried egg",
      // Dairy
      "milk", "cheese", "paneer", "yogurt", "curd", "ghee", "butter", "cream", "cottage cheese", "dairy",
      // Other animal products
      "honey", "gelatin", "lard", "tallow"
    ],
    principles: "LFV (Low Fat Vegan)"
  },
  vegetarian: {
    forbidden: [
      // Meat & Poultry
      "chicken", "mutton", "lamb", "beef", "pork", "bacon", "ham", "turkey", "duck", "goat", "venison", "meat", "poultry",
      // Seafood  
      "fish", "salmon", "tuna", "prawn", "shrimp", "crab", "lobster", "shellfish", "cod", "mackerel", "seafood",
      // Eggs
      "egg", "eggs", "egg white", "egg yolk", "omelette", "scrambled egg", "boiled egg", "fried egg",
      // Dairy (following LFV principles like vegan)
      "milk", "cheese", "paneer", "yogurt", "curd", "ghee", "butter", "cream", "cottage cheese", "dairy",
      // Other animal products
      "gelatin", "lard", "tallow"
    ],
    principles: "LFV (Low Fat Vegan)"
  },
  "meat-based": {
    forbidden: [],
    principles: "LCHF (Low Carb High Fat)"
  }
};

// DETERMINISTIC VALIDATION: Hard constraint checker
function enforceHardDietConstraints(mealPlan: any, profile: UserProfile): { isValid: boolean; violations: string[]; correctedPlan?: any } {
  const violations: string[] = [];
  const dietType = profile.dietType?.toLowerCase();
  
  if (!dietType || !DIET_TYPE_CONSTRAINTS[dietType as keyof typeof DIET_TYPE_CONSTRAINTS]) {
    return { isValid: true, violations: [] };
  }

  const constraints = DIET_TYPE_CONSTRAINTS[dietType as keyof typeof DIET_TYPE_CONSTRAINTS];
  const forbiddenItems = constraints.forbidden;

  if (forbiddenItems.length === 0) {
    return { isValid: true, violations: [] };
  }

  const mealsToCheck = ["breakfast", "lunch", "dinner", "snacks"];
  let hasViolations = false;

  for (const mealType of mealsToCheck) {
    if (!mealPlan[mealType]) continue;

    const meal = mealPlan[mealType];
    const mealTexts = [
      meal.preMealName || "",
      meal.mainMealName || "",
      meal.mainMealPortionSize || "",
      meal.preparation || "",
    ];

    const fullMealText = mealTexts.join(" ").toLowerCase();

    // HARD CONSTRAINT CHECK: Zero tolerance for forbidden ingredients
    for (const forbiddenItem of forbiddenItems) {
      if (fullMealText.includes(forbiddenItem.toLowerCase())) {
        violations.push(
          `HARD CONSTRAINT VIOLATION: ${mealType} contains "${forbiddenItem}" (forbidden for ${profile.dietType} diet)`
        );
        hasViolations = true;
        break;
      }
    }
  }

  // If violations found, return deterministic safe meal plan
  if (hasViolations) {
    console.error("🚨 HARD CONSTRAINT VIOLATIONS DETECTED:", violations);
    return {
      isValid: false,
      violations,
      correctedPlan: getDeterministicSafeMealPlan(dietType)
    };
  }

  return { isValid: true, violations: [] };
}

// DETERMINISTIC SAFE MEAL PLAN: Rule-based generation without AI
function getDeterministicSafeMealPlan(dietType: string) {
  if (dietType === "vegan" || dietType === "vegetarian") {
    return {
      breakfast: {
        preMealName: "Cucumber slices with lemon",
        preMealTime: "7:00 AM",
        preMealCalories: 20,
        mainMealName: "Steamed vegetables with quinoa",
        mainMealPortionSize: "1 cup mixed steamed vegetables with 0.5 cup quinoa",
        mainMealTime: "7:30 AM",
        mainMealCalories: 200,
        totalCalories: 220,
        preparation: "Steam vegetables, cook quinoa in water",
        diabetesManagementTips: "High fiber, plant-based meal for stable blood sugar"
      },
      lunch: {
        preMealName: "Raw vegetables",
        preMealTime: "12:30 PM", 
        preMealCalories: 25,
        mainMealName: "Lentil soup with vegetables",
        mainMealPortionSize: "1 cup cooked lentils with mixed vegetables",
        mainMealTime: "1:00 PM",
        mainMealCalories: 250,
        totalCalories: 275,
        preparation: "Cook lentils in water, add vegetables",
        diabetesManagementTips: "Plant protein and fiber for glucose control"
      },
      dinner: {
        preMealName: "Green salad",
        preMealTime: "7:00 PM",
        preMealCalories: 30,
        mainMealName: "Brown rice with steamed vegetables", 
        mainMealPortionSize: "0.5 cup brown rice with 1 cup steamed vegetables",
        mainMealTime: "7:30 PM",
        mainMealCalories: 200,
        totalCalories: 230,
        preparation: "Steam brown rice and vegetables separately",
        diabetesManagementTips: "Complex carbohydrates with fiber"
      },
      snacks: {
        preMealName: "Raw cucumber",
        preMealTime: "4:00 PM",
        preMealCalories: 15,
        mainMealName: "Mixed nuts and seeds",
        mainMealPortionSize: "Small handful of mixed nuts and seeds",
        mainMealTime: "4:30 PM", 
        mainMealCalories: 100,
        totalCalories: 115,
        preparation: "No preparation needed",
        diabetesManagementTips: "Healthy fats for sustained energy"
      }
    };
  }
  
  // Default for other diet types
  return getFallbackMealPlan(dietType);
}

// Legacy validation function for compatibility
function validateDietTypeCompliance(mealPlan: any, profile: UserProfile) {
  const hardConstraintResult = enforceHardDietConstraints(mealPlan, profile);
  return {
    isCompliant: hardConstraintResult.isValid,
    violations: hardConstraintResult.violations
  };
}

// Critical safety function to validate meal plan against user allergies
async function validateMealPlanForAllergens(mealPlan: any, userId: number) {
  try {
    // Get user allergies from database
    const allergies = await db
      .select()
      .from(userAllergies)
      .where(eq(userAllergies.userId, userId));

    if (allergies.length === 0) {
      return { isSafe: true, violations: [] };
    }

    const violations: string[] = [];
    const allergenKeywords = new Map();

    // Create comprehensive allergen detection patterns
    allergies.forEach((allergy) => {
      const allergenType = allergy.allergyType.toLowerCase();
      const keywords: string[] = [];

      switch (allergenType) {
        case "eggs":
          keywords.push(
            "egg",
            "eggs",
            "omelet",
            "omelette",
            "custard",
            "mayonnaise",
            "meringue",
            "frittata",
            "quiche",
          );
          break;
        case "nuts":
        case "tree nuts":
          keywords.push(
            "nuts",
            "nut",
            "almond",
            "almonds",
            "walnut",
            "walnuts",
            "cashew",
            "cashews",
            "pistachio",
            "pistachios",
            "hazelnut",
            "hazelnuts",
            "pecan",
            "pecans",
            "brazil nut",
            "macadamia",
            "pine nut",
          );
          break;
        case "peanuts":
          keywords.push("peanut", "peanuts", "peanut butter");
          break;
        case "dairy":
        case "milk":
          keywords.push(
            "milk",
            "cheese",
            "yogurt",
            "yoghurt",
            "butter",
            "cream",
            "ice cream",
            "whey",
            "casein",
            "lactose",
          );
          break;
        case "soy":
        case "soya":
          keywords.push(
            "soy",
            "soya",
            "tofu",
            "tempeh",
            "edamame",
            "soy sauce",
            "miso",
          );
          break;
        case "fish":
          keywords.push(
            "fish",
            "salmon",
            "tuna",
            "cod",
            "sardine",
            "mackerel",
            "trout",
            "bass",
            "halibut",
          );
          break;
        case "shellfish":
          keywords.push(
            "shellfish",
            "shrimp",
            "crab",
            "lobster",
            "oyster",
            "mussel",
            "clam",
            "scallop",
          );
          break;
        case "gluten":
        case "wheat":
          keywords.push(
            "wheat",
            "bread",
            "pasta",
            "flour",
            "gluten",
            "barley",
            "rye",
            "oats",
          );
          break;
        default:
          keywords.push(allergenType);
      }

      allergenKeywords.set(allergenType, keywords);
    });

    // Check each meal for allergen presence
    const mealsToCheck = ["breakfast", "lunch", "dinner", "snacks"];

    for (const mealType of mealsToCheck) {
      if (!mealPlan[mealType]) continue;

      const meal = mealPlan[mealType];
      const mealTexts = [
        meal.preMealName || "",
        meal.mainMealName || "",
        meal.mainMealPortionSize || "",
        meal.preparation || "",
      ];

      const fullMealText = mealTexts.join(" ").toLowerCase();

      // Check against each allergen
      allergies.forEach((allergy) => {
        const allergenType = allergy.allergyType.toLowerCase();
        const keywords = allergenKeywords.get(allergenType) || [allergenType];

        for (const keyword of keywords) {
          if (fullMealText.includes(keyword)) {
            violations.push(
              `${mealType}: Contains "${keyword}" (${allergy.allergyType} allergy)`,
            );
            break; // Don't add multiple violations for the same allergen in the same meal
          }
        }
      });
    }

    return {
      isSafe: violations.length === 0,
      violations,
    };
  } catch (error) {
    console.error("Error validating meal plan for allergens:", error);
    // Return unsafe if we can't validate
    return {
      isSafe: false,
      violations: ["Unable to validate allergen safety"],
    };
  }
}

// Generate dynamic meal plan prompt based on user profile
export function generateDynamicMealPlanPrompt(
  profile: UserProfile,
  medicalCondition: UserMedicalCondition | undefined,
  allergies: UserAllergy[],
  cuisinePreferences: UserCuisinePreference[],
  diabetesType: string,
): string {
  const bmi = profile.weightValue / Math.pow(profile.heightValue / 100, 2);
  const recommendations = generateDynamicDietaryRecommendations(
    profile,
    medicalCondition,
    allergies,
    bmi,
    profile.age,
    diabetesType,
  );

  const cuisineList =
    cuisinePreferences.length > 0
      ? cuisinePreferences.map((c) => c.cuisine).join(", ")
      : "Continental";

  return `Create a scientifically-based, personalized diabetes-friendly meal plan for this specific user:

USER PROFILE ANALYSIS:
- Age: ${profile.age} years (${profile.gender || "Not specified"})
- BMI: ${bmi.toFixed(1)} (${getBMICategory(bmi)})
- Height: ${profile.heightValue}${profile.heightUnit}, Weight: ${profile.weightValue}${profile.weightUnit}
- Diabetes: ${diabetesType}
- Diet Type: ${profile.dietType || "Not specified"}
- Exercise Level: ${profile.exerciseLevel || "Not specified"}
- Cuisine Preference: ${cuisineList}
- Daily Calorie Target: ${recommendations.calorieTarget} calories

🚨 CRITICAL DIETARY RESTRICTIONS - ABSOLUTE COMPLIANCE REQUIRED 🚨
${
  profile.dietType?.toLowerCase() === "vegan" || profile.dietType?.toLowerCase() === "vegetarian"
    ? `🚨 VEGAN/VEGETARIAN DIET - ZERO ANIMAL PRODUCTS ALLOWED:
🚨 FORBIDDEN: ALL meat (chicken, mutton, lamb, beef, pork, bacon, ham)
🚨 FORBIDDEN: ALL seafood (fish, prawns, crab, shellfish)
🚨 FORBIDDEN: ALL eggs (whole eggs, egg whites, egg yolks) 
🚨 FORBIDDEN: ALL dairy (milk, cheese, paneer, yogurt, curd, ghee, butter, cream)
🚨 FORBIDDEN: ALL cooking oils (olive oil, coconut oil, sunflower oil, etc.)
🚨 ONLY ALLOWED: Vegetables, fruits, lentils/dals, grains, nuts, seeds (following LFV principles)

⚠️ MANDATORY DIET CHECK: Before suggesting ANY ingredient, verify it contains NO animal products whatsoever.`
    : profile.dietType?.toLowerCase() === "meat-based" 
    ? "🥩 MEAT-BASED DIET: Include meat, poultry, seafood, eggs following LCHF principles"
    : "🍽️ BALANCED OMNIVOROUS DIET: Include variety of protein sources"
}

🚨 CRITICAL ALLERGY SAFETY - ZERO TOLERANCE POLICY 🚨
${
  allergies.length > 0
    ? `THIS USER HAS LIFE-THREATENING ALLERGIES - ABSOLUTE PROHIBITION:\n` +
      allergies
        .map(
          (allergy) =>
            `🚨 FORBIDDEN ALLERGEN: ${allergy.allergyType.toUpperCase()}`,
        )
        .join("\n") +
      `\n\nDETAILED SAFETY EXCLUSIONS:\n` +
      recommendations.allergenExclusions
        .map((exclusion) => `🚨 ${exclusion}`)
        .join("\n") +
      `\n\n⚠️ MANDATORY SAFETY CHECK: Before suggesting ANY ingredient or meal, verify it does NOT contain or use any of the above allergens. This includes hidden ingredients, cooking methods, and cross-contamination risks.`
    : "✅ No known food allergies - all foods may be considered"
}

PORTION SIZE ADJUSTMENTS BASED ON USER PROFILE:
${recommendations.portionAdjustments}

TRIPLE-CHECK REQUIREMENT: 
1. Scan every ingredient for allergen presence
2. Verify cooking methods don't introduce allergens  
3. Ensure no cross-contamination risk

MEDICAL CONDITION MANAGEMENT:
${recommendations.medicalConditionGuidelines || "No specific medical conditions requiring dietary modifications"}

BODY COMPOSITION & WEIGHT MANAGEMENT:
${recommendations.bmiRecommendations}

MACRONUTRIENT TARGETS:
${recommendations.macronutrientTargets}

AGE & GENDER CONSIDERATIONS:
${recommendations.ageGenderRecommendations}

DIABETES-SPECIFIC REQUIREMENTS:
${recommendations.diabetesRecommendations}

DIETARY APPROACH:
${recommendations.specificRestrictions}

MEAL PLANNING REQUIREMENTS:
- Create 4 meals: breakfast, lunch, dinner, snacks
- Each meal must have pre-meal component and main meal
- Include exact portions, calories, and macronutrients
- Ensure meals fit within daily calorie target of ${recommendations.calorieTarget}
- Consider glycemic index and blood sugar impact
- Provide preparation time and difficulty level
- Include diabetes management tips for each meal
- Ensure variety in ingredients across all meals
- All recommendations must be evidence-based and appropriate for diabetes management

FORMAT: Return as JSON with breakfast, lunch, dinner, snacks objects containing:
{
  "preMealName": "name",
  "preMealTime": "time",
  "preMealCalories": number,
  "mainMealName": "name", 
  "mainMealPortionSize": "detailed portions",
  "mainMealTime": "time",
  "mainMealCalories": number,
  "totalCalories": number,
  "mainMealNutrients": {"carbs": "Xg", "protein": "Xg", "fat": "Xg", "fiber": "Xg"},
  "carbs": number,
  "protein": number, 
  "fat": number,
  "fiber": number,
  "preparation": "detailed instructions",
  "preparationTime": number,
  "difficultyLevel": "easy|medium|hard",
  "glycemicImpact": "Low|Medium|High",
  "diabetesManagementTips": "specific tips"
}`;
}

// Generate dynamic meal plan using AI
export async function generateDynamicMealPlanWithAI(
  prompt: string,
  userId?: number,
  profile?: UserProfile,
) {
  try {
    const { GoogleGenerativeAI } = await import("@google/generative-ai");

    if (!process.env.GOOGLE_GEMINI_API_KEY) {
      throw new Error("GOOGLE_GEMINI_API_KEY is not configured");
    }

    const genAI = new GoogleGenerativeAI(process.env.GOOGLE_GEMINI_API_KEY);
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

    console.log("Generating dynamic meal plan with AI...");

    const result = await model.generateContent(prompt);
    const response = await result.response;
    const text = response.text();

    console.log("Dynamic AI response received, parsing...");

    // Extract JSON from response
    let jsonText = text;

    // Try to extract JSON from code blocks first
    let jsonMatch = text.match(/```json\s*([\s\S]*?)\s*```/);
    if (jsonMatch) {
      jsonText = jsonMatch[1].trim();
    } else {
      // Try to find JSON object in the text
      jsonMatch = text.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        jsonText = jsonMatch[0].trim();
      }
    }

    if (
      !jsonText ||
      (!jsonText.startsWith("{") && !jsonText.startsWith("```"))
    ) {
      throw new Error("No valid JSON found in AI response");
    }

    // Clean up any remaining markdown
    jsonText = jsonText
      .replace(/```json\s*/, "")
      .replace(/```\s*$/, "")
      .trim();

    const mealPlan = JSON.parse(jsonText);

    // Validate required structure
    const requiredMeals = ["breakfast", "lunch", "dinner", "snacks"];
    for (const meal of requiredMeals) {
      if (!mealPlan[meal]) {
        throw new Error(`Missing ${meal} in meal plan`);
      }

      const mealData = mealPlan[meal];
      if (!mealData.mainMealName || !mealData.preMealName) {
        throw new Error(`Invalid ${meal} structure in meal plan`);
      }
    }

    // CRITICAL SAFETY CHECK: Enforce hard dietary constraints with deterministic rules
    if (profile) {
      const hardConstraintResult = enforceHardDietConstraints(mealPlan, profile);
      if (!hardConstraintResult.isValid) {
        console.error("🚨 HARD CONSTRAINT VIOLATIONS:", hardConstraintResult.violations);
        // Return deterministic safe meal plan that enforces hard constraints
        return hardConstraintResult.correctedPlan || getDeterministicSafeMealPlan(profile.dietType?.toLowerCase() || "");
      }
    }

    // CRITICAL SAFETY CHECK: Validate no allergens are present
    if (userId) {
      try {
        const allergenCheck = await validateMealPlanForAllergens(
          mealPlan,
          userId,
        );
        if (!allergenCheck.isSafe) {
          console.error("ALLERGEN SAFETY VIOLATION:", allergenCheck.violations);
          // Return safe fallback meal plan instead of throwing error
          return getFallbackMealPlan(profile?.dietType);
        }
      } catch (validationError) {
        console.error("Error during allergen validation:", validationError);
        // Return safe fallback if validation fails
        return getFallbackMealPlan(profile?.dietType);
      }
    }

    console.log(
      "Dynamic meal plan generated successfully and validated for allergen safety",
    );
    return mealPlan;
  } catch (error) {
    console.error("Error in dynamic meal plan generation:", error);

    // Return fallback meal plan that respects basic safety
    return getFallbackMealPlan();
  }
}

// Ultra-safe fallback meal plan avoiding all common allergens and respecting diet type
function getFallbackMealPlan(dietType?: string) {
  const isVeganVegetarian = dietType?.toLowerCase() === "vegan" || dietType?.toLowerCase() === "vegetarian";
  
  if (isVeganVegetarian) {
    return {
      breakfast: {
        preMealName: "Fresh cucumber slices with lemon",
        preMealTime: "7:00 AM",
        preMealCalories: 20,
        mainMealName: "Cooked quinoa with steamed spinach and turmeric",
        mainMealPortionSize: "1 cup cooked quinoa with 1 cup steamed spinach",
        mainMealTime: "7:30 AM",
        mainMealCalories: 250,
        totalCalories: 270,
        mainMealNutrients: {
          carbs: "45g",
          protein: "10g",
          fat: "4g",
          fiber: "6g",
        },
        carbs: 45,
        protein: 10,
        fat: 4,
        fiber: 6,
        preparation: "Cook quinoa, steam spinach, season with turmeric and herbs",
        preparationTime: 15,
        difficultyLevel: "easy",
        glycemicImpact: "Medium",
        diabetesManagementTips: "Quinoa provides complete protein and fiber for blood sugar control",
      },
      lunch: {
        preMealName: "Fresh lettuce leaves",
        preMealTime: "12:30 PM",
        preMealCalories: 15,
        mainMealName: "Cooked lentils with roasted vegetables",
        mainMealPortionSize: "1 cup cooked moong dal with mixed roasted vegetables (zucchini, bell peppers, broccoli)",
        mainMealTime: "1:00 PM",
        mainMealCalories: 320,
        totalCalories: 335,
        mainMealNutrients: {
          carbs: "50g",
          protein: "18g",
          fat: "2g",
          fiber: "12g",
        },
        carbs: 50,
        protein: 18,
        fat: 2,
        fiber: 12,
        preparation: "Cook lentils, roast vegetables with minimal water and herbs",
        preparationTime: 25,
        difficultyLevel: "easy",
        glycemicImpact: "Low",
        diabetesManagementTips: "Lentils provide plant protein and fiber for stable glucose",
      },
      dinner: {
        preMealName: "Tomato and cucumber salad",
        preMealTime: "7:00 PM",
        preMealCalories: 25,
        mainMealName: "Steamed brown rice with mixed vegetables",
        mainMealPortionSize: "1 cup steamed brown rice with 1.5 cups mixed steamed vegetables",
        mainMealTime: "7:30 PM",
        mainMealCalories: 300,
        totalCalories: 325,
        mainMealNutrients: {
          carbs: "60g",
          protein: "8g",
          fat: "2g",
          fiber: "8g",
        },
        carbs: 60,
        protein: 8,
        fat: 2,
        fiber: 8,
        preparation: "Steam brown rice, steam vegetables, season with herbs",
        preparationTime: 20,
        difficultyLevel: "easy",
        glycemicImpact: "Medium",
        diabetesManagementTips: "Brown rice with vegetables provides complex carbs and fiber",
      },
      snacks: {
        preMealName: "Celery sticks",
        preMealTime: "4:00 PM",
        preMealCalories: 10,
        mainMealName: "Raw almonds with cucumber",
        mainMealPortionSize: "10 raw almonds with 1/2 cucumber",
        mainMealTime: "4:30 PM",
        mainMealCalories: 120,
        totalCalories: 130,
        mainMealNutrients: {
          carbs: "8g",
          protein: "6g",
          fat: "10g",
          fiber: "4g",
        },
        carbs: 8,
        protein: 6,
        fat: 10,
        fiber: 4,
        preparation: "Slice cucumber, count almonds, arrange on plate",
        preparationTime: 5,
        difficultyLevel: "easy",
        glycemicImpact: "Low",
        diabetesManagementTips: "Healthy fats and protein to maintain blood sugar stability",
      },
    };
  }

  // Default meat-based fallback meal plan
  return {
    breakfast: {
      preMealName: "Fresh cucumber slices with lemon",
      preMealTime: "7:00 AM",
      preMealCalories: 20,
      mainMealName: "Grilled chicken breast with steamed spinach",
      mainMealPortionSize: "4 oz grilled chicken with 1 cup steamed spinach",
      mainMealTime: "7:30 AM",
      mainMealCalories: 250,
      totalCalories: 270,
      mainMealNutrients: {
        carbs: "4g",
        protein: "35g",
        fat: "8g",
        fiber: "3g",
      },
      carbs: 4,
      protein: 35,
      fat: 8,
      fiber: 3,
      preparation: "Grill chicken, steam spinach, season with herbs",
      preparationTime: 15,
      difficultyLevel: "easy",
      glycemicImpact: "Low",
      diabetesManagementTips: "High protein, low carb meal for stable blood sugar",
    },
    lunch: {
      preMealName: "Fresh lettuce leaves",
      preMealTime: "12:30 PM",
      preMealCalories: 15,
      mainMealName: "Baked fish with roasted vegetables",
      mainMealPortionSize: "5 oz fish with mixed roasted vegetables",
      mainMealTime: "1:00 PM",
      mainMealCalories: 320,
      totalCalories: 335,
      mainMealNutrients: {
        carbs: "12g",
        protein: "40g",
        fat: "10g",
        fiber: "5g",
      },
      carbs: 12,
      protein: 40,
      fat: 10,
      fiber: 5,
      preparation: "Bake fish, roast vegetables with herbs",
      preparationTime: 25,
      difficultyLevel: "easy",
      glycemicImpact: "Low",
      diabetesManagementTips: "Lean protein with fiber-rich vegetables",
    },
    dinner: {
      preMealName: "Tomato and cucumber salad",
      preMealTime: "7:00 PM",
      preMealCalories: 25,
      mainMealName: "Grilled lean beef with green beans",
      mainMealPortionSize: "4 oz lean beef with 1.5 cups steamed green beans",
      mainMealTime: "7:30 PM",
      mainMealCalories: 300,
      totalCalories: 325,
      mainMealNutrients: {
        carbs: "8g",
        protein: "35g",
        fat: "12g",
        fiber: "4g",
      },
      carbs: 8,
      protein: 35,
      fat: 12,
      fiber: 4,
      preparation: "Grill beef, steam green beans, season with herbs",
      preparationTime: 20,
      difficultyLevel: "easy",
      glycemicImpact: "Low",
      diabetesManagementTips: "High-quality protein with low-glycemic vegetables",
    },
    snacks: {
      preMealName: "Celery sticks",
      preMealTime: "4:00 PM",
      preMealCalories: 10,
      mainMealName: "Hard-boiled eggs with cucumber",
      mainMealPortionSize: "2 hard-boiled eggs with 1/2 cucumber",
      mainMealTime: "4:30 PM",
      mainMealCalories: 120,
      totalCalories: 130,
      mainMealNutrients: {
        carbs: "4g",
        protein: "12g",
        fat: "8g",
        fiber: "2g",
      },
      carbs: 4,
      protein: 12,
      fat: 8,
      fiber: 2,
      preparation: "Boil eggs, slice cucumber, arrange on plate",
      preparationTime: 5,
      difficultyLevel: "easy",
      glycemicImpact: "Low",
      diabetesManagementTips: "Protein-rich snack to maintain blood sugar stability",
    },
  };
}
