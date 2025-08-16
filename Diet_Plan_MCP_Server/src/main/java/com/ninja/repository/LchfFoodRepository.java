package com.ninja.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ninja.entity.LchfFood;
import com.ninja.projection.FoodLchfView;

import java.util.List;

/**
 * Repository interface for LCHF (Low Carb High Fat) diet food operations
 */
@Repository
public interface LchfFoodRepository extends JpaRepository<LchfFood, Long> {

    // Search by food name
    List<LchfFood> findByNameContainingIgnoreCase(String name);
    Page<LchfFood> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Find by category
    List<LchfFood> findByCategoryIgnoreCase(String category);
    Page<LchfFood> findByCategoryIgnoreCase(String category, Pageable pageable);

    // Find by limitation status
    List<LchfFood> findByLimitationIgnoreCase(String limitation);
    Page<LchfFood> findByLimitationIgnoreCase(String limitation, Pageable pageable);

    // Find allowed foods (OK and Recommended)
    @Query("SELECT l FROM LchfFood l WHERE UPPER(l.limitation) IN ('OK', 'RECOMMENDED')")
    List<LchfFood> findAllowedFoods();

    // Find restricted foods (Restricted, Avoid, Limited)
    @Query("SELECT l FROM LchfFood l WHERE UPPER(l.limitation) IN ('RESTRICTED', 'AVOID', 'LIMITED')")
    List<LchfFood> findRestrictedFoods();

    // Find recommended foods
    @Query("SELECT l FROM LchfFood l WHERE UPPER(l.limitation) = 'RECOMMENDED'")
    List<LchfFood> findRecommendedFoods();

    // Find foods to avoid
    @Query("SELECT l FROM LchfFood l WHERE UPPER(l.limitation) = 'AVOID'")
    List<LchfFood> findFoodsToAvoid();

    // Find by category and limitation
    List<LchfFood> findByCategoryIgnoreCaseAndLimitationIgnoreCase(String category, String limitation);

    // Get all distinct categories
    @Query("SELECT DISTINCT l.category FROM LchfFood l ORDER BY l.category")
    List<String> findDistinctCategories();

    // Get all distinct limitations
    @Query("SELECT DISTINCT l.limitation FROM LchfFood l ORDER BY l.limitation")
    List<String> findDistinctLimitations();

    // Count by category
    Long countByCategory(String category);

    // Count by limitation
    Long countByLimitation(String limitation);

    // Advanced search - by name and category
    @Query("SELECT l FROM LchfFood l WHERE " +
           "(:name IS NULL OR UPPER(l.name) LIKE UPPER(CONCAT('%', :name, '%'))) AND " +
           "(:category IS NULL OR UPPER(l.category) = UPPER(:category)) AND " +
           "(:limitation IS NULL OR UPPER(l.limitation) = UPPER(:limitation))")
    List<LchfFood> findByMultipleCriteria(@Param("name") String name, 
                                         @Param("category") String category, 
                                         @Param("limitation") String limitation);

    @Query("SELECT l FROM LchfFood l WHERE " +
           "(:name IS NULL OR UPPER(l.name) LIKE UPPER(CONCAT('%', :name, '%'))) AND " +
           "(:category IS NULL OR UPPER(l.category) = UPPER(:category)) AND " +
           "(:limitation IS NULL OR UPPER(l.limitation) = UPPER(:limitation))")
    Page<LchfFood> findByMultipleCriteria(@Param("name") String name, 
                                         @Param("category") String category, 
                                         @Param("limitation") String limitation, 
                                         Pageable pageable);   
    
    //Compare the lchf & food table and exclude the allergic foods
   
	  @Query(value = """
	    	   SELECT f.food_name ,   	
	  			energy_kcal , total_fat_g , protein_g , carbohydrate_g ,
	  			fiber_g , sugars_g , added_sugars_g , sodium_mg ,
	  			potassium_mg , calcium_mg , iron_mg , vitamin_c_mg ,
	  			cholesterol_mg , saturated_fat_g , vitamin_d_mcg ,
	  			magnesium_mg 
	  			FROM lchf_foods_tbl f
	  			join nutrients n
	            on n.fdc_id=f.fdc_id
                WHERE LOWER(limitation) IN ('ok', 'limited')
                AND NOT EXISTS (
	  		             SELECT 1
	  		 		     FROM unnest(string_to_array('dairy,egg,soy', ',')) AS a(allergen_item)
	  		 		 	 WHERE LOWER(allergen_flags) LIKE '%' || LOWER(TRIM(a.allergen_item)) || '%'
	  					)
	    	    """, nativeQuery = true) 
	        List<FoodLchfView> findFoodsforLChfExcludingAllergens(@Param("allergens") String allergens);
	  
	  
	  
	  @Query(value = """
	    	    SELECT distinct(food_name) from
	    	    lfv_foods_tbl 
	    	    WHERE LOWER(limitation) IN ('ok', 'limited')
	    	      AND NOT EXISTS (
	    	        SELECT 1
	    	        FROM unnest(string_to_array(:allergens, ',')) AS a(allergen_item)
	    	        WHERE LOWER(allergen_flags) LIKE CONCAT('%', LOWER(TRIM(a.allergen_item)), '%')
	    	      	) 
	    	    """, nativeQuery = true) 
	        List<String> findFoodsforLfvExcludingAllergens(@Param("allergens") String allergens);
}