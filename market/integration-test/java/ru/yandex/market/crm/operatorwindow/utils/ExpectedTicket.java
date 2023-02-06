package ru.yandex.market.crm.operatorwindow.utils;

import java.util.Set;

public class ExpectedTicket {

    private String title;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String serviceCode;
    private Long orderNumber;
    private Long priority;
    private Set<String> tags;
    private Set<String> categories;
    private String comment;

    public String getTitle() {
        return title;
    }

    public ExpectedTicket setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getClientName() {
        return clientName;
    }

    public ExpectedTicket setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public ExpectedTicket setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
        return this;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public ExpectedTicket setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
        return this;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public ExpectedTicket setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
        return this;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public ExpectedTicket setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    public Long getPriority() {
        return priority;
    }

    public ExpectedTicket setPriority(Long priority) {
        this.priority = priority;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public ExpectedTicket setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public ExpectedTicket setCategories(Set<String> categories) {
        this.categories = categories;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ExpectedTicket setComment(String comment) {
        this.comment = comment;
        return this;
    }
}
