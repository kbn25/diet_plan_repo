package com.ninja.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ninja.entity.Food;
import com.ninja.projection.FoodLchfView;

@Repository
public interface FoodAndLchfRepository extends JpaRepository<Food, Long> {

	  @Query(value = """
	SELECT  fdc_id ,
    score  ,
    food_name ,
    lchf_name ,
    food_category   ,
    lchf_category  ,
    limitation  ,
    veg_nonveg ,
    energy_kcal  ,
    total_fat_g  ,
    protein_g  ,
    carbohydrate_g  ,
    fiber_g  ,
    sugars_g  ,
    added_sugars_g  ,
    sodium_mg  ,
    potassium_mg  ,
    calcium_mg  ,
    iron_mg  ,
    vitamin_c_mg  ,
    cholesterol_mg  ,
    saturated_fat_g  ,
    vitamin_d_mcg  ,
    magnesium_mg  ,
    allergen_flags text ,
    notes 
	    	    FROM lchf_foods f	    	 
	    	    WHERE LOWER(f.limitation) IN ('ok', 'limited')
	    	      AND NOT EXISTS (
	    	        SELECT 1
	    	        FROM unnest(string_to_array(:allergens, ',')) AS a(allergen_item)
	    	        WHERE LOWER(f.allergen_flags) LIKE CONCAT('%', LOWER(TRIM(a.allergen_item)), '%')
	    	      	)
	    	    """, nativeQuery = true) 
    List<FoodLchfView> findAllSuitableLchfFoodsExcludingAllergies(@Param("allergens") String allergens);
}
