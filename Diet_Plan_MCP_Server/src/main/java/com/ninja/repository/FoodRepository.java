package com.ninja.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ninja.entity.Food;


/**
 * Repository interface for Food entity.
 * Provides data access methods for the foods table.
 */
@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    /**
     * Find foods by name containing the search term (case-insensitive)
     */
    @Query("SELECT f FROM Food f WHERE LOWER(f.foodName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Food> findByFoodNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Find foods by name containing the search term with pagination
     */
    @Query("SELECT f FROM Food f WHERE LOWER(f.foodName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Food> findByFoodNameContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find foods by category
     */
    List<Food> findByFoodCategoryIgnoreCase(String category);

    /**
     * Find foods by category with pagination
     */
    Page<Food> findByFoodCategoryIgnoreCase(String category, Pageable pageable);

    /**
     * Find foods by data type
     */
    List<Food> findByDataTypeIgnoreCase(String dataType);

    /**
     * Get all distinct food categories
     */
    @Query("SELECT DISTINCT f.foodCategory FROM Food f WHERE f.foodCategory IS NOT NULL ORDER BY f.foodCategory")
    List<String> findDistinctFoodCategories();

    /**
     * Get all distinct data types
     */
    @Query("SELECT DISTINCT f.dataType FROM Food f WHERE f.dataType IS NOT NULL ORDER BY f.dataType")
    List<String> findDistinctDataTypes();

    /**
     * Find foods by category and name search
     */
    @Query("SELECT f FROM Food f WHERE " +
           "(:category IS NULL OR LOWER(f.foodCategory) = LOWER(:category)) AND " +
           "(:searchTerm IS NULL OR LOWER(f.foodName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Food> findByCategoryAndNameSearch(@Param("category") String category, 
                                         @Param("searchTerm") String searchTerm);

    /**
     * Find foods by category and name search with pagination
     */
    @Query("SELECT f FROM Food f WHERE " +
           "(:category IS NULL OR LOWER(f.foodCategory) = LOWER(:category)) AND " +
           "(:searchTerm IS NULL OR LOWER(f.foodName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Food> findByCategoryAndNameSearch(@Param("category") String category, 
                                         @Param("searchTerm") String searchTerm, 
                                         Pageable pageable);

    /**
     * Count foods by category
     */
    @Query("SELECT COUNT(f) FROM Food f WHERE LOWER(f.foodCategory) = LOWER(:category)")
    Long countByFoodCategory(@Param("category") String category);

    /**
     * Find foods that contain any allergen flags
     */
    @Query("SELECT f FROM Food f WHERE f.allergenFlags IS NOT NULL AND f.allergenFlags != '' AND f.allergenFlags != 'NaN'")
    List<Food> findFoodsWithAllergens();

    /**
     * Find foods without allergen flags
     */
    @Query("SELECT f FROM Food f WHERE f.allergenFlags IS NULL OR f.allergenFlags = '' OR f.allergenFlags = 'NaN'")
    List<Food> findFoodsWithoutAllergens();
}