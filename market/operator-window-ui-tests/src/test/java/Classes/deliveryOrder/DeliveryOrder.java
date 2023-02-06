package ui_tests.src.test.java.Classes.deliveryOrder;

public class DeliveryOrder {
    private MainProperties mainProperties;

    @Override
    public boolean equals(Object actualDeliveryOrder) {
        if (actualDeliveryOrder == this) {
            return true;
        }
        if (actualDeliveryOrder == null || actualDeliveryOrder.getClass() != this.getClass()) {
            return false;
        }

        DeliveryOrder deliveryOrder = (DeliveryOrder) actualDeliveryOrder;
        if(deliveryOrder.getMainProperties()!=null){
            if (!mainProperties.equals(deliveryOrder.mainProperties)){
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "DeliveryOrder{" +
                "mainProperties=" + mainProperties +
                '}';
    }

    public DeliveryOrder setMainProperties(MainProperties mainProperties) {
        this.mainProperties = mainProperties;
        return this;
    }

    public MainProperties getMainProperties() {
        return this.mainProperties;
    }
}
