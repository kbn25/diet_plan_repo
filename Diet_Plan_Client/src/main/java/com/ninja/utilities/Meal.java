package com.ninja.utilities;

public class Meal {
    public String preMealName;
    public String preMealTime;
    public int preMealCalories;
    public String mainMealName;
    public String mainMealPortionSize;
    public String mainMealTime;
    public int mainMealCalories;
    public int totalCalories;
    public Nutrients mainMealNutrients;
    public int carbs;
    public int protein;
    public int fat;
    public int fiber;

    public Meal(String preMealName, String preMealTime, int preMealCalories, String mainMealName,
               String mainMealPortionSize, String mainMealTime, int mainMealCalories,
               int totalCalories, Nutrients mainMealNutrients, int carbs, int protein, int fat, int fiber) {
        this.preMealName = preMealName;
        this.preMealTime = preMealTime;
        this.preMealCalories = preMealCalories;
        this.mainMealName = mainMealName;
        this.mainMealPortionSize = mainMealPortionSize;
        this.mainMealTime = mainMealTime;
        this.mainMealCalories = mainMealCalories;
        this.totalCalories = totalCalories;
        this.mainMealNutrients = mainMealNutrients;
        this.carbs = carbs;
        this.protein = protein;
        this.fat = fat;
        this.fiber = fiber;
    }
}