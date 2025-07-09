package com.ninja.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ninja.entity.Nutrient;


/**
 * Repository interface for Nutrient entity. Provides data access methods for
 * the nutrients table.
 */
@Repository
public interface NutrientRepository extends JpaRepository<Nutrient, Long> {

	/**
	 * Find nutrients by food name containing the search term (case-insensitive)
	 */
	@Query("SELECT n FROM Nutrient n WHERE LOWER(n.foodName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
	List<Nutrient> findByFoodNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

	/**
	 * Find nutrients by food name containing the search term with pagination
	 */
	@Query("SELECT n FROM Nutrient n WHERE LOWER(n.foodName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
	Page<Nutrient> findByFoodNameContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

	/**
	 * Find nutrients by simplified name containing the search term
	 */
	@Query("SELECT n FROM Nutrient n WHERE LOWER(n.simplifiedName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
	List<Nutrient> findBySimplifiedNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

	/**
	 * Find high protein foods (protein > specified amount)
	 */
	@Query("SELECT n FROM Nutrient n WHERE n.proteinG > :minProtein ORDER BY n.proteinG DESC")
	List<Nutrient> findHighProteinFoods(@Param("minProtein") BigDecimal minProtein);

	/**
	 * Find low calorie foods (calories < specified amount)
	 */
	@Query("SELECT n FROM Nutrient n WHERE n.energyKcal < :maxCalories ORDER BY n.energyKcal ASC")
	List<Nutrient> findLowCalorieFoods(@Param("maxCalories") BigDecimal maxCalories);

	/**
	 * Find foods with high fiber content
	 */
	@Query("SELECT n FROM Nutrient n WHERE n.fiberG > :minFiber ORDER BY n.fiberG DESC")
	List<Nutrient> findHighFiberFoods(@Param("minFiber") BigDecimal minFiber);

	/**
	 * Find foods with low sodium content
	 */
	@Query("SELECT n FROM Nutrient n WHERE n.sodiumMg < :maxSodium ORDER BY n.sodiumMg ASC")
	List<Nutrient> findLowSodiumFoods(@Param("maxSodium") BigDecimal maxSodium);

	/**
	 * Find foods rich in specific vitamin/mineral
	 */
	@Query("SELECT n FROM Nutrient n WHERE " + "(:vitaminType = 'C' AND n.vitaminCMg > :minAmount) OR "
			+ "(:vitaminType = 'D' AND n.vitaminDMcg > :minAmount) OR "
			+ "(:vitaminType = 'CALCIUM' AND n.calciumMg > :minAmount) OR "
			+ "(:vitaminType = 'IRON' AND n.ironMg > :minAmount) OR "
			+ "(:vitaminType = 'POTASSIUM' AND n.potassiumMg > :minAmount) OR "
			+ "(:vitaminType = 'MAGNESIUM' AND n.magnesiumMg > :minAmount)")
	List<Nutrient> findFoodsRichInVitaminMineral(@Param("vitaminType") String vitaminType,
			@Param("minAmount") BigDecimal minAmount);

	/**
	 * Find foods within calorie range
	 */
	@Query("SELECT n FROM Nutrient n WHERE n.energyKcal BETWEEN :minCalories AND :maxCalories")
	List<Nutrient> findFoodsInCalorieRange(@Param("minCalories") BigDecimal minCalories,
			@Param("maxCalories") BigDecimal maxCalories);

	/**
	 * Find foods within calorie range with pagination
	 */
	@Query("SELECT n FROM Nutrient n WHERE n.energyKcal BETWEEN :minCalories AND :maxCalories")
	Page<Nutrient> findFoodsInCalorieRange(@Param("minCalories") BigDecimal minCalories,
			@Param("maxCalories") BigDecimal maxCalories, Pageable pageable);

	/**
	 * Find foods with balanced macronutrients Balanced means: Protein 10-35%, Fat
	 * 20-35%, Carbs 45-65% of total calories
	 */
	@Query("SELECT n FROM Nutrient n WHERE " + "n.energyKcal > 0 AND "
			+ "((n.proteinG * 4) / n.energyKcal) BETWEEN 0.10 AND 0.35 AND "
			+ "((n.totalFatG * 9) / n.energyKcal) BETWEEN 0.20 AND 0.35 AND "
			+ "((n.carbohydrateG * 4) / n.energyKcal) BETWEEN 0.45 AND 0.65")
	List<Nutrient> findBalancedMacronutrientFoods();

	/**
	 * Search foods by name, simplified name, or synonyms
	 */
	@Query("SELECT n FROM Nutrient n WHERE " + "LOWER(n.foodName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(n.simplifiedName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(n.synonyms) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
	List<Nutrient> searchFoodsByAllNames(@Param("searchTerm") String searchTerm);

	/**
	 * Get nutritional summary statistics
	 */
	@Query("SELECT " + "AVG(n.energyKcal) as avgCalories, " + "AVG(n.proteinG) as avgProtein, "
			+ "AVG(n.totalFatG) as avgFat, " + "AVG(n.carbohydrateG) as avgCarbs "
			+ "FROM Nutrient n WHERE n.energyKcal IS NOT NULL")
	Object[] getNutritionalStatistics();

	/**
	 * Find foods suitable for specific dietary restrictions
	 */
	@Query("SELECT n FROM Nutrient n WHERE " + "(:lowSodium = false OR n.sodiumMg < 140) AND "
			+ "(:lowFat = false OR n.totalFatG < 3) AND " + "(:highFiber = false OR n.fiberG > 3) AND "
			+ "(:lowSugar = false OR n.sugarsG < 5)")
	List<Nutrient> findFoodsForDietaryRestrictions(@Param("lowSodium") boolean lowSodium,
			@Param("lowFat") boolean lowFat, @Param("highFiber") boolean highFiber,
			@Param("lowSugar") boolean lowSugar);
	
//	@Query("SELECT n FROM Nutrient n WHERE "
//		     + "(:lowSodium = false OR n.sodiumMg < :sodiumThreshold) AND "
//		     + "(:lowFat = false OR n.totalFatG < :fatThreshold) AND "
//		     + "(:highFiber = false OR n.fiberG > :fiberThreshold) AND "
//		     + "(:lowSugar = false OR n.sugarsG < :sugarThreshold)")
//		List<Nutrient> findFoodsForDietaryRestrictions(
//		     @Param("lowSodium") boolean lowSodium,
//		     @Param("lowFat") boolean lowFat,
//		     @Param("highFiber") boolean highFiber,
//		     @Param("lowSugar") boolean lowSugar,
//		     @Param("sodiumThreshold") double sodiumThreshold,
//		     @Param("fatThreshold") double fatThreshold,
//		     @Param("fiberThreshold") double fiberThreshold,
//		     @Param("sugarThreshold") double sugarThreshold
//		);

}