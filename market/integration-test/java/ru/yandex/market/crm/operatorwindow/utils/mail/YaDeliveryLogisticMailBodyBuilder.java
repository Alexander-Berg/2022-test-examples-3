package ru.yandex.market.crm.operatorwindow.utils.mail;

import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrder;

public class YaDeliveryLogisticMailBodyBuilder extends LogisticMailBodyBuilder {

    public static final String YA_DELIVERY_CLIENT_NAME_EXAMPLE = "Жан Рено";
    public static final String YA_DELIVERY_CLIENT_EMAIL_EXAMPLE = "renault@yandex-team.ru";
    public static final String DELIVERY_ORDER_EXAMPLE = "E2E4";
    public static final String DELIVERY_SERVICE_NAME_EXAMPLE = "ПЭК";
    public static final String SORTING_CENTER_NAME_EXAMPLE = "Свердловск-сортировочный";
    public static final String YA_DELIVERY_REGION_EXAMPLE = "Екатеринбург";
    public static final String PLANNED_DELIVERY_DATE_EXAMPLE = "2020-01-08";
    public static final String DATA_TO_CHANGE_EXAMPLE = "Номер подъезда";
    public static final String SHIPMENT_EXAMPLE = "911";
    public static final String SHIPMENT_TYPE_EXAMPLE = "emergency";
    public static final String PICKUP_REGION_EXAMPLE = "Не Москва";
    public static final String SHOP_ID_EXAMPLE = "8800";


    private String yaDeliveryClientName;
    private String yaDeliveryClientEmail;
    private String deliveryOrder;
    private String deliveryServiceName;
    private String sortingCenterName;
    private String yaDeliveryRegion;
    private String plannedDeliveryDate;
    private String dataToChange;
    private String shipment;
    private String shipmentType;
    private String pickupRegion;
    private String shopId;


    public YaDeliveryLogisticMailBodyBuilder() {
        super(": ", "\n");
    }

    public YaDeliveryLogisticMailBodyBuilder(boolean setDefaultValues) {
        super(": ", "\n", setDefaultValues);
    }

    @Override
    public String build() {
        super.build();

        builder.addAttribute("Ваше имя", yaDeliveryClientName)
                .addAttribute("Email, на который будет отправлен ответ", yaDeliveryClientEmail)
                .addAttribute("Номер заказа", deliveryOrder)
                .addAttribute("Служба доставки", deliveryServiceName)
                .addAttribute("Сортировочный центр", sortingCenterName)
                .addAttribute("Регион доставки", yaDeliveryRegion)
                .addAttribute("Расчётная дата доставки", plannedDeliveryDate)
                .addAttribute("Укажите данные, которые нужно внести", dataToChange)
                .addAttribute("Номер забора", shipment)
                .addAttribute("Тип забора/самопривоза", shipmentType)
                .addAttribute("Регион забора/самопривоза", pickupRegion)
                .addAttribute("ID магазина", shopId);

        return builder.build();
    }

    public String getYaDeliveryClientName() {
        return yaDeliveryClientName;
    }

    public YaDeliveryLogisticMailBodyBuilder setYaDeliveryClientName(String yaDeliveryClientName) {
        this.yaDeliveryClientName = yaDeliveryClientName;
        return this;
    }

    public String getYaDeliveryClientEmail() {
        return yaDeliveryClientEmail;
    }

    public YaDeliveryLogisticMailBodyBuilder setYaDeliveryClientEmail(String yaDeliveryClientEmail) {
        this.yaDeliveryClientEmail = yaDeliveryClientEmail;
        return this;
    }

    public String getDeliveryOrder() {
        return deliveryOrder;
    }

    public YaDeliveryLogisticMailBodyBuilder setYaDeliveryOrderText(String deliveryOrder) {
        this.deliveryOrder = deliveryOrder;
        return this;
    }

    public YaDeliveryLogisticMailBodyBuilder setYaDeliveryOrder(YaDeliveryOrder yaDeliveryOrder) {
        this.deliveryOrder = yaDeliveryOrder.getTitle();
        return this;
    }

    public String getDeliveryServiceName() {
        return deliveryServiceName;
    }

    public YaDeliveryLogisticMailBodyBuilder setDeliveryServiceName(String deliveryServiceName) {
        this.deliveryServiceName = deliveryServiceName;
        return this;
    }

    public String getSortingCenterName() {
        return sortingCenterName;
    }

    public YaDeliveryLogisticMailBodyBuilder setSortingCenterName(String sortingCenterName) {
        this.sortingCenterName = sortingCenterName;
        return this;
    }

    public String getYaDeliveryRegion() {
        return yaDeliveryRegion;
    }

    public YaDeliveryLogisticMailBodyBuilder setYaDeliveryRegion(String yaDeliveryRegion) {
        this.yaDeliveryRegion = yaDeliveryRegion;
        return this;
    }

    public String getPlannedDeliveryDate() {
        return plannedDeliveryDate;
    }

    public YaDeliveryLogisticMailBodyBuilder setPlannedDeliveryDate(String plannedDeliveryDate) {
        this.plannedDeliveryDate = plannedDeliveryDate;
        return this;
    }

    public String getDataToChange() {
        return dataToChange;
    }

    public YaDeliveryLogisticMailBodyBuilder setDataToChange(String dataToChange) {
        this.dataToChange = dataToChange;
        return this;
    }

    public String getShipment() {
        return shipment;
    }

    public YaDeliveryLogisticMailBodyBuilder setShipment(String shipment) {
        this.shipment = shipment;
        return this;
    }

    public String getShipmentType() {
        return shipmentType;
    }

    public YaDeliveryLogisticMailBodyBuilder setShipmentType(String shipmentType) {
        this.shipmentType = shipmentType;
        return this;
    }

    public String getPickupRegion() {
        return pickupRegion;
    }

    public YaDeliveryLogisticMailBodyBuilder setPickupRegion(String pickupRegion) {
        this.pickupRegion = pickupRegion;
        return this;
    }

    public String getShopId() {
        return shopId;
    }

    public YaDeliveryLogisticMailBodyBuilder setShopId(String shopId) {
        this.shopId = shopId;
        return this;
    }
}
