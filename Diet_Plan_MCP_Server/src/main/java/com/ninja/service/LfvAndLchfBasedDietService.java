package com.ninja.service;

import java.util.List;
import java.util.Optional;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.ninja.entity.LchfFood;
import com.ninja.entity.LfvFood;
import com.ninja.projection.FoodLchfView;
import com.ninja.repository.LchfFoodRepository;
import com.ninja.repository.LfvFoodRepository;


/**
 * Service class for Diet Planning (LFV and LCHF) with MCP tools Provides both
 * programmatic access and MCP tool capabilities for AI assistants
 */
@Service
public class LfvAndLchfBasedDietService {

	private final LfvFoodRepository lfvFoodRepository;
	private final LchfFoodRepository lchfFoodRepository;

	@Autowired
    public LfvAndLchfBasedDietService(LfvFoodRepository lfvFoodRepository, LchfFoodRepository lchfFoodRepository) {
        this.lfvFoodRepository = lfvFoodRepository;
        this.lchfFoodRepository = lchfFoodRepository;
    }

	// ============ LFV DIET MCP TOOLS ============

	/**
	 * MCP Tool: Search LFV foods by name
	 */
	@Tool(description = "Search for Low Fat Vegetarian (LFV) diet foods by name")
	public List<LfvFood> searchLfvFoodsByName(@ToolParam(description = "food name to search for") String searchTerm) {
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			throw new IllegalArgumentException("Search term cannot be empty");
		}
		return lfvFoodRepository.findByNameContainingIgnoreCase(searchTerm.trim());
	}

	/**
	 * MCP Tool: Get LFV foods by category
	 */
	@Tool(description = "Get LFV foods by category (e.g., Whole Grain, Processed Grain)")
	public List<LfvFood> getLfvFoodsByCategory(@ToolParam(description = "food category") String category) {
		if (category == null || category.trim().isEmpty()) {
			throw new IllegalArgumentException("Category cannot be empty");
		}
		return lfvFoodRepository.findByCategoryIgnoreCase(category.trim());
	}

	/**
	 * MCP Tool: Get LFV foods by limitation status
	 */
	@Tool(description = "Get LFV foods by limitation status (OK, Moderation, Restricted, Limited)")
	public List<LfvFood> getLfvFoodsByLimitation(
			@ToolParam(description = "limitation status: OK, Moderation, Restricted, Limited") String limitation) {
		if (limitation == null || limitation.trim().isEmpty()) {
			throw new IllegalArgumentException("Limitation cannot be empty");
		}
		return lfvFoodRepository.findByLimitationIgnoreCase(limitation.trim());
	}

	/**
	 * MCP Tool: Get allowed LFV foods
	 */
	@Tool(description = "Get all allowed foods for LFV diet (OK and Moderation)")
	public List<LfvFood> getAllowedLfvFoods() {
		return lfvFoodRepository.findAllowedFoods();
	}

	/**
	 * MCP Tool: Get restricted LFV foods
	 */
	@Tool(description = "Get all restricted foods for LFV diet (Restricted and Limited)")
	public List<LfvFood> getRestrictedLfvFoods() {
		return lfvFoodRepository.findRestrictedFoods();
	}

	/**
	 * MCP Tool: Get LFV food categories
	 */
	@Tool(description = "Get all available LFV food categories")
	public List<String> getLfvFoodCategories() {
		return lfvFoodRepository.findDistinctCategories();
	}

	// ============ LCHF DIET MCP TOOLS ============

	/**
	 * MCP Tool: Search LCHF foods by name
	 */
	@Tool(description = "Search for Low Carb High Fat (LCHF) diet foods by name")
	public List<LchfFood> searchLchfFoodsByName(@ToolParam(description = "food name to search for") String searchTerm) {
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			throw new IllegalArgumentException("Search term cannot be empty");
		}
		return lchfFoodRepository.findByNameContainingIgnoreCase(searchTerm.trim());
	}
	
	/**
	 * MCP Tool: Search both food and lchf table and exclude the allergies
	 */
	
