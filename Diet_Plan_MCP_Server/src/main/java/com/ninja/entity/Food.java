package com.ninja.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing a food item from the foods table. This entity maps to the
 * 'foods' table in PostgreSQL database.
 */
@Entity
@Table(name = "foods")
public class Food {

	/**
	 * Primary key - Food Data Central ID
	 * Using Integer to match database int4 type
	 */
	@Id
	@Column(name = "fdc_id")
	@NotNull
	private Integer fdcId;

	/**
	 * Name of the food item
	 */
	@Column(name = "food_name", nullable = false)
	@NotBlank
	private String foodName;

	/**
	 * Type of data (e.g., "Foundation", "Branded", "Survey")
	 */
	@Column(name = "data_type")
	private String dataType;

	/**
	 * Category of the food (e.g., "Fruits", "Vegetables", "Dairy")
	 */
	@Column(name = "food_category")
	private String foodCategory;

	/**
	 * Date when the food data was published
	 * Database stores as text, so using String to match exactly
	 */
	@Column(name = "publication_date")
	private String publicationDate;

	/**
	 * Allergen information flags
	 */
	@Column(name = "allergen_flags")
	private String allergenFlags;

	// Constructors
	public Food() {
	}

	public Food(Integer fdcId, String foodName, String dataType, String foodCategory, String publicationDate,
			String allergenFlags) {
		this.fdcId = fdcId;
		this.foodName = foodName;
		this.dataType = dataType;
		this.foodCategory = foodCategory;
		this.publicationDate = publicationDate;
		this.allergenFlags = allergenFlags;
	}

	// Getters and Setters
	public Integer getFdcId() {
		return fdcId;
	}

	public void setFdcId(Integer fdcId) {
		this.fdcId = fdcId;
	}

	public String getFoodName() {
		return foodName;
	}

	public void setFoodName(String foodName) {
		this.foodName = foodName;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getFoodCategory() {
		return foodCategory;
	}

	public void setFoodCategory(String foodCategory) {
		this.foodCategory = foodCategory;
	}

	public String getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getAllergenFlags() {
		return allergenFlags;
	}

	public void setAllergenFlags(String allergenFlags) {
		this.allergenFlags = allergenFlags;
	}

	@Override
	public String toString() {
		return "Food{" + "fdcId=" + fdcId + ", foodName='" + foodName + '\'' + ", dataType='" + dataType + '\''
				+ ", foodCategory='" + foodCategory + '\'' + ", publicationDate=" + publicationDate
				+ ", allergenFlags='" + allergenFlags + '\'' + '}';
	}
}