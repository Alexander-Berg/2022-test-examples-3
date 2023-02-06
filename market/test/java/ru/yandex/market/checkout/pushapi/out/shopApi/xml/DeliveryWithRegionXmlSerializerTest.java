package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.pushapi.client.entity.DispatchType;
import ru.yandex.market.checkout.pushapi.client.entity.Region;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.ParcelXmlSerializer;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;

import static org.mockito.Mockito.mock;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
public class DeliveryWithRegionXmlSerializerTest {

    private final AddressShopXmlSerializer addressShopXmlSerializer = mock(AddressShopXmlSerializer.class);
    private final DeliveryWithRegionXmlSerializer serializer = new DeliveryWithRegionXmlSerializer();
    private final ParcelXmlSerializer parcelXmlSerializer = mock(ParcelXmlSerializer.class);
    private CourierInfoXmlSerializer courierInfoXmlSerializer = mock(CourierInfoXmlSerializer.class);
    private final Address address = mock(Address.class);
    private final Parcel orderShipment1 = mock(Parcel.class);
    private final Parcel orderShipment2 = mock(Parcel.class);
    private Courier courier = mock(Courier.class);
    private final EnhancedRandom enhancedRandom = EnhancedRandomHelper.createEnhancedRandom();

    @BeforeEach
    public void setUp() throws Exception {
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
        XmlTestUtil.initMockSerializer(
                addressShopXmlSerializer, address,
                writer -> writer.addNode("address", "address")
        );

        XmlTestUtil.initMockSerializer(
                parcelXmlSerializer, orderShipment1,
                writer -> writer.addNode("shipment", "shipment-1")
        );

        XmlTestUtil.initMockSerializer(
                parcelXmlSerializer, orderShipment2,
                writer -> writer.addNode("shipment", "shipment-2")
        );
        XmlTestUtil.initMockSerializer(
                courierInfoXmlSerializer,
                courier,
                writer -> writer.addNode("courier", "courier")
        );
        serializer.setAddressShopXmlSerializer(addressShopXmlSerializer);
        serializer.setParcelXmlSerializer(parcelXmlSerializer);
        serializer.setCourierXmlSerializer(courierInfoXmlSerializer);
    }

