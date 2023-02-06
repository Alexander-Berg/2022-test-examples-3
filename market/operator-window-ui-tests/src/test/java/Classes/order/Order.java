package ui_tests.src.test.java.Classes.order;

import Classes.Comment;
import Classes.OrderItem;
import Classes.customer.Customer;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private DeliveryProperties deliveryProperties = new DeliveryProperties();
    private MainProperties mainProperties = new MainProperties();
    private PaymentProperties paymentProperties = new PaymentProperties();
    private Customer customer = new Customer();
    private List<Comment> comments = new ArrayList<>();
    private List<OrderItem> orderItem = new ArrayList<>();
    private List<History> histories = new ArrayList<>();

    public List<History> getHistories() {
        return histories;
    }

    public Order setHistories(List<History> histories) {
        this.histories = histories;
        return this;
    }

    public List<OrderItem> getOrderItem() {
        return orderItem;
    }

    public Order setOrderItem(List<OrderItem> orderItem) {
        this.orderItem = orderItem;
        return this;
    }

    public DeliveryProperties getDeliveryProperties() {
        return deliveryProperties;
    }

    public Order setDeliveryProperties(DeliveryProperties deliveryProperties) {
        this.deliveryProperties = deliveryProperties;
        return this;
    }

    public MainProperties getMainProperties() {
        return mainProperties;
    }

    public Order setMainProperties(MainProperties mainProperties) {
        this.mainProperties = mainProperties;
        return this;
    }

    public PaymentProperties getPaymentProperties() {
        return paymentProperties;
    }

    public Order setPaymentProperties(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
        return this;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Order setCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public Order setComments(List<Comment> comments) {
        this.comments = comments;
        return this;
    }

    @Override
    public String toString() {
        return "Order{" +
                "deliveryProperties=" + deliveryProperties +
                ", mainProperties=" + mainProperties +
                ", paymentProperties=" + paymentProperties +
                ", customer=" + customer +
                ", comments=" + comments +
                ", orderItem=" + orderItem +
                ", histories=" + histories +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        Order order = (Order) obj;
        if (!this.mainProperties.equals(order.mainProperties)) {
            return false;
        }
        if (!this.deliveryProperties.equals(order.deliveryProperties)) {
            return false;
        }
        if (!this.paymentProperties.equals(order.paymentProperties)) {
            return false;
        }
        if (!this.customer.equals(order.customer)) {
            return false;
        }
        if (!this.comments.containsAll(order.comments)) {
            return false;
        }
        if (!this.orderItem.containsAll(order.orderItem)) {
            return false;
        }
        if (!this.histories.containsAll(order.histories)) {
            return false;
        }
        return true;
    }
}
