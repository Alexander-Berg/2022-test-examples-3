package ui_tests.src.test.java.Classes.ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Properties {

    private String contactEmail;
    private String priority;
    private String service;
    private String order;
    private String team;
    private String contactPhoneNumber;
    private String additionalContactPhoneNumber;
    private String status;
    private List<String> category = new ArrayList<>();
    private String deliveryOrder;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Properties actualProperties = (Properties) o;

        if (this.contactEmail != null) {
            if (!this.contactEmail.toLowerCase().equals(actualProperties.contactEmail.toLowerCase())) {
                return false;
            }
        }
        if (this.priority != null) {
            if (!this.priority.equals(actualProperties.priority)) {
                return false;
            }
        }
        if (this.service != null) {
            if (!this.service.equals(actualProperties.service)) {
                return false;
            }
        }
        if (this.order != null) {
            if (!this.order.equals(actualProperties.order)) {
                return false;
            }
        }
        if (this.team != null) {
            if (!this.team.equals(actualProperties.team)) {

                return false;
            }
        }
        if (this.contactPhoneNumber != null) {
            if (!this.contactPhoneNumber.equals(actualProperties.contactPhoneNumber)) {
                return false;
            }
        }
        if (this.status != null) {
            if (!this.status.equals(actualProperties.status)) {
                return false;
            }
        }
        if (this.deliveryOrder != null) {
            if (!this.deliveryOrder.equals(actualProperties.deliveryOrder)) {
                return false;
            }
        }
        if (this.additionalContactPhoneNumber != null) {
            if (!this.additionalContactPhoneNumber.equals(actualProperties.getAdditionalContactPhoneNumber())) {
                return false;
            }
        }
        return category.containsAll(actualProperties.category);

    }

    /**
     * Получить дополнительный номер телефона клиента
     *
     * @return
     */
    public String getAdditionalContactPhoneNumber() {
        return additionalContactPhoneNumber;
    }

    /**
     * Указать дополнительный номер телефона клиента
     *
     * @param additionalContactPhoneNumber
     * @return
     */
    public Properties setAdditionalContactPhoneNumber(String additionalContactPhoneNumber) {
        this.additionalContactPhoneNumber = additionalContactPhoneNumber;
        return this;
    }

    @Override
    public String toString() {
        return "Properties{" +
                "contactEmail='" + contactEmail + '\'' +
                ", priority='" + priority + '\'' +
                ", service='" + service + '\'' +
                ", order='" + order + '\'' +
                ", team='" + team + '\'' +
                ", contactPhoneNumber='" + contactPhoneNumber + '\'' +
                ", AdditionalContactPhoneNumber='" + additionalContactPhoneNumber + '\'' +
                ", status='" + status + '\'' +
                ", category=" + category +
                ", deliveryOrder='" + deliveryOrder + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactEmail, priority, service, order, team, contactPhoneNumber, status, category, deliveryOrder);
    }

    public String getDeliveryOrder() {
        return deliveryOrder;
    }

    public Properties setDeliveryOrder(String deliveryOrder) {
        this.deliveryOrder = deliveryOrder;
        return this;
    }

    public String getPriority() {
        return priority;
    }

    public Properties setPriority(String priority) {
        this.priority = priority;
        return this;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public Properties setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
        return this;
    }

    public String getTeam() {
        return team;
    }

    public Properties setTeam(String team) {
        this.team = team;
        return this;
    }

    public List<String> getCategory() {
        return category;
    }

    public Properties setCategory(List<String> category) {
        this.category = category;
        return this;
    }

    public String getOrder() {
        return order;
    }

    public Properties setOrder(String order) {
        this.order = order;
        return this;
    }

    public String getService() {
        return service;
    }

    public String getStatus() {
        return status;
    }

    public Properties setStatus(String status) {
        this.status = status;
        return this;
    }

    public Properties setService(String service) {
        this.service = service;
        return this;
    }

    public String getContactPhoneNumber() {
        return contactPhoneNumber;
    }

    public Properties setContactPhoneNumber(String contactPhoneNumber) {
        this.contactPhoneNumber = contactPhoneNumber;
        return this;
    }
}
