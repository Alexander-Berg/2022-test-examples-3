package ui_tests.src.test.java.Classes.order;


import java.util.List;

public class MainProperties {

    private String orderNumber;
    private String typeMarket;
    private String paymentType;
    private String dateCreate;
    private String linkToOrderPage;
    private String status;
    private List<String> markers;
    private String platform;
    private String subStatus;

    public String getPlatform() {
        return platform;
    }

    public MainProperties setPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public String getSubStatus() {
        return subStatus;
    }

    public MainProperties setSubStatus(String subStatus) {
        this.subStatus = subStatus;
        return this;
    }

    /**
     * Получить маркеры заказа
     * @return
     */
    public List<String> getMarkers() {
        return markers;
    }

    /**
     * Задать маркеры заказа
     * @param markers лист с маркерами
     * @return
     */
    public MainProperties setMarkers(List<String> markers) {
        this.markers = markers;
        return this;
    }

    /**
     * Получить статус заказа
     * @return
     */
    public String getStatus() {
        return status;
    }

    /**
     * Задать статус заказа
     * @param status статус заказа
     * @return
     */
    public MainProperties setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Получить ссылку на страницу заказа
     * @return
     */
    public String getLinkToOrderPage() {
        return linkToOrderPage;
    }

    /**
     * Задать ссылку на страницу заказа
     * @param linkToOrderPage ссылка на страницу заказа
     * @return
     */
    public MainProperties setLinkToOrderPage(String linkToOrderPage) {
        this.linkToOrderPage = linkToOrderPage;
        return this;
    }

    /**
     * Получить дату создания заказа
     * @return
     */
    public String getDateCreate() {
        return dateCreate;
    }

    /**
     * Задать дату создания заказа
     * @param dateCreate дата создания заказа в формате ДД.ММ.ГГГГ
     * @return
     */
    public MainProperties setDateCreate(String dateCreate) {
        this.dateCreate = dateCreate;
        return this;
    }

    /**
     * Получить тип маркета
     * @return
     */
    public String getTypeMarket() {
        return typeMarket;
    }

    /**
     * Задать тип маркета
     * @param typeMarket тип маркета
     * @return
     */
    public MainProperties setTypeMarket(String typeMarket) {
        this.typeMarket = typeMarket;
        return this;
    }

    /**
     * Получить тип оплаты
     * @return
     */
    public String getPaymentType() {
        return paymentType;
    }

    /**
     * Задать тип оплаты
     * @param paymentType тип оплаты
     * @return
     */
    public MainProperties setPaymentType(String paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    /**
     * Получить номер заказа
     * @return
     */
    public String getOrderNumber() {
        return orderNumber;
    }

    /**
     * Задать номер заказа
     * @param orderNumber номер заказа
     * @return
     */
    public MainProperties setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    @Override
    public String toString() {
        return "MainProperties{" +
                "orderNumber='" + orderNumber + '\'' +
                ", typeMarket='" + typeMarket + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", dateCreate='" + dateCreate + '\'' +
                ", linkToOrderPage='" + linkToOrderPage + '\'' +
                ", status='" + status + '\'' +
                ", markers=" + markers +
                ", platform='" + platform + '\'' +
                ", subStatus='" + subStatus + '\'' +
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
        MainProperties actualMainProperties = (MainProperties) obj;

        if (this.orderNumber != null) {
            if (!orderNumber.equals(actualMainProperties.orderNumber)) {
                return false;
            }
        }
        if (this.typeMarket != null) {
            if (!typeMarket.equals(actualMainProperties.typeMarket)) {
                return false;
            }
        }
        if (this.paymentType != null) {
            if (!paymentType.equals(actualMainProperties.paymentType)) {
                return false;
            }
        }
        if (this.dateCreate != null) {
            if (!dateCreate.equals(actualMainProperties.dateCreate)) {
                return false;
            }
        }

        if (this.linkToOrderPage!=null){
            if(!linkToOrderPage.equals(actualMainProperties.linkToOrderPage)){
                return false;
            }
        }

        if (this.status!=null){
            if (!status.equals(actualMainProperties.status)){
                return false;
            }
        }
        if (this.markers!=null){
            if (markers.equals(actualMainProperties.getMarkers())){
                return false;
            }
        }
        if (this.platform!=null){
            if (!platform.equals(actualMainProperties.platform)){
                return false;
            }
        }
        if (this.subStatus!=null){
            if (!subStatus.equals(actualMainProperties.getSubStatus())){
                return false;
            }
        }
        return true;
    }

}
