package com.ninja.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ninja.entity.Food;
import com.ninja.entity.Nutrient;
import com.ninja.repository.FoodRepository;
import com.ninja.repository.NutrientRepository;


/**
 * Service class containing meal planning business logic and MCP tools. This
 * service provides both programmatic access and MCP tool capabilities for AI
 * assistants to interact with the meal planning database.
 */
@Service
public class MealPlanningService {

	private final FoodRepository foodRepository;
	private final NutrientRepository nutrientRepository;

	@Autowired
	public MealPlanningService(FoodRepository foodRepository, NutrientRepository nutrientRepository) {
		this.foodRepository = foodRepository;
		this.nutrientRepository = nutrientRepository;
	}

	@Tool(description = "Check the server health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Meal Planner MCP Server is running!");
	}
	/**
	 * MCP Tool: Search for foods by name This tool allows AI assistants to search
	 * for foods in the database
	 */
	@Tool(name="searchByFoodName", description = "Search for foods by name or partial name match")
	public ResponseEntity<?> searchFoodsByName(
			@ToolParam(description = "one word - food by name") String searchTerm) {
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			throw new IllegalArgumentException("Search term cannot be empty");
		}
		return ResponseEntity.ok(Map.of("response ", foodRepository.findByFoodNameContainingIgnoreCase(searchTerm.trim())));
	}

	/**
	 * MCP Tool: Get food by FDC ID Retrieve specific food item by its unique
	 * identifier
	 */
	@Tool(description = "Get detailed food information by FDC ID")
	public Optional<Food> getFoodById(
			@ToolParam(description = "a positive number - FDC ID") Long fdcId) {
		if (fdcId == null || fdcId <= 0) {
			throw new IllegalArgumentException("FDC ID must be a positive number");
		}
		return foodRepository.findById(fdcId);
	}

	/**
	 * MCP Tool: Get all food categories Returns list of all available food
	 * categories for meal planning
	 */
	@Tool(description = "Get all available food categories")
	public ResponseEntity<?> getAllFoodCategories() {
		return ResponseEntity.ok(Map.of("response ", foodRepository.findDistinctFoodCategories()));
	}

	/**
	 * MCP Tool: Get foods by category Find all foods in a specific category
	 */
	@Tool(description = "Get all foods in a specific category")
	public List<Food> getFoodsByCategory(
			@ToolParam(description = "category of the food") String category) {
		if (category == null || category.trim().isEmpty()) {
			throw new IllegalArgumentException("Category cannot be empty");
		}
		return foodRepository.findByFoodCategoryIgnoreCase(category.trim());
	}

	/**
	 * MCP Tool: Get nutritional information by FDC ID Retrieve detailed nutritional
	 * data for a specific food
	 */
	@Tool(description = "Get detailed nutritional information for a food by FDC ID")
	public Optional<Nutrient> getNutrientsByFdcId(
			@ToolParam(description = "a positive number - FDC ID")  Long fdcId) {
		if (fdcId == null || fdcId <= 0) {
			throw new IllegalArgumentException("FDC ID must be a positive number");
		}
		return nutrientRepository.findById(fdcId);
	}

	/**
	 * MCP Tool: Search nutrients by food name Find nutritional information by
	 * searching food names
	 */
	@Tool(description = "Search nutritional information by food name")
	public List<Nutrient> searchNutrientsByFoodName(
			@ToolParam(description = "one word - food by name") String searchTerm) {
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			throw new IllegalArgumentException("Search term cannot be empty");
		}
		return nutrientRepository.searchFoodsByAllNames(searchTerm.trim());
	}

	/**
	 * MCP Tool: Find high protein foods Get foods with protein content above
	 * specified threshold
	 */
	@Tool(description = "Find foods with high protein content (minimum grams of protein per 100g)")
	public List<Nutrient> findHighProteinFoods(
			@ToolParam(description = "a positive decimal number specifying minimum grams of protein per 100g ")  Double minProteinGrams) {
		if (minProteinGrams == null || minProteinGrams < 0) {
			minProteinGrams = 10.0; // Default minimum protein
		}
		return nutrientRepository.findHighProteinFoods(BigDecimal.valueOf(minProteinGrams));
	}

	/**
	 * MCP Tool: Find low calorie foods Get foods with calorie content below
	 * specified threshold
	 */
	@Tool(description = "Find foods with low calorie content (maximum calories per 100g)")
	public List<Nutrient> findLowCalorieFoods(
			@ToolParam(description = "a positive decimal number specifying maximum calories per 100g ") Double maxCalories) {
		if (maxCalories == null || maxCalories <= 0) {
			maxCalories = 100.0; // Default maximum calories
		}
		return nutrientRepository.findLowCalorieFoods(BigDecimal.valueOf(maxCalories));
	}

	/**
	 * MCP Tool: Find foods in calorie range Get foods within specified calorie
	 * range for meal planning
	 */
	@Tool(description = "Find foods within a specific calorie range per 100g")
	public List<Nutrient> findFoodsInCalorieRange(
			@ToolParam(description = "a positive decimal number specifying calorie per 100g ") Double minCalories, 
			@ToolParam(description = "a positive decimal number specifying calorie per 100g ") Double maxCalories) {
		if (minCalories == null || minCalories < 0) {
			minCalories = 0.0;
		}
		if (maxCalories == null || maxCalories <= minCalories) {
			maxCalories = minCalories + 500.0; // Default range
		}
		return nutrientRepository.findFoodsInCalorieRange(BigDecimal.valueOf(minCalories),
				BigDecimal.valueOf(maxCalories));
	}

	/**
	 * MCP Tool: Find high fiber foods Get foods with high fiber content for
	 * digestive health
	 */
	@Tool(description = "Find foods with high fiber content (minimum grams of fiber per 100g)")
	public List<Nutrient> findHighFiberFoods(
			@ToolParam(description = "a positive decimal number specifying minimum grams of fiber per 100g ") Double minFiberGrams) {
		if (minFiberGrams == null || minFiberGrams < 0) {
			minFiberGrams = 3.0; // Default minimum fiber
		}
		return nutrientRepository.findHighFiberFoods(BigDecimal.valueOf(minFiberGrams));
	}

	/**
	 * MCP Tool: Find low sodium foods Get foods with low sodium content for
	 * heart-healthy diets
	 */
	@Tool(description = "Find foods with low sodium content (maximum milligrams of sodium per 100g)")
	public List<Nutrient> findLowSodiumFoods(
			@ToolParam(description = "a positive decimal number specifying maximum milligrams of sodium per 100g ") Double maxSodiumMg) {
		if (maxSodiumMg == null || maxSodiumMg < 0) {
			maxSodiumMg = 140.0; // Default maximum sodium (low sodium threshold)
		}
		return nutrientRepository.findLowSodiumFoods(BigDecimal.valueOf(maxSodiumMg));
	}

	/**
	 * MCP Tool: Find vitamin-rich foods Get foods rich in specific vitamins or
	 * minerals
	 */
	@Tool(description = "Find foods rich in specific vitamins or minerals. Options: C, D, CALCIUM, IRON, POTASSIUM, MAGNESIUM")
	public List<Nutrient> findVitaminRichFoods(
			@ToolParam(description = "vitamins or minerals. Options: C, D, CALCIUM, IRON, POTASSIUM, MAGNESIUM") String vitaminType,
			@ToolParam(description = "a positive decimal number specifying minAmount ") Double minAmount) {
		if (vitaminType == null || vitaminType.trim().isEmpty()) {
			throw new IllegalArgumentException("Vitamin type must be specified");
		}

		String normalizedType = vitaminType.trim().toUpperCase();
		if (!List.of("C", "D", "CALCIUM", "IRON", "POTASSIUM", "MAGNESIUM").contains(normalizedType)) {
			throw new IllegalArgumentException(
					"Invalid vitamin type. Options: C, D, CALCIUM, IRON, POTASSIUM, MAGNESIUM");
		}

		if (minAmount == null || minAmount < 0) {
			// Set default minimum amounts based on vitamin/mineral type
			minAmount = switch (normalizedType) {
			case "C" -> 10.0; // mg
			case "D" -> 1.0; // mcg
			case "CALCIUM" -> 50.0; // mg
			case "IRON" -> 2.0; // mg
			case "POTASSIUM" -> 200.0; // mg
			case "MAGNESIUM" -> 20.0; // mg
			default -> 1.0;
			};
		}

		return nutrientRepository.findFoodsRichInVitaminMineral(normalizedType, BigDecimal.valueOf(minAmount));
	}

	/**
	 * MCP Tool: Find foods for dietary restrictions Get foods suitable for specific
	 * dietary needs
	 */
	@Tool(description = "Find foods suitable for dietary restrictions (lowSodium, lowFat, highFiber, lowSugar)")
	public List<Nutrient> findFoodsForDiet(
			@ToolParam(description = "true or false value for lowSodium") Boolean lowSodium, 
			@ToolParam(description = "true or false value for lowFat") Boolean lowFat, 
			@ToolParam(description = "true or false value for highFiber") Boolean highFiber, 
			@ToolParam(description = "true or false value for lowSugar") Boolean lowSugar) {
		return nutrientRepository.findFoodsForDietaryRestrictions(lowSodium != null && lowSodium,
				lowFat != null && lowFat, highFiber != null && highFiber, lowSugar != null && lowSugar);
	}

	/**
	 * MCP Tool: Get balanced macronutrient foods Find foods with balanced protein,
	 * fat, and carbohydrate ratios
	 */
	@Tool(description = "Find foods with balanced macronutrient ratios (good protein, fat, carb balance)")
	public List<Nutrient> findBalancedFoods() {
		return nutrientRepository.findBalancedMacronutrientFoods();
	}

	/**
	 * MCP Tool: Get foods without allergens Find foods that don't contain common
	 * allergens
	 */
	@Tool(description = "Find foods without allergen flags (safer for people with allergies)")
	public List<Food> findFoodsWithoutAllergens() {
		return foodRepository.findFoodsWithoutAllergens();
	}

	/**
	 * MCP Tool: Get foods with allergens Find foods that contain allergen
	 * information
	 */
	@Tool(description = "Find foods with allergen information")
	public List<Food> findFoodsWithAllergens() {
		return foodRepository.findFoodsWithAllergens();
	}

	// Non-MCP service methods for REST API and internal use

	/**
	 * Get paginated food search results
	 */
	public Page<Food> searchFoodsPaginated(String searchTerm, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			return foodRepository.findAll(pageable);
		}
		return foodRepository.findByFoodNameContainingIgnoreCase(searchTerm.trim(), pageable);
	}

	/**
	 * Get paginated nutrient search results
	 */
	public Page<Nutrient> searchNutrientsPaginated(String searchTerm, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			return nutrientRepository.findAll(pageable);
		}
		return nutrientRepository.findByFoodNameContainingIgnoreCase(searchTerm.trim(), pageable);
	}

	/**
	 * Count foods by category
	 */
	public Long countFoodsByCategory(String category) {
		return foodRepository.countByFoodCategory(category);
	}

	/**
	 * Get nutritional statistics
	 */
	public Object[] getNutritionalStatistics() {
		return nutrientRepository.getNutritionalStatistics();
	}
}