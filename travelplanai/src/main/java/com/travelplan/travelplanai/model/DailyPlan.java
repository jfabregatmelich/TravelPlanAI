package com.travelplan.travelplanai.model;

public class DailyPlan {
    
    private int day;
    private String morning;
    private String lunch;
    private String afternoon;

    public DailyPlan() {}

    public DailyPlan(int day, String morning, String lunch, String afternoon) {
        this.day = day;
        this.morning = morning;
        this.lunch = lunch;
        this.afternoon = afternoon;
    }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public String getMorning() { return morning; }
    public void setMorning(String morning) { this.morning = morning; }
    public String getLunch() { return lunch; }
    public void setLunch(String lunch) { this.lunch = lunch; }
    public String getAfternoon() { return afternoon; }
    public void setAfternoon(String afternoon) { this.afternoon = afternoon; }
}