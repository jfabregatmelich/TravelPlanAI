package com.travelplan.travelplanai.model;

import java.util.List;

public class TravelRequest {
    
    private List<String> countries;
    private String city;
    private int days;
    private int budget;

    public TravelRequest() {}

    public TravelRequest(List<String> countries, String city, int days, int budget) {
        this.countries = countries;
        this.city = city;
        this.days = days;
        this.budget = budget;
    }

    public List<String> getCountries() { return countries; }
    public void setCountries(List<String> countries) { this.countries = countries; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
    public int getBudget() { return budget; }
    public void setBudget(int budget) { this.budget = budget; }
}