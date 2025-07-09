package com.ninja.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing nutritional information from the nutrients table. This
 * entity maps to the 'nutrients' table in PostgreSQL database.
 */
@Entity
@Table(name = "nutrients")
public class Nutrient {

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
	 * Simplified name for easier searching
	 */
	@Column(name = "simplified_name")
	private String simplifiedName;

	/**
	 * Alternative names/synonyms for the food
	 */
	@Column(name = "synonyms")
	private String synonyms;

	// Macronutrients - Changed from BigDecimal to Double to match database double precision
	/**
	 * Energy content in kilocalories
	 */
	@Column(name = "energy_kcal")
	private Double energyKcal;

	/**
	 * Total fat content in grams
	 */
	@Column(name = "total_fat_g")
	private Double totalFatG;

	/**
	 * Protein content in grams
	 */
	@Column(name = "protein_g")
	private Double proteinG;

	/**
	 * Carbohydrate content in grams
	 */
	@Column(name = "carbohydrate_g")
	private Double carbohydrateG;

	/**
	 * Fiber content in grams
	 */
	@Column(name = "fiber_g")
	private Double fiberG;

	/**
	 * Total sugars in grams
	 */
	@Column(name = "sugars_g")
	private Double sugarsG;

	/**
	 * Added sugars in grams
	 */
	@Column(name = "added_sugars_g")
	private Double addedSugarsG;

	// Micronutrients - Changed from BigDecimal to Double to match database double precision
	/**
	 * Sodium content in milligrams
	 */
	@Column(name = "sodium_mg")
	private Double sodiumMg;

	/**
	 * Potassium content in milligrams
	 */
	@Column(name = "potassium_mg")
	private Double potassiumMg;

	/**
	 * Calcium content in milligrams
	 */
	@Column(name = "calcium_mg")
	private Double calciumMg;

	/**
	 * Iron content in milligrams
	 */
	@Column(name = "iron_mg")
	private Double ironMg;

	/**
	 * Vitamin C content in milligrams
	 */
	@Column(name = "vitamin_c_mg")
	private Double vitaminCMg;

	/**
	 * Cholesterol content in milligrams
	 */
	@Column(name = "cholesterol_mg")
	private Double cholesterolMg;

	/**
	 * Saturated fat content in grams
	 */
	@Column(name = "saturated_fat_g")
	private Double saturatedFatG;

	/**
	 * Vitamin D content in micrograms
	 */
	@Column(name = "vitamin_d_mcg")
	private Double vitaminDMcg;

	/**
	 * Magnesium content in milligrams
	 */
	@Column(name = "magnesium_mg")
	private Double magnesiumMg;

	// Constructors
	public Nutrient() {
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

	public String getSimplifiedName() {
		return simplifiedName;
	}

	public void setSimplifiedName(String simplifiedName) {
		this.simplifiedName = simplifiedName;
	}

	public String getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(String synonyms) {
		this.synonyms = synonyms;
	}

	public Double getEnergyKcal() {
		return energyKcal;
	}

	public void setEnergyKcal(Double energyKcal) {
		this.energyKcal = energyKcal;
	}

	public Double getTotalFatG() {
		return totalFatG;
	}

	public void setTotalFatG(Double totalFatG) {
		this.totalFatG = totalFatG;
	}

	public Double getProteinG() {
		return proteinG;
	}

	public void setProteinG(Double proteinG) {
		this.proteinG = proteinG;
	}

	public Double getCarbohydrateG() {
		return carbohydrateG;
	}

	public void setCarbohydrateG(Double carbohydrateG) {
		this.carbohydrateG = carbohydrateG;
	}

	public Double getFiberG() {
		return fiberG;
	}

	public void setFiberG(Double fiberG) {
		this.fiberG = fiberG;
	}

	public Double getSugarsG() {
		return sugarsG;
	}

	public void setSugarsG(Double sugarsG) {
		this.sugarsG = sugarsG;
	}

	public Double getAddedSugarsG() {
		return addedSugarsG;
	}

	public void setAddedSugarsG(Double addedSugarsG) {
		this.addedSugarsG = addedSugarsG;
	}

	public Double getSodiumMg() {
		return sodiumMg;
	}

	public void setSodiumMg(Double sodiumMg) {
		this.sodiumMg = sodiumMg;
	}

	public Double getPotassiumMg() {
		return potassiumMg;
	}

	public void setPotassiumMg(Double potassiumMg) {
		this.potassiumMg = potassiumMg;
	}

	public Double getCalciumMg() {
		return calciumMg;
	}

	public void setCalciumMg(Double calciumMg) {
		this.calciumMg = calciumMg;
	}

	public Double getIronMg() {
		return ironMg;
	}

	public void setIronMg(Double ironMg) {
		this.ironMg = ironMg;
	}

	public Double getVitaminCMg() {
		return vitaminCMg;
	}

	public void setVitaminCMg(Double vitaminCMg) {
		this.vitaminCMg = vitaminCMg;
	}

	public Double getCholesterolMg() {
		return cholesterolMg;
	}

	public void setCholesterolMg(Double cholesterolMg) {
		this.cholesterolMg = cholesterolMg;
	}

	public Double getSaturatedFatG() {
		return saturatedFatG;
	}

	public void setSaturatedFatG(Double saturatedFatG) {
		this.saturatedFatG = saturatedFatG;
	}

	public Double getVitaminDMcg() {
		return vitaminDMcg;
	}

	public void setVitaminDMcg(Double vitaminDMcg) {
		this.vitaminDMcg = vitaminDMcg;
	}

	public Double getMagnesiumMg() {
		return magnesiumMg;
	}

	public void setMagnesiumMg(Double magnesiumMg) {
		this.magnesiumMg = magnesiumMg;
	}

	@Override
	public String toString() {
		return "Nutrient{" + "fdcId=" + fdcId + ", foodName='" + foodName + '\'' + ", energyKcal=" + energyKcal
				+ ", proteinG=" + proteinG + ", totalFatG=" + totalFatG + ", carbohydrateG=" + carbohydrateG + '}';
	}
}