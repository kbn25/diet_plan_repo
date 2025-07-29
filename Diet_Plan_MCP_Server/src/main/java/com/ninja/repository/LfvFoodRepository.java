package com.ninja.repository;

import org.postgresql.util.PSQLException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ninja.entity.LfvFood;

import java.util.List;

/**
 * Repository interface for LFV (Low Fat Vegetarian) diet food operations
 */
@Repository
public interface LfvFoodRepository extends JpaRepository<LfvFood, Long> {

    // Search by food name
    List<LfvFood> findByNameContainingIgnoreCase(String name);
    Page<LfvFood> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Find by category
    List<LfvFood> findByCategoryIgnoreCase(String category);
    Page<LfvFood> findByCategoryIgnoreCase(String category, Pageable pageable);

    // Find by limitation status
    List<LfvFood> findByLimitationIgnoreCase(String limitation);
    Page<LfvFood> findByLimitationIgnoreCase(String limitation, Pageable pageable);

    // Find allowed foods (OK and Moderation)
    @Query("SELECT l FROM LfvFood l WHERE UPPER(l.limitation) IN ('OK', 'MODERATION')")
    List<LfvFood> findAllowedFoods() throws PSQLException;

    // Find restricted foods
    @Query("SELECT l FROM LfvFood l WHERE UPPER(l.limitation) IN ('Restricted', 'Limited')")
    List<LfvFood> findRestrictedFoods();

    // Find by category and limitation
    List<LfvFood> findByCategoryIgnoreCaseAndLimitationIgnoreCase(String category, String limitation);

    // Get all distinct categories
    @Query("SELECT DISTINCT l.category FROM LfvFood l ORDER BY l.category")
    List<String> findDistinctCategories();

    // Get all distinct limitations
    @Query("SELECT DISTINCT l.limitation FROM LfvFood l ORDER BY l.limitation")
    List<String> findDistinctLimitations();

    // Count by category
    Long countByCategory(String category);

    // Count by limitation
    Long countByLimitation(String limitation);

    // Advanced search - by name and category
    @Query("SELECT l FROM LfvFood l WHERE " +
           "(:name IS NULL OR UPPER(l.name) LIKE UPPER(CONCAT('%', :name, '%'))) AND " +
           "(:category IS NULL OR UPPER(l.category) = UPPER(:category)) AND " +
           "(:limitation IS NULL OR UPPER(l.limitation) = UPPER(:limitation))")
    List<LfvFood> findByMultipleCriteria(@Param("name") String name, 
                                        @Param("category") String category, 
                                        @Param("limitation") String limitation);

    @Query("SELECT l FROM LfvFood l WHERE " +
           "(:name IS NULL OR UPPER(l.name) LIKE UPPER(CONCAT('%', :name, '%'))) AND " +
           "(:category IS NULL OR UPPER(l.category) = UPPER(:category)) AND " +
           "(:limitation IS NULL OR UPPER(l.limitation) = UPPER(:limitation))")
    Page<LfvFood> findByMultipleCriteria(@Param("name") String name, 
                                        @Param("category") String category, 
                                        @Param("limitation") String limitation, 
                                        Pageable pageable);
}