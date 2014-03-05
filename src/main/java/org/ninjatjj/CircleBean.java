package org.ninjatjj;

public class CircleBean {

    private String circleName;
    private String circleList;

    private String schedule;

    public CircleBean(String circleName, String circleList, String schedule) {
        this.circleName = circleName;
        this.circleList = circleList;
        this.schedule = schedule;
    }

    public String getCircleName() {
        return circleName;
    }

    public String getCircleList() {
        return circleList;
    }

    public String getSchedule() {
        return schedule;
    }

}
