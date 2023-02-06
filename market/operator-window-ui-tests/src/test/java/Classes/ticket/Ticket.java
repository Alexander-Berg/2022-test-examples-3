package ui_tests.src.test.java.Classes.ticket;

import Classes.Comment;
import Classes.deliveryOrder.DeliveryOrder;
import Classes.order.Order;

import java.util.List;
import java.util.Objects;

public class Ticket {

    private Order order = new Order();
    private Properties properties = new Properties();
    private List<Comment> comments;
    private String subject;
    private List<String> dangerMessages;
    private DeliveryOrder deliveryOrder;

    @Override
    public String toString() {
        return "Ticket{" +
                "order=" + order +
                ", properties=" + properties +
                ", comments=" + comments +
                ", subject='" + subject + '\'' +
                ", dangerMessages=" + dangerMessages +
                ", deliveryOrder=" + deliveryOrder +
                '}';
    }

    public DeliveryOrder getDeliveryOrder() {
        return deliveryOrder;
    }

    public Ticket setDeliveryOrder(DeliveryOrder deliveryOrder) {
        this.deliveryOrder = deliveryOrder;
        return this;
    }

    public List<String> getDangerMessages() {
        return dangerMessages;
    }

    public Ticket setDangerMessages(List<String> dangerMessages) {
        this.dangerMessages = dangerMessages;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public Ticket setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public Properties getProperties() {
        return properties;
    }

    public Ticket setProperties(Properties properties) {
        this.properties = properties;
        return this;
    }

    public Order getOrder() {
        return order;
    }

    public Ticket setOrder(Order order) {
        this.order = order;
        return this;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public Ticket setComments(List<Comment> comments) {
        this.comments = comments;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        if (this.subject != null) {
            if (!this.subject.equals(ticket.subject)) {
                return false;
            }
        }
        if (this.comments != null) {
            if (!ticket.comments.containsAll(this.comments)) {
                return false;
            }
        }

        if (this.dangerMessages != null) {
            if (!this.dangerMessages.containsAll(ticket.dangerMessages)) {
                return false;
            }
        }

        return Objects.equals(this.order, ticket.order) &&
                Objects.equals(this.properties, ticket.properties) &&
                Objects.equals(this.deliveryOrder, ticket.deliveryOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, properties, comments, subject, dangerMessages,deliveryOrder);
    }
}
