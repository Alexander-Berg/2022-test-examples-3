package ru.yandex.market.checkout.pushapi.client.xml;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.AddressXmlSerializer;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
@SuppressWarnings({"rawtypes", "unchecked"})
public class DeliveryXmlSerializerTest {

    private static final EnhancedRandom RANDOM = EnhancedRandomHelper.createEnhancedRandom();

    private final Address address = mock(Address.class);
    private final Parcel parcel1 = new Parcel() {{
        setId(111L);
    }};

    private final Parcel parcel2 = new Parcel() {{
        setId(222L);
    }};

    private final DeliveryXmlSerializer serializer = new DeliveryXmlSerializer();

    @BeforeEach
    public void setUp() throws Exception {
        serializer.setAddressXmlSerializer(
                new AddressXmlSerializer() {
                    @Override
                    public void serializeXml(Address value, PrimitiveXmlWriter writer) throws IOException {
                        assertEquals(address, value);
                        writer.addNode("shop-address", "address");
                    }
                }
        );
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
        serializer.setParcelXmlSerializer(
                new ParcelXmlSerializer() {
                    @Override
                    public void serializeXml(Parcel value, PrimitiveXmlWriter writer) throws IOException {
                        assertThat(value, is(anyOf(equalTo(parcel1), equalTo(parcel2))));
                        writer.addNode("shipment", value.getId());
                    }
                }
        );
    }

