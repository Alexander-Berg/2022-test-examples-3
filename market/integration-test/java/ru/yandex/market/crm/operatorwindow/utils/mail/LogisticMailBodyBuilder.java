package ru.yandex.market.crm.operatorwindow.utils.mail;

import ru.yandex.market.jmf.module.ticket.Ticket;

public class LogisticMailBodyBuilder {
    public static final Long DEFAULT_ORDER_ID = 11989056L;
    public static final String DEFAULT_CLIENT_TYPE = "обычный_клиент";
    public static final String DEFAULT_CLIENT_PROBLEM = "Нарушены сроки доставки";
    public static final String DEFAULT_CLIENT_EMAIL = "galoktev@yandex.ru";
    public static final String DEFAULT_CLIENT_NAME = "Григорий";
    public static final String DEFAULT_REQUEST_FROM = "почта_или_чат";
    public static final String DEFAULT_RESPONSE_TO = "почта";
    public static final String DEFAULT_DELIVERY_SERVICE = "ПЭК";
    public static final String DEFAULT_SORTING_CENTER = "Свердловск-сортировочный";
    public static final String EXAMPLE_NEW_DELIVERY_DATE = "14.03 c 09:00 до 18:00";
    public static final String EXAMPLE_REGION = "Москва";
    public static final String EXAMPLE_NEW_DELIVERY_ADDRESS = "ул. Широкая д. 3 кв. 18";
    public static final String EXAMPLE_NEW_DELIVERY_INTERVAL = "c 09:00 до 18:00";
    public static final String EXAMPLE_CATEGORY = "Жалоба на СД";
    public static final String EXAMPLE_NEW_DELIVERY_INDEX = "620000";
    public static final String EXAMPLE_NEW_BUYER_ADDRESS_PHONE = "+79221234567";
    public static final String EXAMPLE_NEW_RECIPIENT_FULL_NAME = "Иванов Иван Иванович";

    protected DynamicLogisticMailBodyBuilder builder;

    private Long orderId;
    private String clientType;
    private String clientProblem;
    private String clientEmail;
    private String clientName;
    private String requestFrom;
    private String responseTo;
    private String deliveryService;
    private String sortingCenter;
    private String newDeliveryDate;
    private String region;
    private String newDeliveryAddress;
    private String newDeliveryInterval;
    private String category;
    private String newDeliveryIndex;
    private String newBuyerAddressPhone;
    private String newRecipientFullName;
    private Long beruComplaintsTicket;

    public LogisticMailBodyBuilder() {
        builder = new DynamicLogisticMailBodyBuilder();
    }

    public LogisticMailBodyBuilder(String keySeparator, String linesSeparator) {
        builder = new DynamicLogisticMailBodyBuilder(keySeparator, linesSeparator);
    }

    public LogisticMailBodyBuilder(boolean setDefaultValues) {
        this();
        if (setDefaultValues) {
            setDefaultValues();
        }
    }

    public LogisticMailBodyBuilder(String keySeparator, String linesSeparator, boolean setDefaultValues) {
        this(keySeparator, linesSeparator);
        if (setDefaultValues) {
            setDefaultValues();
        }
    }

    public LogisticMailBodyBuilder setDefaultValues() {
        setClientName(DEFAULT_CLIENT_NAME);
        setClientType(DEFAULT_CLIENT_TYPE);
        setOrderId(DEFAULT_ORDER_ID);
        setRequestFrom(DEFAULT_REQUEST_FROM);
        setClientProblem(DEFAULT_CLIENT_PROBLEM);
        setResponseTo(DEFAULT_RESPONSE_TO);
        setClientEmail(DEFAULT_CLIENT_EMAIL);
        setDeliveryService(DEFAULT_DELIVERY_SERVICE);
        setSortingCenter(DEFAULT_SORTING_CENTER);

        return this;
    }