    @RepeatedTest(10)
    public void testSerializeAcceptRequest() throws Exception {
        DeliveryWithRegion deliveryWithRegion = prepareSerializeAcceptRequest();
        deliveryWithRegion.setOutletId(2345L);
        deliveryWithRegion.setOutletCode(null);
        deliveryWithRegion.setOutletIds((Set)null);
        deliveryWithRegion.setOutletCodes(null);
        deliveryWithRegion.setLiftType(null);
        deliveryWithRegion.setCourier(courier);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                "    <delivery id='12345'" +
                        "          type='DELIVERY'" +
                        "          price='1234'" +
                        "          dispatchType='UNKNOWN' " +
                        "          service-name='pochta'>" +
                        "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <outlet id='2345' />" +
                        "        <courier>courier</courier>" +
                        "    </delivery>"
        );

    }

    @RepeatedTest(10)
    public void testSerializeStatusRequest() throws Exception {
        DeliveryWithRegion deliveryWithRegion = enhancedRandom.nextObject(DeliveryWithRegion.class,
                "shopDeliveryId", "hash", "deliveryPartnerType", "regionId", "deliveryServiceId", "validatedDeliveryDates", "vat",
                "parcels.route", "shipments.route", "shipment.route");
        deliveryWithRegion.setId("12345");
        deliveryWithRegion.setType(DeliveryType.DELIVERY);
        deliveryWithRegion.setPrice(new BigDecimal(1234L));
        deliveryWithRegion.setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
        deliveryWithRegion.setServiceName("pochta");
        deliveryWithRegion.setRegion(new Region(123, "Москва", null, new Region(234, "Россия", null, new Region(345,
                "Земля", null))));
        deliveryWithRegion.setShopAddress(address);
        deliveryWithRegion.setOutletCode(null);
        deliveryWithRegion.setOutletIds((Set) null);
        deliveryWithRegion.setOutletCodes(null);
        deliveryWithRegion.setOutletId(2345L);
        deliveryWithRegion.setParcels(Arrays.asList(orderShipment1, orderShipment2));
        deliveryWithRegion.setLiftType(LiftType.FREE);
        deliveryWithRegion.setLiftPrice(null);
        deliveryWithRegion.setBuyerAddress(null);
        deliveryWithRegion.setDispatchType(DispatchType.UNKNOWN);
        deliveryWithRegion.setCourier(courier);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                "    <delivery id='12345'" +
                        "          type='DELIVERY'" +
                        "          price='1234'" +
                        "          dispatchType='UNKNOWN' " +
                        "          service-name='pochta'" +
                        "          lift-type='FREE'>" +
                        "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <outlet id='2345' />" +
                        "        <shipments>" +
                        "           <shipment>shipment-1</shipment>" +
                        "           <shipment>shipment-2</shipment>" +
                        "        </shipments>" +
                        "        <courier>courier</courier>" +
                        "    </delivery>"
        );

    }

    @RepeatedTest(10)
    public void testSerializeAcceptRequestOutletCode() throws Exception {
        DeliveryWithRegion deliveryWithRegion = prepareSerializeAcceptRequest();
        deliveryWithRegion.setOutletCode("str2345l");
        deliveryWithRegion.setOutletId(null);
        deliveryWithRegion.setOutletIds((Set)null);
        deliveryWithRegion.setOutletCodes(null);
        deliveryWithRegion.setLiftType(null);
        deliveryWithRegion.setCourier(courier);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                "    <delivery id='12345'" +
                        "          type='DELIVERY'" +
                        "          price='1234'" +
                        "          dispatchType='UNKNOWN' " +
                        "          service-name='pochta'>" +
                        "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <outlet code='str2345l' />" +
                        "        <courier>courier</courier>" +
                        "    </delivery>"
        );

    }


    @RepeatedTest(10)
    public void testSerializeAcceptRequestOutletIdCode() throws Exception {
        DeliveryWithRegion deliveryWithRegion = prepareSerializeAcceptRequest();
        deliveryWithRegion.setOutletCode("str2345l");
        deliveryWithRegion.setOutletId(2345L);
        deliveryWithRegion.setOutletIds((Set)null);
        deliveryWithRegion.setOutletCodes(null);
        deliveryWithRegion.setLiftType(null);
        deliveryWithRegion.setCourier(courier);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                "    <delivery id='12345'" +
                        "          type='DELIVERY'" +
                        "          price='1234'" +
                        "          dispatchType='UNKNOWN' " +
                        "          service-name='pochta'>" +
                        "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <outlet id='2345' code='str2345l' />" +
                        "        <courier>courier</courier>" +
                        "    </delivery>"
        );

    }

    private DeliveryWithRegion prepareSerializeAcceptRequest() throws Exception {
        DeliveryWithRegion deliveryWithRegion = enhancedRandom.nextObject(DeliveryWithRegion.class,
                "shopDeliveryId", "hash", "deliveryPartnerType", "regionId", "deliveryServiceId", "validatedDeliveryDates", "shipment",
                "shipments", "parcels", "vat");
        deliveryWithRegion.setId("12345");
        deliveryWithRegion.setType(DeliveryType.DELIVERY);
        deliveryWithRegion.setPrice(new BigDecimal(1234L));
        deliveryWithRegion.setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013" +
                "-05-23")));
        deliveryWithRegion.setServiceName("pochta");
        deliveryWithRegion.setRegion(new Region(123, "Москва", null, new Region(234, "Россия", null, new Region(345,
                "Земля", null))));
        deliveryWithRegion.setShopAddress(address);
        deliveryWithRegion.setLiftType(LiftType.FREE);
        deliveryWithRegion.setLiftPrice(null);
        deliveryWithRegion.setBuyerAddress(null);
        deliveryWithRegion.setDispatchType(DispatchType.UNKNOWN);
        return deliveryWithRegion;
    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithAddress() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new DeliveryWithRegion() {{
                    setRegion(new Region(123, "Москва", null, new Region(234, "Россия", null, new Region(345, "Земля", null))));
                    setShopAddress(address);
                    setCourier(courier);
                }},
                "    <delivery>" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <courier>courier</courier>" +
                        "    </delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeDeliveryWithRegionWithSubsidy() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new DeliveryWithRegion() {{
                    setPrices(new ItemPrices(){{setSubsidy(BigDecimal.TEN);}});
                }},
                "    <delivery subsidy=\"10\"/>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithoutAddress() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new DeliveryWithRegion() {{
                    setRegion(new Region(123, "Москва", null, new Region(234, "Россия", null, new Region(345, "Земля"
                            , null))));
                }},
                "    <delivery>" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "    </delivery>"
        );

    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithLiftingOptionsUnknown() throws Exception {
        DeliveryWithRegion deliveryWithRegion = prepareSerializeAcceptRequest();
        deliveryWithRegion.setOutletCode("str2345l");
        deliveryWithRegion.setOutletId(2345L);
        deliveryWithRegion.setOutletIds((Set)null);
        deliveryWithRegion.setOutletCodes(null);
        Address buyerAddress = AddressProvider.getAddress(a -> a.setType(AddressType.BUYER));
        deliveryWithRegion.setBuyerAddress(buyerAddress);
        deliveryWithRegion.setLiftType(LiftType.UNKNOWN);
        deliveryWithRegion.setLiftPrice(BigDecimal.TEN);
        deliveryWithRegion.setCourier(courier);

        String expectedResult =
                "    <delivery id='12345'" +
                        "          type='DELIVERY'" +
                        "          price='1234'" +
                        "          dispatchType='UNKNOWN' " +
                        "          service-name='pochta'>" +
                        "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <outlet id='2345' code='str2345l' />" +
                        "        <courier>courier</courier>" +
                        "    </delivery>";

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                expectedResult
        );

        deliveryWithRegion.setLiftType(null);
        deliveryWithRegion.setLiftPrice(BigDecimal.valueOf(150L));

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                expectedResult
        );
    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithLiftingOptionsWithoutFloor() throws Exception {
        DeliveryWithRegion deliveryWithRegion = prepareSerializeAcceptRequest();
        deliveryWithRegion.setOutletCode("str2345l");
        deliveryWithRegion.setOutletId(2345L);
        deliveryWithRegion.setOutletIds((Set)null);
        deliveryWithRegion.setOutletCodes(null);
        deliveryWithRegion.setBuyerAddress(null);
        deliveryWithRegion.setLiftType(LiftType.NOT_NEEDED);
        deliveryWithRegion.setLiftPrice(BigDecimal.ZERO);
        deliveryWithRegion.setCourier(courier);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                "    <delivery id='12345'" +
                        "          type='DELIVERY'" +
                        "          price='1234'" +
                        "          service-name='pochta'" +
                        "          dispatchType='UNKNOWN' " +
                        "          lift-type='NOT_NEEDED'" +
                        "          lift-price='0'>" +
                        "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <outlet id='2345' code='str2345l' />" +
                        "        <courier>courier</courier>" +
                        "    </delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithLiftingOptionsFree() throws Exception {
        DeliveryWithRegion deliveryWithRegion = prepareSerializeAcceptRequest();
        deliveryWithRegion.setOutletCode("str2345l");
        deliveryWithRegion.setOutletId(2345L);
        deliveryWithRegion.setOutletIds((Set)null);
        deliveryWithRegion.setOutletCodes(null);
        deliveryWithRegion.setBuyerAddress(null);
        deliveryWithRegion.setLiftType(LiftType.FREE);
        deliveryWithRegion.setLiftPrice(BigDecimal.TEN);
        deliveryWithRegion.setCourier(courier);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                "    <delivery id='12345'" +
                        "          type='DELIVERY'" +
                        "          price='1234'" +
                        "          service-name='pochta'" +
                        "          dispatchType='UNKNOWN' " +
                        "          lift-type='FREE'" +
                        "          lift-price='10'>" +
                        "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <outlet id='2345' code='str2345l' />" +
                        "        <courier>courier</courier>" +
                        "    </delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeCartRequestWithLiftingOptions() throws Exception {
        DeliveryWithRegion deliveryWithRegion = prepareSerializeAcceptRequest();
        deliveryWithRegion.setOutletCode("str2345l");
        deliveryWithRegion.setOutletId(2345L);
        deliveryWithRegion.setOutletIds((Set)null);
        deliveryWithRegion.setOutletCodes(null);
        deliveryWithRegion.setBuyerAddress(null);
        deliveryWithRegion.setLiftType(LiftType.ELEVATOR);
        deliveryWithRegion.setLiftPrice(BigDecimal.valueOf(150L));
        deliveryWithRegion.setCourier(courier);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                deliveryWithRegion,
                "    <delivery id='12345'" +
                        "          type='DELIVERY'" +
                        "          price='1234'" +
                        "          service-name='pochta'" +
                        "          lift-type='ELEVATOR'" +
                        "          dispatchType='UNKNOWN' " +
                        "          lift-price='150'>" +
                        "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "        <region id='123' name='Москва'>" +
                        "            <parent id='234' name='Россия'>" +
                        "                <parent id='345' name='Земля' />" +
                        "            </parent>" +
                        "        </region>" +
                        "        <address>address</address>" +
                        "        <courier>courier</courier>" +
                        "        <outlet id='2345' code='str2345l' />" +
                        "    </delivery>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new DeliveryWithRegion(),
                "<delivery />"
        );

    }
}
