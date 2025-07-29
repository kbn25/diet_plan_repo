package com.ninja.repository;

import org.postgresql.util.PSQLException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ninja.entity.LchfFood;

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
    List<LchfFood> findAllowedFoods() throws PSQLException;

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
}