    @RepeatedTest(10)
    public void testSerializeInternalCartRequest() throws Exception {
        Delivery delivery = RANDOM.nextObject(Delivery.class,
                "address",
                "buyerAddress",
                "price",
                "serviceName",
                "id",
                "shopDeliveryId",
                "type",
                "deliveryOptionId",
                "hash",
                "deliveryServiceId",
                "deliveryPartnerType",
                "deliveryDates",
                "validatedDeliveryDates",
                "outlets",
                "outletId",
                "outletIds",
                "outletCodes",
                "outletCode",
                "paymentOptions",
                "shipment",
                "shipments",
                "parcels"
        );
        delivery.setRegionId(213L);
        delivery.setShopAddress(address);
        delivery.setVat(VatType.VAT_10);
        delivery.setLiftType(LiftType.FREE);
        delivery.setLiftPrice(null);
        delivery.setMarketBranded(false);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery lift-type='FREE' region-id='213' vat='VAT_10'>" +
                        "    <shop-address>address</shop-address>" +
                        "</delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeInternalCartResponse() throws Exception {
        Delivery delivery = prepareSerializeInternalCartResponse();
        delivery.setOutletIds(Set.of(3456L, 4567L));
        delivery.setOutletCodes(null);
        delivery.getRawDeliveryIntervals()
                .add(new RawDeliveryInterval(
                        XmlTestUtil.date("2013-05-20"),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        true));
        delivery.setLiftType(null);
        delivery.setMarketBranded(true);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      service-name='pochta' vat='NO_VAT'" +
                        "      market-branded='true'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013'>" +
                        "        <interval date='20-05-2013' from-time='09:00' to-time='10:00' default='true' />" +
                        "    </dates>" +
                        "    <outlets>" +
                        "        <outlet id='3456' />" +
                        "        <outlet id='4567' />" +
                        "    </outlets>" +
                        "</delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeInternalCartResponseWithOutletCode() throws Exception {
        Delivery delivery = prepareSerializeInternalCartResponse();
        delivery.setOutletIds((Set) null);
        delivery.setOutletCodes(Set.of("3456str", "4567str"));
        delivery.setLiftType(null);
        delivery.setMarketBranded(false);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      service-name='pochta' vat='NO_VAT'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <outlets>" +
                        "        <outlet code='3456str' />" +
                        "        <outlet code='4567str' />" +
                        "    </outlets>" +
                        "</delivery>"
        );
    }

    private Delivery prepareSerializeInternalCartResponse() throws Exception {
        Delivery delivery = RANDOM.nextObject(Delivery.class,
                "address",
                "buyerAddress",
                "shopAddress",
                "id",
                "serviceName",
                "regionId",
                "deliveryOptionId",
                "deliveryServiceId",
                "hash",
                "deliveryPartnerType",
                "validatedDeliveryDates",
                "paymentOptions",
                "shipment",
                "shipments",
                "parcels"
        );
        delivery.setShopDeliveryId("12345");
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setPrice(new BigDecimal(2345));
        delivery.setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
        delivery.setServiceName("pochta");
        delivery.setVat(VatType.NO_VAT);
        delivery.setLiftType(LiftType.FREE);
        delivery.setLiftPrice(null);
        return delivery;
    }

    @RepeatedTest(10)
    public void testSerializeInternalAcceptRequest() throws Exception {
        Delivery delivery = prepareSerializeInternalAcceptRequest();
        delivery.setOutletId(3456L);
        delivery.setOutletCode(null);
        delivery.setLiftType(null);
        delivery.setMarketBranded(true);

        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setScheduleString("schedule-string");
        shopOutlet.setPostcode("postcode");
        shopOutlet.setCity("city");
        shopOutlet.setStreet("street");
        shopOutlet.setHouse("house");
        shopOutlet.setBlock("block");
        delivery.setOutlet(shopOutlet);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta' vat='VAT_18'" +
                        "      market-branded='true'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "        <outlet id='3456' " +
                        "                schedule-string='schedule-string' " +
                        "                postcode='postcode' " +
                        "                city='city' "+
                        "                house='house' " +
                        "                block='block' " +
                        "                street='street' />" +
                        "    <shipment>111</shipment>" +
                        "    <shipments>" +
                        "       <shipment>111</shipment>" +
                        "       <shipment>222</shipment>" +
                        "    </shipments>" +
                        "</delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeInternalAcceptRequestWithOutletCode() throws Exception {
        Delivery delivery = prepareSerializeInternalAcceptRequest();
        delivery.setOutletCode("3456str");
        delivery.setOutletId(null);
        delivery.setLiftType(null);
        delivery.setMarketBranded(false);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta' vat='VAT_18'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet code='3456str' />" +
                        "    <shipment>111</shipment>" +
                        "    <shipments>" +
                        "       <shipment>111</shipment>" +
                        "       <shipment>222</shipment>" +
                        "    </shipments>" +
                        "</delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeInternalAcceptRequestWithDeliverySubsidy() throws Exception {
        Delivery delivery = prepareSerializeInternalAcceptRequest();
        delivery.getPrices().setSubsidy(BigDecimal.valueOf(13));
        delivery.setOutletId(3456L);
        delivery.setOutletCode(null);
        delivery.setLiftType(null);
        delivery.setMarketBranded(false);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta' vat='VAT_18'>" +
                        "    <prices subsidy='13'/>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet id='3456' />" +
                        "    <shipment>111</shipment>" +
                        "    <shipments>" +
                        "       <shipment>111</shipment>" +
                        "       <shipment>222</shipment>" +
                        "    </shipments>" +
                        "</delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithLiftingOptionsUnknown() throws Exception {
        Delivery delivery = prepareSerializeInternalAcceptRequest();
        delivery.getPrices().setSubsidy(BigDecimal.valueOf(13));
        delivery.setOutletId(3456L);
        delivery.setOutletCode(null);
        delivery.setLiftType(LiftType.UNKNOWN);
        delivery.setMarketBranded(false);

        String expectedResult =
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'" +
                        "       vat='VAT_18'>" +
                        "    <prices subsidy='13'/>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet id='3456' />" +
                        "    <shipment>111</shipment>" +
                        "    <shipments>" +
                        "       <shipment>111</shipment>" +
                        "       <shipment>222</shipment>" +
                        "    </shipments>" +
                        "</delivery>";

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                expectedResult
        );

        delivery.setLiftType(null);
        delivery.setLiftPrice(BigDecimal.valueOf(150L));

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                expectedResult
        );
    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithLiftingOptionsFree() throws Exception {
        Delivery delivery = prepareSerializeInternalAcceptRequest();
        delivery.getPrices().setSubsidy(BigDecimal.valueOf(13));
        delivery.setOutletId(3456L);
        delivery.setOutletCode(null);
        delivery.setLiftType(LiftType.FREE);
        delivery.setLiftPrice(BigDecimal.ZERO);
        delivery.setBuyerAddress(null);
        delivery.setMarketBranded(false);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta' vat='VAT_18'" +
                        "      lift-type='FREE' lift-price='0'>" +
                        "    <prices subsidy='13'/>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet id='3456' />" +
                        "    <shipment>111</shipment>" +
                        "    <shipments>" +
                        "       <shipment>111</shipment>" +
                        "       <shipment>222</shipment>" +
                        "    </shipments>" +
                        "</delivery>"
        );
    }


    @RepeatedTest(10)
    public void testSerializeCartRequestWithLiftingOptionsWithoutAddress() throws Exception {
        Delivery delivery = prepareSerializeInternalAcceptRequest();
        delivery.getPrices().setSubsidy(BigDecimal.valueOf(13));
        delivery.setOutletId(3456L);
        delivery.setOutletCode(null);
        delivery.setLiftType(LiftType.NOT_NEEDED);
        delivery.setLiftPrice(BigDecimal.ZERO);
        delivery.setBuyerAddress(null);
        delivery.setMarketBranded(true);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta' vat='VAT_18'" +
                        "      lift-type='NOT_NEEDED' lift-price='0'" +
                        "      market-branded='true'>" +
                        "    <prices subsidy='13'/>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet id='3456' />" +
                        "    <shipment>111</shipment>" +
                        "    <shipments>" +
                        "       <shipment>111</shipment>" +
                        "       <shipment>222</shipment>" +
                        "    </shipments>" +
                        "</delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithLiftingOptions() throws Exception {
        Delivery delivery = prepareSerializeInternalAcceptRequest();
        delivery.getPrices().setSubsidy(BigDecimal.valueOf(13));
        delivery.setOutletId(3456L);
        delivery.setOutletCode(null);
        delivery.setLiftType(LiftType.ELEVATOR);
        delivery.setLiftPrice(BigDecimal.valueOf(150L));
        delivery.setMarketBranded(true);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                delivery,
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta' vat='VAT_18'" +
                        "      lift-type='ELEVATOR' lift-price='150'" +
                        "      market-branded='true'>" +
                        "    <prices subsidy='13'/>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet id='3456' />" +
                        "    <shipment>111</shipment>" +
                        "    <shipments>" +
                        "       <shipment>111</shipment>" +
                        "       <shipment>222</shipment>" +
                        "    </shipments>" +
                        "</delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new Delivery(),
                "<delivery />"
        );
    }

    private Delivery prepareSerializeInternalAcceptRequest() throws Exception {
        Delivery delivery = RANDOM.nextObject(Delivery.class,
                "address", "buyerAddress", "shopAddress",
                "hash", "id", "shopDeliveryId", "deliveryOptionId", "deliveryServiceId", "deliveryPartnerType",
                "validatedDeliveryDates", "outlet", "outlets", "outletIds", "outletCodes", "paymentOptions", "shipment",
                "parcels.route", "shipments.route"
        );
        delivery.setShopDeliveryId("12345");
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setPrice(new BigDecimal(2345));
        delivery.setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));

        delivery.setRegionId(213L);
        delivery.setServiceName("pochta");
        delivery.setShopAddress(address);
        delivery.setParcels(Arrays.asList(parcel1, parcel2));
        delivery.setVat(VatType.VAT_18);
        delivery.setLiftType(LiftType.FREE);
        delivery.setLiftPrice(null);
        return delivery;
    }
}
