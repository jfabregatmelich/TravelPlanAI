package com.travelplan.travelplanai.model;

import java.util.List;

public class TravelPlan {
    
    private String destination;
    private int totalDays;
    private int totalBudget;
    private Integer budgetPerDay;  // Nuevo campo para presupuesto por día
    private List<DailyPlan> itinerary;
    private String description;  // Campo opcional para descripción general
    private String summary;      // Campo opcional para resumen del viaje

    public TravelPlan() {}

    public TravelPlan(String destination, int totalDays, int totalBudget, List<DailyPlan> itinerary) {
        this.destination = destination;
        this.totalDays = totalDays;
        this.totalBudget = totalBudget;
        this.itinerary = itinerary;
        if (totalDays > 0) {
            this.budgetPerDay = totalBudget / totalDays;
        }
    }
    
    public TravelPlan(String destination, int totalDays, int totalBudget, Integer budgetPerDay, List<DailyPlan> itinerary) {
        this.destination = destination;
        this.totalDays = totalDays;
        this.totalBudget = totalBudget;
        this.budgetPerDay = budgetPerDay;
        this.itinerary = itinerary;
    }

    // Getters y Setters
    public String getDestination() { 
        return destination; 
    }
    
    public void setDestination(String destination) { 
        this.destination = destination; 
    }
    
    public int getTotalDays() { 
        return totalDays; 
    }
    
    public void setTotalDays(int totalDays) { 
        this.totalDays = totalDays; 
    }
    
    public int getTotalBudget() { 
        return totalBudget; 
    }
    
    public void setTotalBudget(int totalBudget) { 
        this.totalBudget = totalBudget; 
    }
    
    public Integer getBudgetPerDay() { 
        return budgetPerDay; 
    }
    
    
    
    public List<DailyPlan> getItinerary() { 
        return itinerary; 
    }
    
    public void setItinerary(List<DailyPlan> itinerary) { 
        this.itinerary = itinerary; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    public String getSummary() { 
        return summary; 
    }
    
    public void setSummary(String summary) { 
        this.summary = summary; 
    }
    
    // Método útil para obtener presupuesto por día de forma segura
    public int getEffectiveBudgetPerDay() {
        if (budgetPerDay != null && budgetPerDay > 0) {
            return budgetPerDay;
        }
        return totalDays > 0 ? totalBudget / totalDays : 0;
    }
    
    // Método para calcular presupuesto total si solo tenemos presupuesto por día
    public void calculateTotalBudgetFromPerDay() {
        if (budgetPerDay != null && budgetPerDay > 0 && totalDays > 0) {
            this.totalBudget = budgetPerDay * totalDays;
        }
    }
    
    @Override
    public String toString() {
        return "TravelPlan{" +
                "destination='" + destination + '\'' +
                ", totalDays=" + totalDays +
                ", totalBudget=" + totalBudget +
                ", budgetPerDay=" + budgetPerDay +
                ", itinerary=" + (itinerary != null ? itinerary.size() + " days" : "null") +
                '}';
    }

    public void setBudgetPerDay(int budgetPerDay) {
        this.budgetPerDay = budgetPerDay; 
    }
}