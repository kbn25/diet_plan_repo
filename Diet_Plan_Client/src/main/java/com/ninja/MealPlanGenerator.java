package com.ninja;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

// Placeholder classes for user data (equivalent to TypeScript interfaces)
class UserProfile {
    public double heightValue;
    public double weightValue;
    public String dietType;
    public int age;

    public UserProfile(double heightValue, double weightValue, String dietType, int age) {
        this.heightValue = heightValue;
        this.weightValue = weightValue;
        this.dietType = dietType != null ? dietType : "LCHF";
        this.age = age;
    }
}

class UserMedicalCondition {
    public String diabetesType;

    public UserMedicalCondition(String diabetesType) {
        this.diabetesType = diabetesType != null ? diabetesType : "Type 2";
    }
}

class UserAllergy {
    public String allergyType;

    public UserAllergy(String allergyType) {
        this.allergyType = allergyType;
    }
}

class UserCuisinePreference {
    public String cuisine;

    public UserCuisinePreference(String cuisine) {
        this.cuisine = cuisine;
    }
}

class MealPlan {
    public Meal breakfast;
    public Meal lunch;
    public Meal dinner;
    public Meal snacks;

    public MealPlan(Meal breakfast, Meal lunch, Meal dinner, Meal snacks) {
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
        this.snacks = snacks;
    }
}

class Meal {
    public String preMealName;
    public String preMealTime;
    public int preMealCalories;
    public String mainMealName;
    public String mainMealPortionSize;
    public String mainMealTime;
    public int mainMealCalories;
    public int totalCalories;
    public Nutrients mainMealNutrients;
    public int carbs;
    public int protein;
    public int fat;
    public int fiber;

    public Meal(String preMealName, String preMealTime, int preMealCalories, String mainMealName,
               String mainMealPortionSize, String mainMealTime, int mainMealCalories,
               int totalCalories, Nutrients mainMealNutrients, int carbs, int protein, int fat, int fiber) {
        this.preMealName = preMealName;
        this.preMealTime = preMealTime;
        this.preMealCalories = preMealCalories;
        this.mainMealName = mainMealName;
        this.mainMealPortionSize = mainMealPortionSize;
        this.mainMealTime = mainMealTime;
        this.mainMealCalories = mainMealCalories;
        this.totalCalories = totalCalories;
        this.mainMealNutrients = mainMealNutrients;
        this.carbs = carbs;
        this.protein = protein;
        this.fat = fat;
        this.fiber = fiber;
    }
}

class Nutrients {
    public String carbs;
    public String protein;
    public String fat;
    public String fiber;

    public Nutrients(String carbs, String protein, String fat, String fiber) {
        this.carbs = carbs;
        this.protein = protein;
        this.fat = fat;
        this.fiber = fiber;
    }
}

class RecentMeal {
    public String mainMealName;
    public LocalDate date;

    public RecentMeal(String mainMealName, LocalDate date) {
        this.mainMealName = mainMealName;
        this.date = date;
    }
}

public class MealPlanGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    // Placeholder for Google Generative AI client (Java equivalent not directly available)
    private static final String GEMINI_API_KEY = "";
    // Assume a JDBC connection pool is configured elsewhere
//    private static Connection dbConnection; // Initialize this appropriately

    public static double calculateBMI(double height, double weight) {
        double heightInMeters = height / 100;
        return weight / (heightInMeters * heightInMeters);
    }

	
