package com.travelplan.travelplanai.model;

import java.util.List;

public class TravelPlan {
    
    private String destination;
    private int totalDays;
    private int totalBudget;
    private List<DailyPlan> itinerary;

    public TravelPlan() {}

    public TravelPlan(String destination, int totalDays, int totalBudget, List<DailyPlan> itinerary) {
        this.destination = destination;
        this.totalDays = totalDays;
        this.totalBudget = totalBudget;
        this.itinerary = itinerary;
    }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public int getTotalBudget() { return totalBudget; }
    public void setTotalBudget(int totalBudget) { this.totalBudget = totalBudget; }
    public List<DailyPlan> getItinerary() { return itinerary; }
    public void setItinerary(List<DailyPlan> itinerary) { this.itinerary = itinerary; }
}