package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalendaringInterval {

    private String serviceId;
    private String day;
    private String from;
    private String to;

    public CalendaringInterval() {
        this.serviceId = serviceId;
        this.day = day;
        this.from = from;
        this.to = to;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

}
