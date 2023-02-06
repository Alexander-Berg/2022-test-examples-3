package ui_tests.src.test.java.Classes.customer;

import java.util.Objects;

public class Customer {
    private MainProperties mainProperties = new MainProperties();

    @Override
    public String toString() {
        return "Customer{" +
                "mainProperties=" + mainProperties +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(mainProperties, customer.mainProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainProperties);
    }

    public MainProperties getMainProperties() {
        return mainProperties;
    }

    public Customer setMainProperties(MainProperties mainProperties) {
        this.mainProperties = mainProperties;
        return this;
    }
}
