package ui_tests.src.test.java.unit;

import Classes.OrderItem;
import Classes.customer.Customer;
import Classes.deliveryOrder.DeliveryOrder;
import Classes.order.DeliveryProperties;
import Classes.order.MainProperties;
import Classes.order.Order;
import Classes.order.PaymentProperties;

import java.util.Arrays;
import java.util.HashMap;

public class Orders {

    /**
     * Заказ с таблицей способа платежа и суммой платежа
     */
    public static Order getOrderWithTypePaymentAndPaymentAmount() {
        HashMap<String, String> map = new HashMap();
        map.put("YANDEX_CASHBACK", "756,00 ₽");
        map.put("Яндекс", "259,00 ₽");
        Order orderWithTypePaymentAndPaymentAmount = new Order()
                .setMainProperties(new MainProperties().setOrderNumber("7189512"))
                .setPaymentProperties(new PaymentProperties()
                        .setTypePaymentAndPaymentAmount(map));
        return orderWithTypePaymentAndPaymentAmount;
    }


    /**
     * TODO: переименовать заказ – написать специфику
     * Заказ DSBS
     */
    public static Order getDSBSOrder() {
        Order DSBSOrder = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("7212530")
                        .setStatus("доставлен (вручен)")
                        .setLinkToOrderPage(Config.getProjectURL() + "/order/7212530")
                        .setTypeMarket("Белый DSBS")
                        .setPaymentType("Предоплата")
                        .setDateCreate("23.09.2020 17:19"))
                .setDeliveryProperties(new DeliveryProperties()
                        .setDeliveryTimeFull("15-18 февраля время по договорённости")
                        .setOriginalDeliveryTimeFull("15-18 февраля время по договорённости")
                        .setTypeDelivery("курьер: Собственная служба")
                        .setTrackCode("")
                        .setAddress("Русь, 131488, г. Питер, метро Петровско-Разумовская, ул. Победы, д. 13, корп. 666, подъезд 404, домофон 007, этаж 8, кв. 303")
                        .setConsignee("последнееимя первоеимя среднееимя • 02")
                        .setComment("заметочка")
                        .setCountOfBoxes("0")
                        .setWeightOfOrder("0 г")
                        .setDateOfShipment(""))
                .setPaymentProperties(new PaymentProperties()
                        .setPayer("последнееимя первоеимя среднееимя")
                        .setOrderAmount("1 090,00 ₽")
                        .setCostDelivery("1 000,00 ₽")
                        .setTotalCostOrder("2 090,00 ₽")
                        .setTypePayment("Оплата через Яндекс"));
        return DSBSOrder;
    }

    /**
     * Заказ с разной датой доставки при оформлении и даты доставки
     */
    public static Order getPostpaidOrder() {
        Order postpaidOrder = new Order()
                .setMainProperties(new MainProperties()
                        .setDateCreate("06.10.2019 05:02")
                        .setOrderNumber("4379129")
                        .setPaymentType("Постоплата")
                        .setTypeMarket("Покупки")
                        .setLinkToOrderPage(Config.getProjectURL() + "/order/4379129")
                        .setStatus("отменен")
                )
                .setDeliveryProperties(new DeliveryProperties() //Доставка
                        .setOriginalDeliveryTimeFull("8 октября время по договорённости")
                        .setDeliveryTimeFull("23 октября 09:00-22:00")
                        .setTypeDelivery("курьер: DPD")
                        .setAddress("Россия, 123456, г. Тест-Петербург, метро Тестовская, ул. Тестовая, д. 42, корп. a, подъезд 1, домофон 42test, этаж 13, кв. 111")
                        .setRegion("Ростов-на-Дону")
                        .setComment("")
                        .setConsignee("Смоктуновский Инокетий Иванович • +79123456789")
                        .setTrackCode("D0000824587")
                        .setCourierDataButton(false)
                        .setCountOfBoxes("1")
                        .setWeightOfOrder("2.500 кг")
                        .setDateOfShipment("06.10.2019"))
                .setPaymentProperties(new PaymentProperties() //Оплата
                        .setTypePayment("Наличными при получении")
                        .setCostDelivery("0,00 ₽")
                        .setOrderAmount("3 668,00 ₽")
                        .setPayer("Смоктуновский Инокетий Иванович")
                        .setTotalCostOrder("3 668,00 ₽"));
        return postpaidOrder;
    }

    /**
     * Заказ с постоплатой и заказом логистики
     */
    public static Order getPostpaidOrderWithADeliveryOrder() {
        Order postpaidOrderWithADeliveryOrder = new Order()
                .setPaymentProperties(new PaymentProperties() //Оплата
                        .setTypePayment("Наличными при получении")
                        .setCostDelivery("49,00 ₽")
                        .setOrderAmount("737,00 ₽")
                        .setPayer("Пупкин Василий")
                        .setTotalCostOrder("785,00 ₽"))
                .setDeliveryProperties(new DeliveryProperties() //Доставка
                        .setOriginalDeliveryTimeFull("завтра, 3 февраля время по договорённости")
                        .setDeliveryTimeFull("завтра, 3 февраля время по договорённости")
                        .setTypeDelivery("курьер: Служба доставки #1005515")
                        .setAddress("Россия, 127018, г. Москва, ул. улица Сущёвский Вал, д. 14/22к7, кв. 16")
                        .setComment("")
                        .setConsignee("Пупкин Василий • +78906868221")
                        .setTrackCode("2177456")
                        .setCourierDataButton(false)
                        .setStatusDeliveryOrder("заказ отменён техподдержкой")
                        .setDeliveryOrderNumber("363529/32164798")
                        .setDateOfShipment("02.02.2021")
                        .setCountOfBoxes("1")
                        .setWeightOfOrder("1.100 кг")
                        .setLinkToDeliveryOrderPage(Config.getProjectURL() + "/entity/yaDeliveryOrder@2102T363529"))

                .setMainProperties(new MainProperties()
                        .setDateCreate("02.02.2021 07:27")
                        .setOrderNumber("32164798")
                        .setPaymentType("Постоплата")
                        .setStatus("отменен")
                        .setTypeMarket("Покупки")
                        .setLinkToOrderPage(Config.getProjectURL() + "/order/32164798"));

        return postpaidOrderWithADeliveryOrder;
    }

    /**
     * Заказ с курьером яндекса
     */
    public static Order getOrderWithYandexCourier() {
        Order orderWithYandexCourier = new Order()
                .setMainProperties(new MainProperties()
                        .setDateCreate("23 июля 10:00-18:00")
                        .setOrderNumber("32821209")
                        .setPaymentType("Постоплата")
                        .setTypeMarket("Покупки")
                        .setLinkToOrderPage(Config.getProjectURL() + "/order/32821209")
                        .setStatus("в обработке"))
                .setDeliveryProperties(new DeliveryProperties() //Доставка
                        .setEstimatedDeliveryTime("3 сентября 12:00-13:00")
                        .setDeliveryTimeFull("11 сентября 10:00-19:00")
                        .setOriginalDeliveryTimeFull("11 сентября 10:00-19:00")
                        .setTypeDelivery("курьер: Яндекс")
                        .setAddress("Россия, 107045, г. Москва, метро Пражская, ул. Сретенка, д. 14, корп. 1, подъезд 404, домофон 007, этаж 8, кв. 303")
                        .setComment("")
                        .setConsignee("последнееимя первоеимя среднееимя • +78887776655")
                        .setTrackCode("1303765")
                        .setRegion("Москва")
                        .setCourierDataButton(true)
                        .setFirstNameCourier("Trash Trash")
                        .setPhoneCourier("79220393379")
                        .setWeightOfOrder("500 г")
                        .setDateOfShipment("10.09.2021")
                        .setCountOfBoxes("1"))
                .setDeliveryProperties(new DeliveryProperties()
                        .setDeliveryOrderNumber("713067/32821209")
                        .setLinkToDeliveryOrderPage("https://ow.tst.market.yandex-team.ru/entity/yaDeliveryOrder@2109T713067")
                        .setStatusDeliveryOrder("доставляется по городу")
                )
                .setPaymentProperties(new PaymentProperties() //Оплата
                        .setTypePayment("Наличными при получении")
                        .setCostDelivery("99,00 ₽")
                        .setOrderAmount("300,00 ₽")
                        .setPayer("последнееимя первоеимя среднееимя")
                        .setTotalCostOrder("399,00 ₽"));
        return orderWithYandexCourier;
    }

    /**
     * Заказ с курьером яндекса без ссылки на трекинг курьера
     */
    public static Order getOrderWithYandexCourier2() {
        Order orderWithYandexCourier2 = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("6752745"));
        return orderWithYandexCourier2;
    }

    /**
     * логистический заказ
     */
    public static DeliveryOrder getDeliveryOrder() {
        DeliveryOrder deliveryOrder = new DeliveryOrder()
                .setMainProperties(new Classes.deliveryOrder.MainProperties()
                        .setDeliveryOrderNumber("21675/6916220")
                        .setStatus("Решен")
                        .setRecipientFullName("Смоктуновский Инокетий Иванович")
                        .setRecipientAddress("Область: Свердловская область\n" +
                                "Населенный пункт: Екатеринбург\n" +
                                "Улица: Вокзальная\n" +
                                "Дом: 22\n" +
                                "Квартира или офис: 111\n" +
                                "Почтовый индекс: 620107")
                        .setLinkToDeliveryOrderPage(Config.getProjectURL() + "/entity/yaDeliveryOrder@2008T21675"));
        return deliveryOrder;
    }

    /**
     * Получить заказ с меткой "было изменение рдд"
     *
     * @return
     */
    public static Order getOrderWithMarkersRDD() {
        Order order = new Order().setMainProperties(new MainProperties()
                .setOrderNumber("6822843")
                .setMarkers(Arrays.asList("было изменение рдд")));
        return order;
    }

    /**
     * Получить заказ с меткой "Яндекс.Плюс"
     *
     * @return
     */
    public static Order getOrderWithMarkersYandexPlus() {
        Order order = new Order().setMainProperties(new MainProperties()
                .setOrderNumber("2806543")
                .setMarkers(Arrays.asList("яндекс.плюс")));
        return order;
    }

    /**
     * Получить заказ с меткой заказа "подозрение на фрод" и меткой клиента "VIP" по email
     *
     * @return
     */
    public static Order getOrderWithMarkersOrderAntiFraudAndMarkersCustomerVIP() {
        Order order = new Order().setMainProperties(new MainProperties()
                .setOrderNumber("6420120")
                .setMarkers(Arrays.asList("подозрение на фрод")))
                .setCustomer(new Customer()
                        .setMainProperties(new Classes.customer.MainProperties()
                                .setMarkers(Arrays.asList("VIP"))));
        return order;
    }

    /**
     * Получить заказ с меткой клиента "VIP" по номеру телефона
     *
     * @return
     */
    public static Order getOrderWithMarkersCustomerVIPByPhoneNumber() {
        Order order = new Order().setMainProperties(new MainProperties()
                .setOrderNumber("7283228"))
                .setCustomer(new Customer()
                        .setMainProperties(new Classes.customer.MainProperties()
                                .setMarkers(Arrays.asList("VIP"))));
        return order;
    }

    /**
     * Получить заказ с меткой клиента "VIP" по UID клиента
     *
     * @return
     */
    public static Order getOrderWithMarkersCustomerVIPByUID() {
        Order order = new Order().setMainProperties(new MainProperties()
                .setOrderNumber("7140547"))
                .setCustomer(new Customer()
                        .setMainProperties(new Classes.customer.MainProperties()
                                .setMarkers(Arrays.asList("VIP"))));
        return order;
    }

    /**
     * Получить заказ с меткой "Архивный"
     *
     * @return
     */
    public static Order getOrderWithMarkersArchived() {
        Order order = new Order().setMainProperties(new MainProperties()
                .setOrderNumber("7374800")
                .setMarkers(Arrays.asList("архивный")));

        return order;
    }

    /**
     * Получить заказ начисленным кэшбеком
     *
     * @return
     */
    public static Order getOrderWithCashBack1() {
        Order order = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("7274832"))
                .setPaymentProperties(new PaymentProperties()
                        .setAccruedCashBack("231"))
                .setOrderItem(
                        Arrays.asList(
                                new OrderItem().setAccruedCashBack("154"),
                                new OrderItem().setAccruedCashBack("77")
                        )
                );

        return order;
    }

    /**
     * Получить заказ начисленным кэшбеком
     *
     * @return
     */
    public static Order getOrderWithCashBack2() {
        Order order = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("7203441"))
                .setOrderItem(Arrays.asList(new OrderItem().setCashBackSpent("756")));

        return order;
    }

    /**
     * Получить заказ с плановым кэшбэком
     *
     * @return
     */
    public static Order getOrderWithCashBack3() {
        Order order = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("32073904"))
                .setOrderItem(Arrays.asList(new OrderItem()
                        .setPlannedCashback("37.00")));
        return order;
    }

    /**
     * Мультизаказ
     */
    public static Order getMultiOrder() {
        Order order = new Order().setMainProperties(new MainProperties()
                .setOrderNumber("3237167")).
                setDeliveryProperties(new DeliveryProperties()
                        .setLinkToMultiOrderPage(Config.getProjectURL() + "/entity/order@1904T3237168")
                        .setNumberOfMultiOrder("3237168"));
        return order;
    }

    /**
     * Получить DSBS-заказ
     *
     * @return
     */
    public static Order getOrderWithMarketTitleDSBS() {
        Order order = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("7375586"))
                .setOrderItem(
                        Arrays.asList(
                                new OrderItem().setOrderMarketTitle("DSBS (Витрина)")
                        )
                );

        return order;
    }

    /**
     * Получить Click&Collect-заказ
     *
     * @return
     */
    public static Order getOrderWithMarketTitleClickCollect() {
        Order order = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("2922668"))
                .setOrderItem(
                        Arrays.asList(
                                new OrderItem().setOrderMarketTitle("Click&Collect (Витрина)")
                        )
                );

        return order;
    }

    /**
     * Получить Cross-docking-заказ
     *
     * @return
     */
    public static Order getOrderWithMarketTitleCrossDocking() {
        Order order = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("4535416"))
                .setOrderItem(
                        Arrays.asList(
                                new OrderItem().setOrderMarketTitle("Cross-docking (Витрина + Упаковка + Доставка)")
                        )
                );

        return order;
    }

    /**
     * Получить заказ с 1p и 3p товарами
     *
     * @return
     */
    public static Order getOrderWithMarketTitle1PAnd3p() {
        Order order = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("3565340"))
                .setOrderItem(
                        Arrays.asList(
                                new OrderItem().setOrderMarketTitle("1P"),
                                new OrderItem().setOrderMarketTitle("Товар 3P")
                        )
                );

        return order;
    }

    /**
     * Получить Dropshipping-заказ
     *
     * @return
     */
    public static Order getOrderWithMarketTitleDropshipping() {
        Order order = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("32057671"))
                .setOrderItem(
                        Arrays.asList(
                                new OrderItem().setOrderMarketTitle("Dropshipping (Витрина + Доставка)")
                        )
                );

        return order;
    }
}
