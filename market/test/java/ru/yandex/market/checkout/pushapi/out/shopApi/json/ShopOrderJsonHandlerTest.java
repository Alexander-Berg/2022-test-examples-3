package ru.yandex.market.checkout.pushapi.out.shopApi.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRule;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutTimeFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.order.AddressJsonSerializer;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.order.BuyerJsonSerializer;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.order.OrderItemInstanceJsonSerializer;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.order.ShopOrderItemJsonSerializer;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.promo.OrderItemPromoJsonSerializer;
import ru.yandex.market.checkout.pushapi.out.shopApi.xml.ShopOrderXmlSerializerTest;
import ru.yandex.market.checkout.pushapi.providers.ShopOrderProvider;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrderItem;
import ru.yandex.market.checkout.pushapi.shop.entity.StubExtraContext;
import ru.yandex.market.checkout.util.ShopOrderTestData;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.DELIVERY_DATE;
import static ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.ORDER_CREATION_DATE;

//TODO пераработать тест на адекатную проверку MARKETCHECKOUT-14893
public class ShopOrderJsonHandlerTest {

    private ShopOrderItem item1 = new ShopOrderItem() {{
        setFeedId(1L);
        setOfferId("item1");
        setFeedCategoryId("Камеры");
        setOfferName("OfferName");
        setPrice(new BigDecimal("4567"));
        setBuyerPriceBeforeDiscount(new BigDecimal("4569"));
        setCount(5);
        setDelivery(true);
    }};
    private ShopOrderItem item2 = new ShopOrderItem() {{
        setFeedId(1L);
        setOfferId("item2");
        setFeedCategoryId("Камеры");
        setOfferName("OfferName");
        setPrice(new BigDecimal("4567"));
        setBuyerPriceBeforeDiscount(new BigDecimal("4569"));
        setCount(5);
        setDelivery(true);
    }};

    private DeliveryWithRegion delivery = mock(DeliveryWithRegion.class);
    private ShopOrderJsonSerializer serializer = new ShopOrderJsonSerializer();

    @BeforeEach
    public void setUp() throws Exception {
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
        serializer.setItemJsonSerializer(
                new ShopOrderItemJsonSerializer(null, null) {
                    @Override
                    public void serialize(ShopOrderItem value, JsonWriter generator) throws IOException {
                        if (value == item1) {
                            generator.setValue("item1");
                        } else if (value == item2) {
                            generator.setValue("item2");
                        } else {
                            fail();
                        }
                    }
                }
        );
        serializer.setDeliveryWithRegionJsonSerializer(
                new DeliveryWithRegionJsonSerializer() {
                    @Override
                    public void serialize(DeliveryWithRegion value, JsonWriter writer) throws IOException {
                        assertEquals(delivery, value);
                        writer.setValue("delivery");
                    }
                }
        );

        serializer.setBuyerJsonSerializer(new BuyerJsonSerializer());
        serializer.setStubExtraContextJsonSerializer(new StubExtraContextJsonSerializer(
                new ShipmentDateCalculationRuleJsonSerializer()));
    }

