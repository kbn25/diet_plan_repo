package com.ninja.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ninja.entity.Food;
import com.ninja.entity.Nutrient;
import com.ninja.service.MealPlanningService;

/**
 * REST API Controller for Meal Planning functionality. Provides HTTP endpoints
 * for accessing food and nutritional data. This controller complements the MCP
 * tools by providing direct REST API access.
 */
@RestController
@RequestMapping("/api/v1/diet_plan")
@CrossOrigin(origins = "*") // Configure as needed for your frontend
public class MealPlannerController {

	private final MealPlanningService mealPlanningService;

	@Autowired
	public MealPlannerController(MealPlanningService mealPlanningService) {
		this.mealPlanningService = mealPlanningService;
	}

	/**
	 * Health check endpoint
	 */
	@GetMapping("/health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Meal Planner MCP Server is running!");
	}

	// FOOD ENDPOINTS

	/**
	 * Search foods by name GET
	 * /api/v1/meal-planner/foods/search?q=chicken&page=0&size=10
	 */
	@GetMapping("/foods/search")
	public ResponseEntity<Page<Food>> searchFoods(@RequestParam(value = "q", required = false) String searchTerm,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size) {

		Page<Food> foods = mealPlanningService.searchFoodsPaginated(searchTerm, page, size);
		return ResponseEntity.ok(foods);
	}

	/**
	 * Get food by FDC ID GET /api/v1/meal-planner/foods/123456
	 */
	@GetMapping("/foods/{fdcId}")
	public ResponseEntity<Food> getFoodById(@PathVariable Long fdcId) {
		Optional<Food> food = mealPlanningService.getFoodById(fdcId);
		return food.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Get all food categories GET /api/v1/meal-planner/foods/categories
	 */
	@GetMapping("/foods/categories")
	public ResponseEntity<?> getFoodCategories() {
//		List<String> categories = mealPlanningService.getAllFoodCategories();
		return mealPlanningService.getAllFoodCategories();
//				ResponseEntity.ok(categories);
	}

	/**
	 * Get foods by category GET
	 * /api/v1/meal-planner/foods/categories/Dairy%20and%20Egg%20Products
	 */
	@GetMapping("/foods/categories/{category}")
	public ResponseEntity<List<Food>> getFoodsByCategory(@PathVariable String category) {
		List<Food> foods = mealPlanningService.getFoodsByCategory(category);
		return ResponseEntity.ok(foods);
	}

	/**
	 * Get foods without allergens GET /api/v1/meal-planner/foods/allergen-free
	 */
	@GetMapping("/foods/allergen-free")
	public ResponseEntity<List<Food>> getFoodsWithoutAllergens() {
		List<Food> foods = mealPlanningService.findFoodsWithoutAllergens();
		return ResponseEntity.ok(foods);
	}

	/**
	 * Get foods with allergens GET /api/v1/meal-planner/foods/with-allergens
	 */
	@GetMapping("/foods/with-allergens")
	public ResponseEntity<List<Food>> getFoodsWithAllergens() {
		List<Food> foods = mealPlanningService.findFoodsWithAllergens();
		return ResponseEntity.ok(foods);
	}

	// NUTRIENT ENDPOINTS

	/**
	 * Search nutrients by food name GET
	 * /api/v1/meal-planner/nutrients/search?q=apple&page=0&size=10
	 */
	@GetMapping("/nutrients/search")
	public ResponseEntity<Page<Nutrient>> searchNutrients(
			@RequestParam(value = "q", required = false) String searchTerm,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size) {

		Page<Nutrient> nutrients = mealPlanningService.searchNutrientsPaginated(searchTerm, page, size);
		return ResponseEntity.ok(nutrients);
	}

	/**
	 * Get nutrient information by FDC ID GET /api/v1/meal-planner/nutrients/123456
	 */
	@GetMapping("/nutrients/{fdcId}")
	public ResponseEntity<Nutrient> getNutrientById(@PathVariable Long fdcId) {
		Optional<Nutrient> nutrient = mealPlanningService.getNutrientsByFdcId(fdcId);
		return nutrient.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Find high protein foods GET
	 * /api/v1/meal-planner/nutrients/high-protein?min=15.0
	 */
	@GetMapping("/nutrients/high-protein")
	public ResponseEntity<List<Nutrient>> getHighProteinFoods(
			@RequestParam(value = "min", defaultValue = "10.0") Double minProtein) {
		List<Nutrient> nutrients = mealPlanningService.findHighProteinFoods(minProtein);
		return ResponseEntity.ok(nutrients);
	}

	/**
	 * Find low calorie foods GET
	 * /api/v1/meal-planner/nutrients/low-calorie?max=100.0
	 */
	@GetMapping("/nutrients/low-calorie")
	public ResponseEntity<List<Nutrient>> getLowCalorieFoods(
			@RequestParam(value = "max", defaultValue = "100.0") Double maxCalories) {
		List<Nutrient> nutrients = mealPlanningService.findLowCalorieFoods(maxCalories);
		return ResponseEntity.ok(nutrients);
	}

	/**
	 * Find foods in calorie range GET
	 * /api/v1/meal-planner/nutrients/calorie-range?min=50&max=200
	 */
	@GetMapping("/nutrients/calorie-range")
	public ResponseEntity<List<Nutrient>> getFoodsInCalorieRange(
			@RequestParam(value = "min", defaultValue = "0") Double minCalories,
			@RequestParam(value = "max", defaultValue = "500") Double maxCalories) {
		List<Nutrient> nutrients = mealPlanningService.findFoodsInCalorieRange(minCalories, maxCalories);
		return ResponseEntity.ok(nutrients);
	}

	/**
	 * Find high fiber foods GET /api/v1/meal-planner/nutrients/high-fiber?min=5.0
	 */
	@GetMapping("/nutrients/high-fiber")
	public ResponseEntity<List<Nutrient>> getHighFiberFoods(
			@RequestParam(value = "min", defaultValue = "3.0") Double minFiber) {
		List<Nutrient> nutrients = mealPlanningService.findHighFiberFoods(minFiber);
		return ResponseEntity.ok(nutrients);
	}

	/**
	 * Find low sodium foods GET /api/v1/meal-planner/nutrients/low-sodium?max=140.0
	 */
	@GetMapping("/nutrients/low-sodium")
	public ResponseEntity<List<Nutrient>> getLowSodiumFoods(
			@RequestParam(value = "max", defaultValue = "140.0") Double maxSodium) {
		List<Nutrient> nutrients = mealPlanningService.findLowSodiumFoods(maxSodium);
		return ResponseEntity.ok(nutrients);
	}

	/**
	 * Find vitamin-rich foods GET
	 * /api/v1/meal-planner/nutrients/vitamin-rich?type=C&min=10.0
	 */
	@GetMapping("/nutrients/vitamin-rich")
	public ResponseEntity<List<Nutrient>> getVitaminRichFoods(@RequestParam(value = "type") String vitaminType,
			@RequestParam(value = "min", required = false) Double minAmount) {

		try {
			List<Nutrient> nutrients = mealPlanningService.findVitaminRichFoods(vitaminType, minAmount);
			return ResponseEntity.ok(nutrients);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Find foods for dietary restrictions GET
	 * /api/v1/meal-planner/nutrients/dietary?lowSodium=true&highFiber=true
	 */
	@GetMapping("/nutrients/dietary")
	public ResponseEntity<List<Nutrient>> getFoodsForDiet(
			@RequestParam(value = "lowSodium", required = false) Boolean lowSodium,
			@RequestParam(value = "lowFat", required = false) Boolean lowFat,
			@RequestParam(value = "highFiber", required = false) Boolean highFiber,
			@RequestParam(value = "lowSugar", required = false) Boolean lowSugar) {

		List<Nutrient> nutrients = mealPlanningService.findFoodsForDiet(lowSodium, lowFat, highFiber, lowSugar);
		return ResponseEntity.ok(nutrients);
	}

	/**
	 * Find foods with balanced macronutrients GET
	 * /api/v1/meal-planner/nutrients/balanced
	 */
	@GetMapping("/nutrients/balanced")
	public ResponseEntity<List<Nutrient>> getBalancedFoods() {
		List<Nutrient> nutrients = mealPlanningService.findBalancedFoods();
		return ResponseEntity.ok(nutrients);
	}

	// STATISTICS ENDPOINTS

	/**
	 * Get nutritional statistics GET /api/v1/meal-planner/stats/nutrition
	 */
	@GetMapping("/stats/nutrition")
	public ResponseEntity<Object[]> getNutritionalStats() {
		Object[] stats = mealPlanningService.getNutritionalStatistics();
		return ResponseEntity.ok(stats);
	}

	/**
	 * Count foods by category GET
	 * /api/v1/meal-planner/stats/category-count/Vegetables%20and%20Vegetable%20Products
	 */
	@GetMapping("/stats/category-count/{category}")
	public ResponseEntity<Long> countFoodsByCategory(@PathVariable String category) {
		Long count = mealPlanningService.countFoodsByCategory(category);
		return ResponseEntity.ok(count);
	}
}