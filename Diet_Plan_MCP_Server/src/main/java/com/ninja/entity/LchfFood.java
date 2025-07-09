package com.ninja.entity;

import jakarta.persistence.*;

/**
 * Entity representing Low Carb High Fat (LCHF) diet food items
 * Contains information about food limitations and categories for LCHF diet planning
 */
@Entity
@Table(name = "lchf_tbl")
public class LchfFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "limitation", nullable = false)
    private String limitation; // OK, Restricted, Limit, Avoid, Limited, Recommended

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public LchfFood() {}

    public LchfFood(String name, String category, String limitation, String notes) {
        this.name = name;
        this.category = category;
        this.limitation = limitation;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLimitation() {
        return limitation;
    }

    public void setLimitation(String limitation) {
        this.limitation = limitation;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "LchfFood{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", limitation='" + limitation + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}