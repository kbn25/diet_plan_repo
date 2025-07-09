//package com.ninja;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.core.type.TypeReference;
//import okhttp3.*;
//import java.io.IOException;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.env.Environment;
//
//public class AIDietGenerator {
//	
//    private static final ObjectMapper objectMapper = new ObjectMapper();
//    private static final OkHttpClient client;
//    
////    @Value("${spring.ai.openai.api-key}")
//    private static String API_KEY = "AIzaSyDXmGZ7aXaJikzH9YvWoeFlsK91beYlz4k";
//    
////    @Value("${spring.ai.openai.base-url}")
//    private static String OPENAI_API_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
//
//    static {
//        client = API_KEY != null ? new OkHttpClient() : null;
//    }
//
//    public static String cleanJsonResponse(String text) {
//        Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*?\\}");
//        Matcher matcher = jsonPattern.matcher(text.trim());
//        return matcher.find() ? matcher.group(0) : "{}";
//    }
//
//    @SuppressWarnings("unchecked")
//    public static Map<String, Object> generateAIDietPlan(Map<String, Object> patientData) {
//        if (client == null) {
//            return getDietRecommendations(patientData);
//        }
//
//        // Extract patient details
//        String name = (String) patientData.getOrDefault("name", "Patient");
//        int age = ((Number) patientData.getOrDefault("age", 30)).intValue();
//        String gender = (String) patientData.getOrDefault("gender", "Not specified");
//        double heightFt = ((Number) patientData.getOrDefault("height_ft", 0)).doubleValue();
//        double heightIn = ((Number) patientData.getOrDefault("height_in", 0)).doubleValue();
//        double weightKg = ((Number) patientData.getOrDefault("weight_kg", 0)).doubleValue();
//
//        // Calculate BMI
//        double heightM = ((heightFt * 12 + heightIn) * 0.0254);
//        double bmi = heightM > 0 ? weightKg / (heightM * heightM) : 0;
//        String bmiCategory;
//        if (bmi < 18.5) {
//            bmiCategory = "Underweight";
//        } else if (bmi < 25) {
//            bmiCategory = "Normal Weight";
//        } else if (bmi < 30) {
//            bmiCategory = "Overweight";
//        } else {
//            bmiCategory = "Obese";
//        }
//
//        // Extract medical conditions
//        Object medicalConditionsObj = patientData.getOrDefault("medical_conditions", new ArrayList<>());
//        String medicalHistory;
//        if (medicalConditionsObj instanceof String) {
//            try {
//                List<String> conditions = objectMapper.readValue((String) medicalConditionsObj, new TypeReference<List<String>>(){});
//                medicalHistory = conditions.stream()
//                        .filter(c -> !"None".equals(c))
//                        .collect(Collectors.joining(", "));
//                medicalHistory = medicalHistory.isEmpty() ? "None specified" : medicalHistory;
//            } catch (Exception e) {
//                System.out.println("DEBUG - Failed to parse medical_conditions JSON, treating as string");
//                medicalHistory = medicalConditionsObj.toString().isEmpty() ? "None specified" : medicalConditionsObj.toString();
//            }
//        } else if (medicalConditionsObj instanceof List) {
//            List<String> conditions = (List<String>) medicalConditionsObj;
//            medicalHistory = conditions.stream()
//                    .filter(c -> !"None".equals(c))
//                    .collect(Collectors.joining(", "));
//            medicalHistory = medicalHistory.isEmpty() ? "None specified" : medicalHistory;
//        } else {
//            medicalHistory = "None specified";
//        }
//
//        // Extract lab values
//        Object labValuesObj = patientData.getOrDefault("lab_values", new HashMap<>());
//        String labValues;
//        if (labValuesObj instanceof Map) {
//            Map<String, String> labValuesMap = (Map<String, String>) labValuesObj;
//            labValues = labValuesMap.getOrDefault("html", "").isEmpty() ?
//                    labValuesMap.getOrDefault("text", labValuesMap.getOrDefault("content", "")) : labValuesMap.get("html");
//        } else {
//            labValues = labValuesObj.toString();
//        }
//        labValues = labValues.isEmpty() ? "None available" : labValues;
//
//        // Other dietary preferences
//        String dietType = (String) patientData.getOrDefault("diet_type", "Regular");
//        String allergies = (String) patientData.getOrDefault("allergies", "None specified");
//        String foodPreferences = (String) patientData.getOrDefault("food_preferences", "None specified");
//        String ethnicity = (String) patientData.getOrDefault("ethnicity", "No specific preference");
//        String cuisinePreference = (String) patientData.getOrDefault("cuisine_preference", ethnicity);
//        String activityLevel = (String) patientData.getOrDefault("activity_level", "Moderate");
//        String proteinRestricted = (String) patientData.getOrDefault("protein_restricted", "None");
//        String spiceLevel = (String) patientData.getOrDefault("spice_level", "Medium");
//        String language = (String) patientData.getOrDefault("preferred_language", "English");
//        String companyName = "DIA";
//
//        // Calculate calorie needs (Mifflin-St Jeor Equation)
//        double heightCm = heightM * 100;
//        int calorieNeeds;
//        if (gender.equalsIgnoreCase("male")) {
//            calorieNeeds = (int) (10 * weightKg + 6.25 * heightCm - 5 * age + 5);
//        } else {
//            calorieNeeds = (int) (10 * weightKg + 6.25 * heightCm - 5 * age - 161);
//        }
//
//        // Adjust for activity level
//        Map<String, Double> activityMultipliers = new HashMap<>();
//        activityMultipliers.put("sedentary", 1.2);
//        activityMultipliers.put("light", 1.375);
//        activityMultipliers.put("moderate", 1.55);
//        activityMultipliers.put("active", 1.725);
//        activityMultipliers.put("very active", 1.9);
//        double multiplier = activityMultipliers.getOrDefault(activityLevel.toLowerCase(), 1.55);
//        calorieNeeds = (int) (calorieNeeds * multiplier);
//
//        // Define meal types
//        List<String> mealTypes = Arrays.asList("breakfast", "morning_snack", "lunch", "afternoon_snack", "dinner", "evening_snack");
//
//        // Create the AI prompt
//        String prompt = String.format("""
//                You are a clinical dietitian creating a professionally styled HTML diet plan document.
//
//                PATIENT INFORMATION:
//                - Name: %s
//                - Age: %d
//                - Gender: %s
//                - Height: %.0f' %.0f"
//                - Weight: %.1f kg
//                - BMI: %.1f (%s)
//                - Activity Level: %s
//                - Estimated Daily Calorie Needs: ~%d kcal
//                - Medical Conditions (Give this as priority based on this diet items should be selected and given in respective amount): %s
//                - Lab Values: %s
//                - Diet Type: %s
//                - Food Allergies/Intolerances (completely avoid these in the diet plans): %s
//                - Additional Food Preferences/Restrictions: %s
//                - Ethnicity/Cultural Background: %s
//                - Cuisine Preference: %s
//                - Protein Restrictions: %s
//                - Spice Level Preference: %s
//
//                CREATE AN HTML BODY FOR A DIET PLAN with the following sections:
//
//                1. Brief patient summary (DO NOT include patient name in this section - only show age, gender, height, weight, BMI, activity level - DO NOT include lab values)(Show this information in a rectangle box with columns and rows with proper spacing(max three columns))
//                2. Summary of nutritional goals based on patient needs
//                3. Seven day meal plan recommendations with:
//                   - Breakfast (3 Combinations, with portion sizes and calories)
//                   - Morning Snack (2-3 Combinations, with portion sizes)
//                   - Lunch (3 Combinations, with portion sizes and calories)
//                   - Afternoon Snack (2-3 Combinations, with portion sizes)
//                   - Dinner (3 Combinations, with portion sizes and calories)
//                   - Evening Snack (2-3 Combinations if appropriate, with portion sizes)
//                   - include content why we are giving these recommendations.
//                   - show the options as option 1, option 2, etc.
//                4. Place special markers for the macro visualization charts to be inserted by our system:
//                   - For each meal type, insert this placeholder: <!--MACRO_CHART_PLACEHOLDER_[MEAL_TYPE]-->
//                   - Example: <!--MACRO_CHART_PLACEHOLDER_BREAKFAST-->
//                5. Dietary restrictions and foods to avoid section
//                6. Special instructions section
//                7. Recommended supplements section
//                8. In the meal plan recommendations, select one best for the patient profile and mark it as "Recommended".
//                9. The calories should be calculated based on the given diet plan, the calories calculation is the most important part of the diet plan. So, it should be accurate.
//                10. For snacks also provide the calories and macronutrient content.
//                [All sections should be clearly separated with appropriate headings]
//
//                Calories calculation:
//                - The calories are to be calculated for the ingredients based on 'USDA data'.
//                - You can do calculations based on the given portion size to get the correct calories.
//
//                Each meal should have a detailed description to include:
//                - Ingredients with quantities
//                - Approximate calories per serving
//                - Preparation method if needed
//                - Nutritional highlights (rich in protein, fiber, etc.)
//                - Why this meal is recommended for the patient's needs info
//
//                STYLE REQUIREMENTS:
//                - Use a clean, consistent style with appropriate colors for a nutrition document
//                - Use proper heading hierarchy (h1, h2, h3)
//                - Use tables for presenting meal options clearly
//                - Include appropriate spacing between sections
//                - ONLY provide the BODY content (no html, head, title, body tags)
//                - Use inline CSS for consistent styling
//
//                CONTENT REQUIREMENTS:
//                - Make all recommendations personalized based on the patient's profile
//                - Ensure meals are culturally appropriate based on ethnicity/cuisine preferences
//                - Explain nutritional rationale for recommendations
//                - Focus on clarity and readability
//                - Provide actual meal recommendations, not just food groups
//                - Provide detailed options rather than general suggestions
//                - Include why that combination of foods is recommended by telling about the health condition.
//
//                OUTPUT FORMAT:
//                - Return valid HTML with inline CSS styles
//                - Do NOT include <html>, <head>, or <body> tags - just provide the body content
//                - Use consistent styling with a clean, professional look
//                - Use div-based layout with modern CSS
//                - Language: %s Give the output in %s.
//
//                ALSO, in addition to the HTML, I need information about the estimated macronutrient content for each meal type. The macronutrient should be in percentage of the total calories, and it should be more accurate based on the meal.
//
//                Structure this data as follows (for the recommended selected option of each meal type):
//                breakfast_protein_g: [protein percentage in breakfast option]
//                breakfast_carbs_g: [carbs percentage in breakfast option]
//                breakfast_fat_g: [fat percentage in breakfast option]
//                morning_snack_protein_g: [protein percentage in morning snack option]
//                morning_snack_carbs_g: [carbs percentage in morning snack option]
//                morning_snack_fat_g: [fat percentage in snack option]
//                lunch_protein_g: [protein percentage in lunch option]
//                lunch_carbs_g: [carbs percentage in lunch option]
//                lunch_fat_g: [fat percentage in lunch option]
//                afternoon_snack_protein_g: [protein percentage in afternoon snack option]
//                afternoon_snack_carbs_g: [carbs percentage in afternoon snack option]
//                afternoon_snack_fat_g: [fat percentage in afternoon snack option]
//                dinner_protein_g: [protein percentage in dinner option]
//                dinner_carbs_g: [carbs percentage in dinner option]
//                dinner_fat_g: [fat percentage in dinner option]
//                evening_snack_protein_g: [protein percentage in evening snack option]
//                evening_snack_carbs_g: [carbs percentage in evening snack option]
//                evening_snack_fat_g: [fat percentage in evening snack option]
//
//                Add these values at the bottom of your response in the exact format shown.
//                I'll extract these values programmatically.
//                Check the attached documents to get info for diet plan generation items, we have LFV and LCHF by analyzing the patient data and their preferences from the attached documents data give the diet plans.
//                """, name, age, gender, heightFt, heightIn, weightKg, bmi, bmiCategory, activityLevel, calorieNeeds,
//                medicalHistory, labValues, dietType, allergies, foodPreferences, ethnicity, cuisinePreference,
//                proteinRestricted, spiceLevel, language, language);
//
//        try {
//            // Call OpenAI API
//            String content = callOpenAIApi(prompt);
//            if (content.startsWith("```html")) {
//                content = content.substring(7);
//            }
//            if (content.endsWith("```")) {
//                content = content.substring(0, content.length() - 3);
//            }
//
//            // Extract macro data
//            Map<String, Map<String, Integer>> macrosData = new HashMap<>();
//            List<String> nutrients = Arrays.asList("protein", "carbs", "fat");
//            for (String meal : mealTypes) {
//                for (String nutrient : nutrients) {
//                    Pattern pattern = Pattern.compile(meal + "_" + nutrient + "_g:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
//                    Matcher matcher = pattern.matcher(content);
//                    if (matcher.find()) {
//                        macrosData.computeIfAbsent(meal, k -> new HashMap<>());
//                        try {
//                            macrosData.get(meal).put(nutrient, Integer.parseInt(matcher.group(1)));
//                        } catch (NumberFormatException e) {
//                            macrosData.get(meal).put(nutrient, 0);
//                        }
//                    }
//                }
//            }
//
//            // Remove macro data section
//            List<int[]> macroPatterns = new ArrayList<>();
//            for (String meal : mealTypes) {
//                for (String nutrient : nutrients) {
//                    Pattern pattern = Pattern.compile(meal + "_" + nutrient + "_g:\\s*\\d+", Pattern.CASE_INSENSITIVE);
//                    Matcher matcher = pattern.matcher(content);
//                    while (matcher.find()) {
//                        macroPatterns.add(new int[]{matcher.start(), matcher.end()});
//                    }
//                }
//            }
//            if (!macroPatterns.isEmpty()) {
//                macroPatterns.sort(Comparator.comparingInt(a -> a[0]));
//                int earliestStart = macroPatterns.get(0)[0];
//                int sectionStart = 0;
//                for (int i = earliestStart - 1; i > 0; i--) {
//                    if (content.charAt(i) == '\n' && content.charAt(i - 1) == '\n') {
//                        sectionStart = i + 1;
//                        break;
//                    }
//                }
//                int latestEnd = macroPatterns.get(macroPatterns.size() - 1)[1];
//                int sectionEnd = content.length();
//                int nextBlankLine = content.indexOf("\n\n", latestEnd);
//                if (nextBlankLine != -1) {
//                    sectionEnd = nextBlankLine;
//                }
//                content = content.substring(0, sectionStart) + content.substring(sectionEnd);
//            }
//
//            // Extract meal recommendations
//            Map<String, Object> recommendations = new HashMap<>();
//            recommendations.put("breakfast", extractMealRecommendation(content, "breakfast", "Breakfast"));
//            recommendations.put("morning_snack", extractMealRecommendation(content, "morning[_\\s]snack", "Morning Snack"));
//            recommendations.put("lunch", extractMealRecommendation(content, "lunch", "Lunch"));
//            recommendations.put("afternoon_snack", extractMealRecommendation(content, "afternoon[_\\s]snack", "Afternoon Snack"));
//            recommendations.put("dinner", extractMealRecommendation(content, "dinner", "Dinner"));
//            recommendations.put("evening_snack", extractMealRecommendation(content, "evening[_\\s]snack", "Evening Snack"));
//            recommendations.put("supplements", extractSection(content, "supplement", "Supplements"));
//            recommendations.put("special_instructions", extractSection(content, "special[_\\s]instructions", "Special Instructions"));
//            recommendations.put("restrictions", extractSection(content, "restrictions", "Restrictions"));
//            recommendations.put("foods_to_avoid", extractSection(content, "foods[_\\s]to[_\\s]avoid", "Foods to Avoid"));
//            recommendations.put("body_content", content);
//            recommendations.put("macros", macrosData);
//
//            return recommendations;
//        } catch (Exception e) {
//            System.err.println("Error generating AI diet recommendations: " + e.getMessage());
//            return getDietRecommendations(patientData);
//        }
//    }
//
//    private static String callOpenAIApi(String prompt) throws IOException {
//        // Simulate OpenAI API call with OkHttp
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", "gemini-2.0-flash");
//        requestBody.put("messages", Arrays.asList(
//                Map.of("role", "system", "content", "You are a clinical dietitian expert specialized in personalized nutrition planning and document design. Regarding diet plans, be cautious, check two, three times and make sure that the diet plan and macros are accurate and correct. Analyze the patient health data first and then make reasoning 'what diet plan is best for this patient?', 'Why this diet plan only perfectly fits for this patient?', 'what are the benefits of this diet plan for this patient?', 'what are the risks of this diet plan for this patient?' and then give the diet plan. After the reasoning process, make a well-suited diet plan for the patient health conditions."),
//                Map.of("role", "user", "content", prompt)
//        ));
//        requestBody.put("reasoning", Map.of("effort", "medium"));
//
//        RequestBody body = RequestBody.create(
//                objectMapper.writeValueAsString(requestBody),
//                MediaType.parse("application/json")
//        );
//        Request request = new Request.Builder()
//                .url(OPENAI_API_URL)
//                .addHeader("Authorization", "Bearer " + API_KEY)
//                .post(body)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            String responseBody = response.body().string();
//            Map<String, Object> responseJson = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>(){});
//            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseJson.get("choices");
//            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
//            return (String) message.get("content");
//        }
//    }
//
//    private static Map<String, Object> getDietRecommendations(Map<String, Object> patientData) {
//        // Fallback recommendations (simplified)
//        Map<String, Object> recommendations = new HashMap<>();
//        recommendations.put("breakfast", "- Fallback breakfast: Oatmeal with fruit");
//        recommendations.put("morning_snack", "- Fallback morning snack: Apple slices");
//        recommendations.put("lunch", "- Fallback lunch: Grilled chicken salad");
//        recommendations.put("afternoon_snack", "- Fallback afternoon snack: Almonds");
//        recommendations.put("dinner", "- Fallback dinner: Steamed vegetables with fish");
//        recommendations.put("evening_snack", "- Fallback evening snack: Greek yogurt");
//        recommendations.put("supplements", "- No supplements recommended");
//        recommendations.put("special_instructions", "- Follow a balanced diet");
//        recommendations.put("restrictions", "- Avoid processed foods");
//        recommendations.put("foods_to_avoid", "- None specified");
//        recommendations.put("body_content", "<div>Fallback diet plan</div>");
//        recommendations.put("macros", new HashMap<>());
//        return recommendations;
//    }
//
//    private static String extractMealRecommendation(String htmlContent, String mealPattern, String mealTitle) {
//        Pattern pattern = Pattern.compile("<h[2-3][^>]*>(" + mealTitle + "|" + mealPattern + ")", Pattern.CASE_INSENSITIVE);
//        Matcher match = pattern.matcher(htmlContent);
//        if (!match.find()) {
//            return "";
//        }
//
//        int startIdx = match.start();
//        int endIdx = htmlContent.length();
//        Matcher nextHeading = Pattern.compile("<h[2-3][^>]*>", Pattern.CASE_INSENSITIVE).matcher(htmlContent.substring(startIdx + 1));
//        if (nextHeading.find()) {
//            endIdx = startIdx + 1 + nextHeading.start();
//        }
//
//        String section = htmlContent.substring(startIdx, endIdx);
//        section = section.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
//        String[] lines = section.split("\\. ");
//        return Arrays.stream(lines)
//                .filter(line -> line.length() > 5)
//                .map(line -> "- " + line)
//                .collect(Collectors.joining("\n"));
//    }
//
//    private static String extractSection(String content, String sectionPattern, String sectionTitle) {
//        try {
//            Pattern pattern = Pattern.compile("<h[2-4][^>]*>(" + sectionTitle + "|" + sectionPattern + ")", Pattern.CASE_INSENSITIVE);
//            Matcher match = pattern.matcher(content);
//            if (!match.find()) {
//                return "";
//            }
//
//            int startIdx = match.start();
//            int endIdx = content.length();
//            Matcher nextHeading = Pattern.compile("<h[2-4][^>]*>", Pattern.CASE_INSENSITIVE).matcher(content.substring(startIdx + 1));
//            if (nextHeading.find()) {
//                endIdx = startIdx + 1 + nextHeading.start();
//            }
//
//            String section = content.substring(startIdx, endIdx);
//            section = section.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
//            String[] lines = section.split("\\. ");
//            return Arrays.stream(lines)
//                    .filter(line -> line.length() > 5)
//                    .map(line -> "- " + line)
//                    .collect(Collectors.joining("\n"));
//        } catch (Exception e) {
//            return "";
//        }
//    }
//
//    public static void main(String[] args) {
//        // Example usage
//        Map<String, Object> patientData = new HashMap<>();
//        patientData.put("name", "John Doe");
//        patientData.put("age", 40);
//        patientData.put("gender", "Male");
//        patientData.put("height_ft", 5);
//        patientData.put("height_in", 10);
//        patientData.put("weight_kg", 80);
//        patientData.put("diet_type", "LCHF");
//        patientData.put("activity_level", "Moderate");
//        patientData.put("medical_conditions", Arrays.asList("Diabetes Type 2"));
//        patientData.put("allergies", "Peanuts");
//        patientData.put("ethnicity", "Italian");
//        patientData.put("cuisine_preference", "Italian");
//        patientData.put("lab_values", Map.of("text", "A1C: 7.0%"));
//        Map<String, Object> result = generateAIDietPlan(patientData);
//        System.out.println("Generated Diet Plan: " + result);
//    }
//}