//	  public static List<RecentMeal> getRecentMealPlans(int userId, String
//	  mealType, int days) { List<RecentMeal> recentMeals = new ArrayList<>(); try {
//	  LocalDate cutoffDate = LocalDate.now().minusDays(days); String query =
//	  "SELECT ai_generated_plan, date FROM free_meal_plans WHERE user_id = ? AND meal_type = ? AND date >= ? ORDER BY date DESC"
//	  ; PreparedStatement stmt = dbConnection.prepareStatement(query);
//	  stmt.setInt(1, userId); stmt.setString(2, mealType); stmt.setString(3,
//	  cutoffDate.toString());
//	  
//	  ResultSet rs = stmt.executeQuery(); while (rs.next()) { try { String planJson
//	  = rs.getString("ai_generated_plan"); ObjectMapper mapper = new
//	  ObjectMapper(); MealPlan planData = mapper.readValue(planJson,
//	  MealPlan.class); if (planData != null && planData.breakfast != null) {
//	  recentMeals.add(new RecentMeal(planData.breakfast.mainMealName,
//	  rs.getDate("date").toLocalDate())); } } catch (Exception parseError) {
//	  System.err.println("Error parsing meal plan for repetition check: " +
//	  parseError.getMessage()); } } } catch (SQLException e) {
//	  System.err.println("Error fetching recent meal plans: " + e.getMessage()); }
//	  return recentMeals; }
//	 
//	
//	  public static CompletableFuture<MealPlan> generatePersonalizedMealPlan( int
//	  userId, UserProfile profile, UserMedicalCondition medicalCondition, //
//	  List<UserAllergy> allergies, List<UserCuisinePreference> cuisinePreferences)
//	  { try { double bmi = calculateBMI(profile.heightValue, profile.weightValue);
//	  String dietType = profile.dietType; String diabetesType =
//	  medicalCondition.diabetesType; List<String> cuisineList =
//	  cuisinePreferences.stream().map(pref ->
//	  pref.cuisine).collect(Collectors.toList()); List<String> allergyList =
//	  allergies.stream().map(allergy ->
//	  allergy.allergyType).collect(Collectors.toList());
//	  
//	  // Placeholder for RAG meal plan generator (assuming it’s not available in
//	  Java) try { // Simulate RAG meal plan generation (replace with actual
//	  implementation if available) throw new
//	  RuntimeException("RAG meal plan generation not implemented in Java"); } catch
//	  (Exception ragError) { System.out.
//	  println("RAG meal plan generation failed, falling back to dynamic generation: "
//	  + ragError.getMessage()); // Fall back to dynamic meal plan generation String
//	  dynamicPrompt = generateDynamicMealPlanPrompt(profile, medicalCondition,
//	  allergies, cuisinePreferences, diabetesType); return
//	  generateMealPlanWithAI(dietType, bmi, diabetesType, allergies, cuisineList,
//	  profile.age, userId); } } catch (Exception e) {
//	  System.err.println("Error generating meal plan: " + e.getMessage()); return
//	  CompletableFuture.completedFuture(getFallbackMealPlan()); } }
	 
    private static String generateDynamicMealPlanPrompt(UserProfile profile, UserMedicalCondition medicalCondition,
                                                       List<UserAllergy> allergies, List<UserCuisinePreference> cuisinePreferences,
                                                       String diabetesType) {
        // Simplified prompt generation (adapt as needed)
        return String.format(
                "Generate a personalized diabetes-friendly meal plan for a %d-year-old person with %s diabetes. Diet: %s. Cuisine: %s. Allergies: %s.",
                profile.age, diabetesType, profile.dietType,
                cuisinePreferences.stream().map(p -> p.cuisine).collect(Collectors.joining(", ")),
                allergies.stream().map(a -> a.allergyType).collect(Collectors.joining(", "))
        );
    }

    public static CompletableFuture<MealPlan> generateMealPlanWithAI(
            String dietType, double bmi, String diabetesType, List<UserAllergy> allergies,
            List<String> cuisineList, int age, Integer userId) {
        try {
            String allergyStrings = String.join(", ", allergies.stream().map(a -> a.allergyType).collect(Collectors.toList()));
            String cuisine = cuisineList.isEmpty() ? "Continental" : String.join(", ", cuisineList);
            StringBuilder avoidanceContext = new StringBuilder();
//            if (userId != null) {
//                List<RecentMeal> recentBreakfasts = getRecentMealPlans(userId, "breakfast", 3);
//                List<RecentMeal> recentLunches = getRecentMealPlans(userId, "lunch", 3);
//                List<RecentMeal> recentDinners = getRecentMealPlans(userId, "dinner", 3);
//                List<RecentMeal> recentSnacks = getRecentMealPlans(userId, "snacks", 3);
//
//                if (!recentBreakfasts.isEmpty() || !recentLunches.isEmpty() || !recentDinners.isEmpty() || !recentSnacks.isEmpty()) {
//                    avoidanceContext.append("\n\nIMPORTANT: AVOID REPEATING THESE RECENT MEALS:\n");
//                    if (!recentBreakfasts.isEmpty()) {
//                        avoidanceContext.append("Recent breakfast meals (avoid these): ")
//                                .append(recentBreakfasts.stream().map(m -> m.mainMealName).collect(Collectors.joining(", ")))
//                                .append("\n");
//                    }
//                    if (!recentLunches.isEmpty()) {
//                        avoidanceContext.append("Recent lunch meals (avoid these): ")
//                                .append(recentLunches.stream().map(m -> m.mainMealName).collect(Collectors.joining(", ")))
//                                .append("\n");
//                    }
//                    if (!recentDinners.isEmpty()) {
//                        avoidanceContext.append("Recent dinner meals (avoid these): ")
//                                .append(recentDinners.stream().map(m -> m.mainMealName).collect(Collectors.joining(", ")))
//                                .append("\n");
//                    }
//                    if (!recentSnacks.isEmpty()) {
//                        avoidanceContext.append("Recent snack meals (avoid these): ")
//                                .append(recentSnacks.stream().map(m -> m.mainMealName).collect(Collectors.joining(", ")))
//                                .append("\n");
//                    }
//                    avoidanceContext.append("Create completely different meal options that haven't been used in the last 3 days.\n");
//                }
//            }

            String dietaryGuidelines = getDietaryGuidelines(dietType);
            String prompt = String.format(
                    "Generate a personalized diabetes-friendly meal plan for a %d-year-old person with %s diabetes with BMI %.1f. " +
                            "Their diet preference is %s and cuisine preference is %s. %s%s%s" +
                            "CRITICAL ALLERGY SAFETY REQUIREMENTS: %s" +
                            "MEDICAL CONDITION SAFETY REQUIREMENTS: If the user has any medical conditions, completely exclude foods that may worsen or aggravate those conditions." +
                            "%s" +
                            "ADDITIONAL DIETARY RESTRICTIONS: " +
                            "- Avoid processed food completely\n" +
                            "- Allow less processed cheese like goat cheese and grass-fed cheese only\n" +
                            "- Use less oil, dry roast spices and cook\n" +
                            "- Include raw fruits and raw salad (with minimal dressings) in the diet plan\n" +
                            "- Main course recipes should be mostly grilled, avoid adding any Indian gravy to the meal plan\n" +
                            "- Ensure ingredients do not include high carbs or oily recipes for the diet\n" +
                            "SPECIFIC DIETARY RATIOS: " +
                            "- For LFV (Low Fat Vegan) diets: Fat content should not exceed 5%% of total calories\n" +
                            "- For LCHF (Low Carb High Fat) diets: Carbohydrate content should not exceed 20%% of total calories\n" +
                            "Create a full day's meal plan with pre-meal salads/appetizers, main meals, and snacks. Include exact portion sizes, calories, and timing. " +
                            "For each meal, provide detailed nutritional information including carbs, protein, fat, and fiber content. " +
                            "STRICTLY follow the dietary guidelines and restrictions mentioned above. " +
                            "Ensure that the same main ingredient is NOT used more than once across all meals in a single day. " +
                            "Format the response as a JSON object with the specified structure.",
                    age, diabetesType, bmi, dietType, cuisine,
                    allergyStrings.isEmpty() ? "They have no known food allergies." : "They have allergies to: " + allergyStrings + ".",
                    avoidanceContext.toString(),
                    allergyStrings.isEmpty() ? "" : "STRICTLY AVOID ALL FOODS CONTAINING: " + allergyStrings + ". This includes any dishes, ingredients, preparations, or cooking methods that contain or may contain " + allergyStrings + ". DO NOT include any meal that contains these allergens under any circumstances.",
                    dietaryGuidelines
            );

            // Placeholder for AI API call (replace with actual Google AI SDK or HTTP client if available)
            String responseText = callGenerativeAI(prompt); // Implement this method
            try {
                MealPlan mealPlan = objectMapper.readValue(responseText, MealPlan.class);
                return CompletableFuture.completedFuture(mealPlan);
            } catch (Exception parseError) {
                System.err.println("Error parsing JSON from AI response: " + parseError.getMessage());
                return CompletableFuture.completedFuture(getFallbackMealPlan());
            }
        } catch (Exception aiError) {
            System.err.println("Error calling AI API: " + aiError.getMessage());
            return CompletableFuture.completedFuture(getFallbackMealPlan());
        }
    }

    private static String getDietaryGuidelines(String dietType) {
        if (dietType.equalsIgnoreCase("lfv") || dietType.equalsIgnoreCase("Vegetarian") ||
                dietType.equalsIgnoreCase("Vegan") || dietType.equalsIgnoreCase("low fat vegan")) {
            return """
                    IMPORTANT DIETARY GUIDELINES FOR LFV (LOW FAT VEGAN):
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
                    - Oats and oat-based products""";
        } else if (dietType.equalsIgnoreCase("lchf") || dietType.equalsIgnoreCase("Meat-based") ||
                dietType.equalsIgnoreCase("low carb high fat")) {
            return """
                    IMPORTANT DIETARY GUIDELINES FOR LCHF (LOW CARB HIGH FAT):
                    - Follow Low Carb High Fat (LCHF) principles for diabetes management
                    - Vegetables: ALL leafy greens allowed (lettuce, kale, spinach, cabbage, etc.), above-ground vegetables (broccoli, cauliflower, bell peppers, mushrooms, tomatoes, eggplant)
                    - RESTRICT root vegetables (yams, beets, parsnips, turnips, carrots, yuca, etc.) and pumpkin/squash
                    - Use onion, garlic, turmeric, ginger only as spices in limited quantities
                    - Dairy: butter, ghee, hard cheese, paneer, cottage cheese, sour cream, Greek yogurt allowed
                    - RESTRICT whole milk, low-fat milk, curd, buttermilk, ice cream, flavored milk, soft cheese
                    - Nuts & seeds: ALL allowed (almonds, pistachios, brazil nuts, walnuts, pine nuts, hazelnuts, macadamia, pecans, hemp seeds, sunflower seeds, sesame seeds, chia seeds, flax seeds)
                    - Meat, poultry, fish, and eggs are allowed and encouraged
                    
                    COMPLETELY RESTRICTED ITEMS - NEVER INCLUDE:
                    - All grains (rice, wheat, millets, jowar, bajra, corn)
                    - All dal/lentils
                    - All fruits except blueberry, blackberry, and limited strawberries
                    - All cooking oils (soybean, corn, safflower, sunflower, rapeseed, peanut, rice bran, cottonseed, canola, mustard oil)
                    - All added sugars, jaggery, glucose, fructose, high fructose corn syrup, cane sugar, aspartame, corn syrup, maltose, dextrose, sorbitol, mannitol, xylitol, maltodextrin, molasses, brown rice syrup, splenda, nutrasweet, stevia, barley malt
                    - Oats and oat-based products""";
        }
        return "";
    }

    private static String callGenerativeAI(String prompt) {
        // Placeholder for AI API call (implement with actual Google AI SDK or HTTP client)
        // For now, return a mock JSON response
        return """
                {
                    "breakfast": {
                        "preMealName": "Mock fruit salad",
                        "preMealTime": "7:00 AM",
                        "preMealCalories": 60,
                        "mainMealName": "Mock breakfast",
                        "mainMealPortionSize": "1 serving",
                        "mainMealTime": "7:30 AM",
                        "mainMealCalories": 250,
                        "totalCalories": 310,
                        "mainMealNutrients": {
                            "carbs": "30g",
                            "protein": "15g",
                            "fat": "10g",
                            "fiber": "5g"
                        },
                        "carbs": 30,
                        "protein": 15,
                        "fat": 10,
                        "fiber": 5
                    },
                    "lunch": {
                        "preMealName": "Mock greens salad",
                        "preMealTime": "12:30 PM",
                        "preMealCalories": 70,
                        "mainMealName": "Mock lunch",
                        "mainMealPortionSize": "1 serving",
                        "mainMealTime": "1:00 PM",
                        "mainMealCalories": 350,
                        "totalCalories": 420,
                        "mainMealNutrients": {
                            "carbs": "45g",
                            "protein": "20g",
                            "fat": "15g",
                            "fiber": "7g"
                        },
                        "carbs": 45,
                        "protein": 20,
                        "fat": 15,
                        "fiber": 7
                    },
                    "dinner": {
                        "preMealName": "Mock cucumber salad",
                        "preMealTime": "7:30 PM",
                        "preMealCalories": 60,
                        "mainMealName": "Mock dinner",
                        "mainMealPortionSize": "1 serving",
                        "mainMealTime": "8:00 PM",
                        "mainMealCalories": 320,
                        "totalCalories": 380,
                        "mainMealNutrients": {
                            "carbs": "40g",
                            "protein": "18g",
                            "fat": "15g",
                            "fiber": "6g"
                        },
                        "carbs": 40,
                        "protein": 18,
                        "fat": 15,
                        "fiber": 6
                    },
                    "snacks": {
                        "preMealName": "Mock herbal tea",
                        "preMealTime": "4:00 PM",
                        "preMealCalories": 5,
                        "mainMealName": "Mock snack",
                        "mainMealPortionSize": "1 serving",
                        "mainMealTime": "4:30 PM",
                        "mainMealCalories": 150,
                        "totalCalories": 155,
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
                }""";
    }

    public static MealPlan getFallbackMealPlan() {
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

    public static void main(String[] args) {
        // Example usage
        UserProfile profile = new UserProfile(170, 70, "LCHF", 40);
        UserMedicalCondition medicalCondition = new UserMedicalCondition("Type 2");
        List<UserAllergy> allergies = List.of(new UserAllergy("Peanuts"));
        List<UserCuisinePreference> cuisines = List.of(new UserCuisinePreference("Italian"));
//        generatePersonalizedMealPlan(1, profile, medicalCondition, allergies, cuisines)
//                .thenAccept(mealPlan -> System.out.println("Generated meal plan: " + mealPlan));
    }
}