    public String build() {
        var owLink = "https://ow.market.yandex-team.ru/order/";

        builder.addAttribute("Заказ", owLink + orderId)
                .addAttribute("Имя клиента", clientName)
                .addAttribute("Тип клиента", clientType)
                .addAttribute("Номер заказа", orderId)
                .addAttribute("Откуда запрос?", requestFrom)
                .addAttribute("Ссылка на тикет в Едином Окне", owLink + orderId)
                .addAttribute("Проблема клиента", clientProblem)
                .addAttribute("Как удобно получить ответ?", responseTo)
                .addAttribute("e-mail клиента для ответа или отправки кассового чека", clientEmail)
                .addAttribute("Причина обращения покупателя по нарушению срока доставки",
                        "Нарушены сроки: РДД был вчера или раньше")
                .addAttribute("Служба доставки", deliveryService)
                .addAttribute("Сортировочный центр", sortingCenter)
                .addAttribute("Последняя дата доставки в интервале", "2020-01-08")
                .addAttribute("Новая дата доставки", newDeliveryDate)
                .addAttribute("Регион", region)
                .addAttribute("Новый адрес доставки", newDeliveryAddress)
                .addAttribute("Новый интервал", newDeliveryInterval)
                .addAttribute("Категория обращения", category)
                .addAttribute("Новый индекс", newDeliveryIndex)
                .addAttribute("Новый телефон", newBuyerAddressPhone)
                .addAttribute("Новые ФИО покупателя", newRecipientFullName)
                .addAttribute("Номер исходного обращения", beruComplaintsTicket);

        return builder.build();
    }

    public Long getOrderId() {
        return orderId;
    }

    public LogisticMailBodyBuilder setOrderId(Long orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getNewDeliveryDate() {
        return newDeliveryDate;
    }

    public LogisticMailBodyBuilder setNewDeliveryDate(String newDeliveryDate) {
        this.newDeliveryDate = newDeliveryDate;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public LogisticMailBodyBuilder setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getNewDeliveryAddress() {
        return newDeliveryAddress;
    }

    public LogisticMailBodyBuilder setNewDeliveryAddress(String newDeliveryAddress) {
        this.newDeliveryAddress = newDeliveryAddress;
        return this;
    }

    public String getNewDeliveryInterval() {
        return newDeliveryInterval;
    }

    public LogisticMailBodyBuilder setNewDeliveryInterval(String newDeliveryInterval) {
        this.newDeliveryInterval = newDeliveryInterval;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public LogisticMailBodyBuilder setCategory(String... categories) {
        this.category = String.join(", ", categories);
        return this;
    }

    public String getClientType() {
        return clientType;
    }

    public LogisticMailBodyBuilder setClientType(String clientType) {
        this.clientType = clientType;
        return this;
    }

    public String getClientProblem() {
        return clientProblem;
    }

    public LogisticMailBodyBuilder setClientProblem(String... clientProblems) {
        this.clientProblem = String.join(", ", clientProblems);
        return this;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public LogisticMailBodyBuilder setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
        return this;
    }

    public String getClientName() {
        return clientName;
    }

    public LogisticMailBodyBuilder setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public String getRequestFrom() {
        return requestFrom;
    }

    public LogisticMailBodyBuilder setRequestFrom(String requestFrom) {
        this.requestFrom = requestFrom;
        return this;
    }

    public String getResponseTo() {
        return responseTo;
    }

    public LogisticMailBodyBuilder setResponseTo(String responseTo) {
        this.responseTo = responseTo;
        return this;
    }

    public LogisticMailBodyBuilder setDeliveryService(String deliveryService) {
        this.deliveryService = deliveryService;
        return this;
    }

    public LogisticMailBodyBuilder setSortingCenter(String sortingCenter) {
        this.sortingCenter = sortingCenter;
        return this;
    }

    public String getNewDeliveryIndex() {
        return newDeliveryIndex;
    }

    public LogisticMailBodyBuilder setNewDeliveryIndex(String newDeliveryIndex) {
        this.newDeliveryIndex = newDeliveryIndex;
        return this;
    }

    public String getNewBuyerAddressPhone() {
        return newBuyerAddressPhone;
    }

    public LogisticMailBodyBuilder setNewBuyerAddressPhone(String newBuyerAddressPhone) {
        this.newBuyerAddressPhone = newBuyerAddressPhone;
        return this;
    }

    public String getNewRecipientFullName() {
        return newRecipientFullName;
    }

    public LogisticMailBodyBuilder setNewRecipientFullName(String newRecipientFullName) {
        this.newRecipientFullName = newRecipientFullName;
        return this;
    }

    public Long getBeruComplaintsTicket() {
        return beruComplaintsTicket;
    }

    public LogisticMailBodyBuilder setBeruComplaintsTicket(Ticket beruComplaintsTicket) {
        this.beruComplaintsTicket = beruComplaintsTicket.getId();
        return this;
    }

    @Override
    public String toString() {
        return "LogisticMailBodyBuilder{" +
                "orderId=" + orderId +
                '}';
    }
}