    @Test
    public void testSerializePostpaid() throws Exception {
        String expected = IOUtils.readInputStream(ShopOrderXmlSerializerTest.class
                .getResourceAsStream("/files/pushapi/shopOrder.json"));

        JsonTestUtil.assertJsonSerialize(
                serializer,
                new ShopOrder() {{
                    setBusinessId(1L);
                    setId(1234L);
                    setCurrency(Currency.RUR);
                    setPaymentType(PaymentType.POSTPAID);
                    setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
                    setItems(
                            Arrays.asList(
                                    item1,
                                    item2
                            )
                    );
                    setDeliveryWithRegion(delivery);
                    setNotes("notes-notes-notes");
                    setFake(true);
                    setContext(Context.SANDBOX);
                    setStubContext(StubExtraContext.builder()
                            .withShopLocalDeliveryRegion(213L)
                            .withShipmentDateCalculationRule(ShipmentDateCalculationRule.builder()
                                    .withHourBefore(13)
                                    .withRuleForLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                                            .withDaysToAdd(1)
                                            .withBaseDateForCalculation(ORDER_CREATION_DATE)
                                            .build())
                                    .withRuleForNonLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                                            .withDaysToAdd(-1)
                                            .withBaseDateForCalculation(DELIVERY_DATE)
                                            .build())
                                    .withHolidays(singletonList(LocalDate.of(2020, 5, 13)))
                                    .build())
                            .build());
                }},
                expected
        );
    }

    @Test
    public void testSerializePrepaid() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new ShopOrder() {{
                    setBusinessId(1L);
                    setId(1234L);
                    setCurrency(Currency.RUR);
                    setPaymentType(PaymentType.PREPAID);
                    setItems(
                            Arrays.asList(
                                    item1,
                                    item2
                            )
                    );
                    setDeliveryWithRegion(delivery);
                    setNotes("notes-notes-notes");
                }},
                "{'order': {" +
                        "   'businessId': 1," +
                        "   'id': 1234," +
                        "   'currency': 'RUR'," +
                        "   'paymentType': 'PREPAID'," +
                        "   'subsidyTotal': 0," +
                        "   'items': ['item1', 'item2']," +
                        "   'delivery': 'delivery'," +
                        "   'notes': 'notes-notes-notes'" +
                        "}}"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new ShopOrder(),
                "{'order': {'subsidyTotal': 0}}"
        );
    }

    @Test
    public void testSerializeWithSubstatus() throws Exception {
        ShopOrder shopOrder = ShopOrderProvider.prepareOrder(delivery, null);

        JsonTestUtil.assertJsonSerialize(
                serializer, shopOrder,
                "{'order': {" +
                        "   'businessId': 1," +
                        "   'id': 1234," +
                        "   'status': 'CANCELLED'," +
                        "   'substatus': 'USER_CHANGED_MIND'," +
                        "   'creationDate': '06-07-2013 15:30:40'," +
                        "   'currency': 'RUR'," +
                        "   'itemsTotal': 10.75," +
                        "   'total': 11.43," +
                        "   'paymentType': 'PREPAID'," +
                        "   'paymentMethod': 'CARD_ON_DELIVERY'," +
                        "   'fake': true," +
                        "   'context': SANDBOX," +
                        "   'subsidyTotal': 0," +
                        "   'delivery': 'delivery'," +
                        "   'buyer': {" +
                        "       'id': '1234567890'," +
                        "       'lastName': 'Tolstoy'," +
                        "       'firstName': 'Leo'," +
                        "       'middleName': 'Nikolaevich'," +
                        "       'phone': '+71234567891'," +
                        "       'email': 'a@b.com'," +
                        "       'uid': 359953025" +
                        "   }" +
                        "}}"
        );
    }

    @Test
    public void testSerializeWithAcceptCode() throws Exception {
        ShopOrder shopOrder = ShopOrderProvider.prepareOrder(delivery, null);
        shopOrder.setElectronicAcceptanceCertificateCode("123QWEasd");

        JsonTestUtil.assertJsonSerialize(
                serializer, shopOrder,
                "{'order': {" +
                        "   'businessId': 1," +
                        "   'id': 1234," +
                        "   'status': 'CANCELLED'," +
                        "   'substatus': 'USER_CHANGED_MIND'," +
                        "   'creationDate': '06-07-2013 15:30:40'," +
                        "   'currency': 'RUR'," +
                        "   'itemsTotal': 10.75," +
                        "   'total': 11.43," +
                        "   'paymentType': 'PREPAID'," +
                        "   'paymentMethod': 'CARD_ON_DELIVERY'," +
                        "   'fake': true," +
                        "   'context': SANDBOX," +
                        "   'subsidyTotal': 0," +
                        "   'delivery': 'delivery'," +
                        "   'electronicAcceptanceCertificateCode': '123QWEasd'," +
                        "   'buyer': {" +
                        "       'id': '1234567890'," +
                        "       'lastName': 'Tolstoy'," +
                        "       'firstName': 'Leo'," +
                        "       'middleName': 'Nikolaevich'," +
                        "       'phone': '+71234567891'," +
                        "       'email': 'a@b.com'," +
                        "       'uid': 359953025" +
                        "   }" +
                        "}}"
        );
    }

    @Test
    public void testSerializeWithVehicle() throws Exception {
        ShopOrder shopOrder = ShopOrderProvider.prepareOrder(delivery, null);

        JsonTestUtil.assertJsonSerialize(
                serializer, shopOrder,
                "{'order': {" +
                        "   'businessId': 1," +
                        "   'id': 1234," +
                        "   'status': 'CANCELLED'," +
                        "   'substatus': 'USER_CHANGED_MIND'," +
                        "   'creationDate': '06-07-2013 15:30:40'," +
                        "   'currency': 'RUR'," +
                        "   'itemsTotal': 10.75," +
                        "   'total': 11.43," +
                        "   'paymentType': 'PREPAID'," +
                        "   'paymentMethod': 'CARD_ON_DELIVERY'," +
                        "   'fake': true," +
                        "   'context': SANDBOX," +
                        "   'subsidyTotal': 0," +
                        "   'delivery': 'delivery'," +
                        "   'buyer': {" +
                        "       'id': '1234567890'," +
                        "       'lastName': 'Tolstoy'," +
                        "       'firstName': 'Leo'," +
                        "       'middleName': 'Nikolaevich'," +
                        "       'phone': '+71234567891'," +
                        "       'email': 'a@b.com'," +
                        "       'uid': 359953025" +
                        "   }" +
                        "}}"
        );
    }

    @Test
    public void testSerializeWithCourier() throws Exception {
        ShopOrder shopOrder = ShopOrderProvider.prepareOrder(delivery, null);

        JsonTestUtil.assertJsonSerialize(
                serializer, shopOrder,
                "{'order': {" +
                        "   'businessId': 1," +
                        "   'id': 1234," +
                        "   'status': 'CANCELLED'," +
                        "   'substatus': 'USER_CHANGED_MIND'," +
                        "   'creationDate': '06-07-2013 15:30:40'," +
                        "   'currency': 'RUR'," +
                        "   'itemsTotal': 10.75," +
                        "   'total': 11.43," +
                        "   'paymentType': 'PREPAID'," +
                        "   'paymentMethod': 'CARD_ON_DELIVERY'," +
                        "   'fake': true," +
                        "   'context': SANDBOX," +
                        "   'subsidyTotal': 0," +
                        "   'delivery': 'delivery'," +
                        "   'buyer': {" +
                        "       'id': '1234567890'," +
                        "       'lastName': 'Tolstoy'," +
                        "       'firstName': 'Leo'," +
                        "       'middleName': 'Nikolaevich'," +
                        "       'phone': '+71234567891'," +
                        "       'email': 'a@b.com'," +
                        "       'uid': 359953025" +
                        "   }" +
                        "}}"
        );
    }

    @Test
    public void testSerializeWithSberId() throws Exception {
        ShopOrder shopOrder = ShopOrderProvider.prepareOrderWithSberId(delivery, null);

        JsonTestUtil.assertJsonSerialize(
                serializer, shopOrder,
                "{'order': {" +
                        "   'businessId': 1," +
                        "   'id': 1234," +
                        "   'status': 'CANCELLED'," +
                        "   'substatus': 'USER_CHANGED_MIND'," +
                        "   'creationDate': '06-07-2013 15:30:40'," +
                        "   'currency': 'RUR'," +
                        "   'itemsTotal': 10.75," +
                        "   'total': 11.43," +
                        "   'paymentType': 'PREPAID'," +
                        "   'paymentMethod': 'CARD_ON_DELIVERY'," +
                        "   'fake': true," +
                        "   'context': SANDBOX," +
                        "   'subsidyTotal': 0," +
                        "   'delivery': 'delivery'," +
                        "   'buyer': {" +
                        "       'id': '1234567890'," +
                        "       'lastName': 'Tolstoy'," +
                        "       'firstName': 'Leo'," +
                        "       'middleName': 'Nikolaevich'," +
                        "       'phone': '+71234567891'," +
                        "       'email': 'a@b.com'," +
                        "       'uid': 2305843009213693951" +
                        "   }" +
                        "}}"
        );
    }

    @Test
    public void testSerializeWithoutSubstatus() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new ShopOrder() {{
                    setBusinessId(1L);
                    setStatus(OrderStatus.DELIVERED);
                }},
                "{'order': {'businessId': 1, 'status': 'DELIVERED', 'subsidyTotal': 0}}"
        );
    }

    @Test
    public void testSerializeTotalWithSubsidy() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new ShopOrder() {{
                setBusinessId(1L);
                setStatus(OrderStatus.DELIVERED);
                setTotalWithSubsidy(BigDecimal.valueOf(999));
            }},
            "{'order': {'businessId': 1, 'status': 'DELIVERED', 'subsidyTotal': 0, 'totalWithSubsidy': 999}}"
        );
    }

    @Test
    public void testSerializeBuyerItemsTotal() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new ShopOrder() {{
                    setBusinessId(1L);
                    setStatus(OrderStatus.DELIVERED);
                    setBuyerTotalBeforeDiscount(BigDecimal.valueOf(10000));
                    setBuyerItemsTotalBeforeDiscount(BigDecimal.valueOf(9800));
                    setBuyerItemsTotal(BigDecimal.valueOf(9500));
                    setBuyerTotal(BigDecimal.valueOf(9700));
                }},
                "{'order': {'businessId': 1, 'status': 'DELIVERED', 'subsidyTotal': 0," +
                        " 'buyerItemsTotal': 9500, 'buyerItemsTotalBeforeDiscount': 9800, 'buyerTotal': 9700," +
                        " 'buyerTotalBeforeDiscount': 10000}}"
        );
    }

    @Test
    @DisplayName("тест полной сериализации заказа")
    public void testSerializeFullShopOrder() throws Exception {
        CheckoutDateFormat dateFormat = new CheckoutDateFormat();
        DeliveryWithRegionJsonSerializer deliverySerializer = new DeliveryWithRegionJsonSerializer();
        deliverySerializer.setAddressJsonSerializer(new AddressJsonSerializer());
        deliverySerializer.setCheckoutDateFormat(dateFormat);
        deliverySerializer.setCourierJsonSerializer(new CourierJsonSerializer());
        deliverySerializer.setParcelJsonSerializer(new ParcelJsonSerializer());
        deliverySerializer.setCheckoutTimeFormat(new CheckoutTimeFormat());
        ShopOrderItemJsonSerializer orderItemSerializer = new ShopOrderItemJsonSerializer(new OrderItemPromoJsonSerializer(),
                                                                                          new OrderItemInstanceJsonSerializer());
        ShopOrderJsonSerializer fullSerializer = new ShopOrderJsonSerializer();
        fullSerializer.setBuyerJsonSerializer(new BuyerJsonSerializer());
        fullSerializer.setItemJsonSerializer(orderItemSerializer);
        fullSerializer.setStubExtraContextJsonSerializer(new StubExtraContextJsonSerializer(new ShipmentDateCalculationRuleJsonSerializer()));
        fullSerializer.setDeliveryWithRegionJsonSerializer(deliverySerializer);
        fullSerializer.setCheckoutDateFormat(dateFormat);

        JsonTestUtil.assertJsonSerialize(
                fullSerializer,
                ShopOrderTestData.get(),
                IOUtils.readInputStream(this.getClass().getResourceAsStream("/files/pushapi/fullShopOrder.json"))
        );
    }
}
