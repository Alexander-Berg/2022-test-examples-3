package ui_tests.src.test.java.Classes.yaDeliveryOrder;

public class GeneralProperties {

    private String orderCreationDate;
    private String barcode;
    private String deliveryType;
    private String deliveryInterval;
    private String clientFullName;
    private String clientNumber;
    private String clientEmail;
    private String clientsAddress;

    /**
     * Задать время создания заказа
     * @param orderCreationDate
     * @return
     */
    public GeneralProperties setOrderCreationDate(String orderCreationDate){
        this.orderCreationDate = orderCreationDate;
        return this;
    }

    /**
     * Получить время создания заказа
     * @return
     */
    public String getOrderCreationDate(){ return orderCreationDate; }

    /**
     * Задать Barcode заказа
     * @param barcode
     * @return
     */
    public GeneralProperties setBarcode(String barcode){
        this.barcode = barcode;
        return this;
    }

    /**
     * Получить Barcode заказа
     * @return
     */
    public String getBarcode(){ return barcode; }

    /**
     * Задать тип доставки
     * @param deliveryType
     * @return
     */
    public GeneralProperties setDeliveryType(String deliveryType){
        this.deliveryType = deliveryType;
        return this;
    }

    /**
     * Получить тип доставки
     * @return
     */
    public String getDeliveryType(){ return deliveryType; }

    /**
     * Задать интервал доставки
     * @param deliveryInterval
     * @return
     */
    public GeneralProperties setDeliveryInterval(String deliveryInterval){
        this.deliveryInterval = deliveryInterval;
        return this;
    }

    /**
     * Получить интервал доставки
     * @return
     */
    public String getDeliveryInterval(){ return deliveryInterval; }

    /**
     * Задать ФИО получателя
     * @param clientFullName
     * @return
     */
    public GeneralProperties setClientFullName(String clientFullName){
        this.clientFullName = clientFullName;
        return this;
    }

    /**
     * Получить ФИО получателя
     * @return
     */
    public String getClientFullName(){ return clientFullName; }

    /**
     * Задать телефон получателя
     * @param clientNumber
     * @return
     */
    public GeneralProperties setClientNumber(String clientNumber){
        this.clientNumber = clientNumber;
        return this;
    }

    /**
     * Получить телефон получателя
     * @return
     */
    public String getClientNumber(){ return clientNumber; }

    /**
     * Задать email получателя
     * @param clientEmail
     * @return
     */
    public GeneralProperties setClientEmail(String clientEmail){
        this.clientEmail = clientEmail;
        return this;
    }

    /**
     * Получить email получателя
     * @return
     */
    public String getClientEmail(){ return clientEmail; }

    /**
     * Задать адрес получателя
     * @param clientsAddress
     * @return
     */
    public GeneralProperties setClientsAddress(String clientsAddress){
        this.clientsAddress = clientsAddress;
        return this;
    }

    /**
     * Получить адрес получателя
     * @return
     */
    public String getClientsAddress(){ return clientsAddress; }

    @Override
    public String toString() {
        return "MainProperties{" +
                "orderCreationDate='" + orderCreationDate + '\'' +
                ", barcode='" + barcode + '\'' +
                ", deliveryType='" + deliveryType + '\'' +
                ", deliveryInterval='" + deliveryInterval + '\'' +
                ", clientFullName='" + clientFullName + '\'' +
                ", clientNumber='" + clientNumber + '\'' +
                ", clientEmail='" + clientEmail + '\'' +
                ", clientsAddress='" + clientsAddress +
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
        GeneralProperties actualGeneralProperties = (GeneralProperties) obj;

        if (this.orderCreationDate != null) {
            if (!orderCreationDate.equals(actualGeneralProperties.orderCreationDate)) {
                return false;
            }
        }
        if (this.barcode != null) {
            if (!barcode.equals(actualGeneralProperties.barcode)) {
                return false;
            }
        }
        if (this.deliveryType != null) {
            if (!deliveryType.equals(actualGeneralProperties.deliveryType)) {
                return false;
            }
        }
        if (this.deliveryInterval != null) {
            if (!deliveryInterval.equals(actualGeneralProperties.deliveryInterval)) {
                return false;
            }
        }

        if (this.clientFullName!=null){
            if(!clientFullName.equals(actualGeneralProperties.clientFullName)){
                return false;
            }
        }

        if (this.clientNumber!=null){
            if (!clientNumber.equals(actualGeneralProperties.clientNumber)){
                return false;
            }
        }
        if (this.clientEmail!=null){
            if (!clientEmail.equals(actualGeneralProperties.clientEmail)){
                return false;
            }
        }
        if (this.clientsAddress!=null){
            if (!clientsAddress.equals(actualGeneralProperties.clientsAddress)){
                return false;
            }
        }
        return true;
    }
}
