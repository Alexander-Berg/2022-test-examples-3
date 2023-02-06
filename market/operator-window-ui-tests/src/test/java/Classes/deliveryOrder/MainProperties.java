package ui_tests.src.test.java.Classes.deliveryOrder;

public class MainProperties {

    private String deliveryOrderNumber;
    private String status ;
    private String recipientFullName;
    private String recipientAddress;
    private String linkToDeliveryOrderPage;


    @Override
    public boolean equals(Object actualMainProperties) {
        if (actualMainProperties == this) {
            return true;
        }
        if (actualMainProperties == null || actualMainProperties.getClass() != this.getClass()) {
            return false;
        }
        MainProperties mainProperties = (MainProperties) actualMainProperties;

        if (mainProperties.deliveryOrderNumber!=null){
            if (!deliveryOrderNumber.equals(mainProperties.deliveryOrderNumber)){
                return false;
            }
        }

        if (mainProperties.status!=null){
            if (!status.equals(mainProperties.status)){
                return false;
            }
        }

        if (mainProperties.recipientFullName!=null){
            if (!recipientFullName.equals(mainProperties.recipientFullName)){
                return false;
            }
        }

        if (mainProperties.recipientAddress!=null){
            if (!recipientAddress.equals(mainProperties.recipientAddress)){
                return false;
            }
        }

        if (mainProperties.linkToDeliveryOrderPage!=null){
            if (!linkToDeliveryOrderPage.equals(mainProperties.linkToDeliveryOrderPage)){
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "MainProperties{" +
                "deliveryOrderNumber='" + deliveryOrderNumber + '\'' +
                ", status='" + status + '\'' +
                ", recipientFullName='" + recipientFullName + '\'' +
                ", recipientAddress='" + recipientAddress + '\'' +
                ", linkToDeliveryOrderPage='" + linkToDeliveryOrderPage + '\'' +
                '}';
    }

    /**
     * Получить ссылку на страниу заказа логистики
     * @return
     */
    public String getLinkToDeliveryOrderPage() {
        return linkToDeliveryOrderPage;
    }

    /**
     * Указать ссылку на страну заказа логистики
     * @param linkToDeliveryOrderPage ссыдка на заказ логистики
     * @return
     */
    public MainProperties setLinkToDeliveryOrderPage(String linkToDeliveryOrderPage) {
        this.linkToDeliveryOrderPage = linkToDeliveryOrderPage;
        return this;
    }

    /**
     * Получить идентификатор заказа
     * @return номер заказа
     */
    public String getDeliveryOrderNumber() {
        return deliveryOrderNumber;
    }

    /**
     * указать идентификатор заказа
     * @param deliveryOrderNumber - номер заказа
     * @return
     */
    public MainProperties setDeliveryOrderNumber(String deliveryOrderNumber) {
        this.deliveryOrderNumber = deliveryOrderNumber;
        return this;
    }

    /**
     * Получить статус заказа
     * @return статус заказа
     */
    public String getStatus() {
        return status;
    }

    /**
     * указать статус заказа
     * @param status статус заказа
     * @return
     */
    public MainProperties setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * получить ФИО получателя
     * @return ФИО получателя
     */
    public String getRecipientFullName() {
        return recipientFullName;
    }

    /**
     * указать ФИО получателя
     * @param recipientFullName ФИО получателя
     * @return
     */
    public MainProperties setRecipientFullName(String recipientFullName) {
        this.recipientFullName = recipientFullName;
        return this;
    }

    /**
     * получить адрес получателя
     * @return адрес получателя
     */
    public String getRecipientAddress() {
        return recipientAddress;
    }

    /**
     * указать адрес получателя
     * @param recipientAddress адрес получателя
     * @return
     */
    public MainProperties setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
        return this;
    }
}
