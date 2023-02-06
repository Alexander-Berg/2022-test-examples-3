package ui_tests.src.test.java.Classes.yaDeliveryOrder;

public class MainProperties {

    private String number;

    /**
     * Задать номер заказа
     * @param number
     * @return
     */
    public MainProperties setNumber(String number){
        this.number = number;
        return this;
    }

    /**
     * Получить номер заказа
     * @return
     */
    public String getNumber(){ return number; }

    @Override
    public String toString() {
        return "MainProperties{" +
                "number=" + number +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        MainProperties mainProperties = (MainProperties) obj;
        if (this.number != null) {
            if (!this.number.equals(mainProperties.number)) {
                return false;
            }
        }
        return true;
    }
}
