package ui_tests.src.test.java.Classes.yaDeliveryOrder;

public class YaDeliveryOrder {

    private MainProperties mainProperties = new MainProperties();
    private GeneralProperties generalProperties = new GeneralProperties();

    /**
     * Задать основную информацию
     */
    public YaDeliveryOrder setMainProperties(MainProperties mainProperties){
        this.mainProperties = mainProperties;
        return this;
    }

    /**
     * Задать Общую информацию
     * @param generalProperties
     * @return
     */
    public YaDeliveryOrder setGeneralProperties(GeneralProperties generalProperties){
        this.generalProperties = generalProperties;
        return this;
    }

    @Override
    public String toString() {
        return "YaDeliveryOrder{" +
                "mainProperties=" + mainProperties +
                ", generalProperties=" + generalProperties +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        YaDeliveryOrder order = (YaDeliveryOrder) obj;
        if (this.mainProperties != null) {
            if (!this.mainProperties.equals(order.mainProperties)) {
                return false;
            }
        }
        if (this.generalProperties != null) {
            if (!this.generalProperties.equals(order.generalProperties)) {
                return false;
            }
        }
        return true;
    }
}
