package com.travelplan.travelplanai.model;

import java.util.List;

public class TravelRequest {
    
    private List<String> countries;
    private String city;
    private int days;
    private int budget;
    private Integer budgetPerDay;  // Nuevo campo para presupuesto por día

    public TravelRequest() {}

    public TravelRequest(List<String> countries, String city, int days, int budget) {
        this.countries = countries;
        this.city = city;
        this.days = days;
        this.budget = budget;
    }
    
    public TravelRequest(List<String> countries, String city, int days, int budget, Integer budgetPerDay) {
        this.countries = countries;
        this.city = city;
        this.days = days;
        this.budget = budget;
        this.budgetPerDay = budgetPerDay;
    }

    // Getters y Setters
    public List<String> getCountries() { 
        return countries; 
    }
    
    public void setCountries(List<String> countries) { 
        this.countries = countries; 
    }
    
    public String getCity() { 
        return city; 
    }
    
    public void setCity(String city) { 
        this.city = city; 
    }
    
    public int getDays() { 
        return days; 
    }
    
    public void setDays(int days) { 
        this.days = days; 
    }
    
    public int getBudget() { 
        return budget; 
    }
    
    public void setBudget(int budget) { 
        this.budget = budget; 
    }
    
    public Integer getBudgetPerDay() { 
        return budgetPerDay; 
    }
    
    public void setBudgetPerDay(Integer budgetPerDay) { 
        this.budgetPerDay = budgetPerDay; 
    }
    
    // Método útil para calcular presupuesto por día si no está definido
    public int getEffectiveBudgetPerDay() {
        if (budgetPerDay != null && budgetPerDay > 0) {
            return budgetPerDay;
        }
        return days > 0 ? budget / days : 0;
    }
    
    @Override
    public String toString() {
        return "TravelRequest{" +
                "countries=" + countries +
                ", city='" + city + '\'' +
                ", days=" + days +
                ", budget=" + budget +
                ", budgetPerDay=" + budgetPerDay +
                '}';
    }
}