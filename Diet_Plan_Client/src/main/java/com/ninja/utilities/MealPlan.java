package com.ninja.utilities;

public class MealPlan {
    public Meal breakfast;
    public Meal lunch;
    public Meal dinner;
    public Meal snacks;

    public MealPlan(Meal breakfast, Meal lunch, Meal dinner, Meal snacks) {
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
        this.snacks = snacks;
    }
}