//	@Tool(description="Find Foods Suitable for LCHF Diet Excluding Allergies")
//	public List<String> findFoodsForLCHFExcludingAllergies(@ToolParam (description="comma seperated allergies") String allergens)
//	{
//
//	    // 1. Get the filtered food list
//		System.out.println("Allergies" + allergens);
//	    List<String> foods = lchfFoodRepository.findFoodsforLChfExcludingAllergens(allergens);
//	    System.out.println(foods.toString());
//	    return foods;
//	}	
//	
	@Tool(description="Find Foods Suitable for LCHF Diet Excluding Allergies")
	public List<FoodLchfView> findFoodsForLCHFExcludingAllergies(@ToolParam (description="comma seperated allergies") String allergens)
	{

	    // 1. Get the filtered food list
		System.out.println("Allergies" + allergens);
	    List<FoodLchfView> lchfFoods = lchfFoodRepository.findFoodsforLChfExcludingAllergens(allergens);	  
	    return lchfFoods;
	}	
	
	/**
	 * MCP Tool: Get LCHF foods by category
	 */
	@Tool(description = "Get LCHF foods by category (e.g., Fermented Dairy, Cheese, Seafood, Meat)")
	public List<LchfFood> getLchfFoodsByCategory(@ToolParam(description = "food category") String category) {
		if (category == null || category.trim().isEmpty()) {
			throw new IllegalArgumentException("Category cannot be empty");
		}
		return lchfFoodRepository.findByCategoryIgnoreCase(category.trim());
	}

	/**
	 * MCP Tool: Get LCHF foods by limitation status
	 */
	@Tool(description = "Get LCHF foods by limitation status (OK, Restricted, Limit, Avoid, Limited, Recommended)")
	public List<LchfFood> getLchfFoodsByLimitation(
			@ToolParam(description = "limitation status: OK, Restricted, Limit, Avoid, Limited, Recommended") String limitation) {
		if (limitation == null || limitation.trim().isEmpty()) {
			throw new IllegalArgumentException("Limitation cannot be empty");
		}
		return lchfFoodRepository.findByLimitationIgnoreCase(limitation.trim());
	}

	/**
	 * MCP Tool: Get allowed LCHF foods
	 */
	@Tool(description = "Get all allowed foods for LCHF diet (OK and Recommended)")
	public List<LchfFood> getAllowedLchfFoods() {
		return lchfFoodRepository.findAllowedFoods();
	}

	/**
	 * MCP Tool: Get recommended LCHF foods
	 */
	@Tool(description = "Get all recommended foods for LCHF diet")
	public List<LchfFood> getRecommendedLchfFoods() {
		return lchfFoodRepository.findRecommendedFoods();
	}

	/**
	 * MCP Tool: Get restricted LCHF foods
	 */
	@Tool(description = "Get all restricted foods for LCHF diet (Restricted, Avoid, Limited)")
	public List<LchfFood> getRestrictedLchfFoods() {
		return lchfFoodRepository.findRestrictedFoods();
	}

	/**
	 * MCP Tool: Get foods to avoid on LCHF diet
	 */
	@Tool(description = "Get all foods to avoid on LCHF diet")
	public List<LchfFood> getFoodsToAvoidLchf() {
		return lchfFoodRepository.findFoodsToAvoid();
	}

	/**
	 * MCP Tool: Get LCHF food categories
	 */
	@Tool(description = "Get all available LCHF food categories")
	public List<String> getLchfFoodCategories() {
		return lchfFoodRepository.findDistinctCategories();
	}

	/**
	 * MCP Tool: Advanced LFV food search
	 */
	@Tool(description = "Advanced search for LFV foods by multiple criteria")
	public List<LfvFood> searchLfvFoodsAdvanced(@ToolParam(description = "food name (optional)") String name,
			@ToolParam(description = "food category (optional)") String category,
			@ToolParam(description = "limitation status (optional)") String limitation) {
		return lfvFoodRepository.findByMultipleCriteria(name, category, limitation);
	}

	/**
	 * MCP Tool: Advanced LCHF food search
	 */
	@Tool(description = "Advanced search for LCHF foods by multiple criteria")
	public List<LchfFood> searchLchfFoodsAdvanced(@ToolParam(description = "food name (optional)") String name,
			@ToolParam(description = "food category (optional)") String category,
			@ToolParam(description = "limitation status (optional)") String limitation) {
		return lchfFoodRepository.findByMultipleCriteria(name, category, limitation);
	}

	// ============ NON-MCP SERVICE METHODS ============

	// LFV Pagination methods
	public Page<LfvFood> searchLfvFoodsPaginated(String searchTerm, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			return lfvFoodRepository.findAll(pageable);
		}
		return lfvFoodRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
	}

	public Page<LfvFood> getLfvFoodsByCategoryPaginated(String category, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		return lfvFoodRepository.findByCategoryIgnoreCase(category, pageable);
	}

	public Page<LfvFood> getLfvFoodsByLimitationPaginated(String limitation, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		return lfvFoodRepository.findByLimitationIgnoreCase(limitation, pageable);
	}

	// LCHF Pagination methods
	public Page<LchfFood> searchLchfFoodsPaginated(String searchTerm, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			return lchfFoodRepository.findAll(pageable);
		}
		return lchfFoodRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
	}

	public Page<LchfFood> getLchfFoodsByCategoryPaginated(String category, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		return lchfFoodRepository.findByCategoryIgnoreCase(category, pageable);
	}

	public Page<LchfFood> getLchfFoodsByLimitationPaginated(String limitation, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		return lchfFoodRepository.findByLimitationIgnoreCase(limitation, pageable);
	}

	// Get by ID methods
	public Optional<LfvFood> getLfvFoodById(Long id) {
		return lfvFoodRepository.findById(id);
	}

	public Optional<LchfFood> getLchfFoodById(Long id) {
		return lchfFoodRepository.findById(id);
	}

	// Count methods
	public Long countLfvFoodsByCategory(String category) {
		return lfvFoodRepository.countByCategory(category);
	}

	public Long countLfvFoodsByLimitation(String limitation) {
		return lfvFoodRepository.countByLimitation(limitation);
	}

	public Long countLchfFoodsByCategory(String category) {
		return lchfFoodRepository.countByCategory(category);
	}

	public Long countLchfFoodsByLimitation(String limitation) {
		return lchfFoodRepository.countByLimitation(limitation);
	}

	// Get distinct values
	public List<String> getLfvLimitations() {
		return lfvFoodRepository.findDistinctLimitations();
	}

	public List<String> getLchfLimitations() {
		return lchfFoodRepository.findDistinctLimitations();
	}
}