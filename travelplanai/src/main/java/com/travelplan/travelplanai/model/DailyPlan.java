package com.travelplan.travelplanai.model;

public class DailyPlan {
    
    private int day;
    private String morning;
    private String lunch;
    private String afternoon;
    private Integer morningPrice;
    private Integer lunchPrice;
    private Integer afternoonPrice;

    public DailyPlan() {}

    public DailyPlan(int day, String morning, String lunch, String afternoon) {
        this.day = day;
        this.morning = morning;
        this.lunch = lunch;
        this.afternoon = afternoon;
    }
    
    public DailyPlan(int day, String morning, String lunch, String afternoon, 
                     Integer morningPrice, Integer lunchPrice, Integer afternoonPrice) {
        this.day = day;
        this.morning = morning;
        this.lunch = lunch;
        this.afternoon = afternoon;
        this.morningPrice = morningPrice;
        this.lunchPrice = lunchPrice;
        this.afternoonPrice = afternoonPrice;
    }

    // Getters y Setters
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    
    public String getMorning() { return morning; }
    public void setMorning(String morning) { this.morning = morning; }
    
    public String getLunch() { return lunch; }
    public void setLunch(String lunch) { this.lunch = lunch; }
    
    public String getAfternoon() { return afternoon; }
    public void setAfternoon(String afternoon) { this.afternoon = afternoon; }
    
    public Integer getMorningPrice() { return morningPrice; }
    public void setMorningPrice(Integer morningPrice) { this.morningPrice = morningPrice; }
    
    public Integer getLunchPrice() { return lunchPrice; }
    public void setLunchPrice(Integer lunchPrice) { this.lunchPrice = lunchPrice; }
    
    public Integer getAfternoonPrice() { return afternoonPrice; }
    public void setAfternoonPrice(Integer afternoonPrice) { this.afternoonPrice = afternoonPrice; }
    
    // Método para calcular el total del día
    public int getDayTotal() {
        return (morningPrice != null ? morningPrice : 0) +
               (lunchPrice != null ? lunchPrice : 0) +
               (afternoonPrice != null ? afternoonPrice : 0);
    }
    public void setDayTotal(int dayTotal) {
    // Este método existe por compatibilidad con el PDF controller
    // El cálculo real se hace en getDayTotal()
    // Si necesitas almacenar un valor específico, puedes guardarlo en un campo
    }
    
}