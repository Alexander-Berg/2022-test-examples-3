package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author mmetlov
 */
class ParcelXmlDeserializerTest {

    private ParcelXmlDeserializer deserializer;

    @BeforeEach
    void setUp() {
        OrderShipmentBoxXmlDeserializer orderShipmentBoxXmlDeserializer = new OrderShipmentBoxXmlDeserializer();
        OrderShipmentItemXmlDeserializer orderShipmentItemXmlDeserializer = new OrderShipmentItemXmlDeserializer();
        deserializer = new ParcelXmlDeserializer(orderShipmentBoxXmlDeserializer, orderShipmentItemXmlDeserializer);
    }

    @Test
    void testParse() throws Exception {
        Parcel actual = XmlTestUtil.deserialize(
                deserializer,
                "<shipment " +
                        "   id='123'" +
                        "   shipmentDate='17-01-2020'" +
                        "   shipmentTime='17:24'" +
                        "   status='NEW'" +
                        "   weight='500'" +
                        "   width='100'" +
                        "   height='200'" +
                        "   depth='50'/>");
        assertEquals(123L, actual.getId().longValue());
        assertEquals(LocalDate.of(2020, 1, 17), actual.getShipmentDate());
        assertEquals(LocalDateTime.of(2020, 1, 17, 17, 24), actual.getShipmentTime());
        assertEquals(Long.valueOf(500L), actual.getWeight());
        assertEquals(Long.valueOf(100L), actual.getWidth());
        assertEquals(Long.valueOf(200L), actual.getHeight());
        assertEquals(Long.valueOf(50L), actual.getDepth());
        assertEquals(ParcelStatus.NEW, actual.getStatus());
        assertNull(actual.getLabelURL());
        assertNull(actual.getShopShipmentId());
    }

    @Test
    void testParseEmpty() throws Exception {
        Parcel actual = XmlTestUtil.deserialize(
                deserializer,
                "<shipment />");

        assertNull(actual.getId());
        assertNull(actual.getWeight());
        assertNull(actual.getWidth());
        assertNull(actual.getHeight());
        assertNull(actual.getDepth());
        assertNull(actual.getStatus());
        assertNull(actual.getLabelURL());
        assertNull(actual.getShopShipmentId());
    }

    @Test
    void testParseBox() throws Exception {
        Parcel actual = XmlTestUtil.deserialize(
                deserializer,
                "<shipment " +
                        "   id='123'" +
                        "   status='NEW'" +
                        "   weight='500'" +
                        "   width='100'" +
                        "   height='200'" +
                        "   depth='50'>" +
                        "   <boxes>" +
                        "     <box " +
                        "       id='456'" +
                        "       weight='1'" +
                        "       width='2'" +
                        "       height='3'" +
                        "       depth='4'" +
                        "     />" +
                        "   </boxes>" +
                        "</shipment>");

        assertEquals(1, actual.getBoxes().size());
        ParcelBox box = actual.getBoxes().get(0);
        assertEquals(456, box.getId().longValue());
        assertEquals(1, box.getWeight().longValue());
        assertEquals(2, box.getWidth().longValue());
        assertEquals(3, box.getHeight().longValue());
        assertEquals(4, box.getDepth().longValue());
    }

    @Test
    void testParseBoxItem() throws Exception {
        Parcel actual = XmlTestUtil.deserialize(
                deserializer,
                "<shipment " +
                        "   id='123'" +
                        "   status='NEW'" +
                        "   weight='500'" +
                        "   width='100'" +
                        "   height='200'" +
                        "   depth='50'>" +
                        "   <boxes>" +
                        "     <box " +
                        "       id='456'" +
                        "       weight='1'" +
                        "       width='2'" +
                        "       height='3'" +
                        "       depth='4'>" +
                        "       <items>" +
                        "         <item id='123456' count='47'/>" +
                        "       </items>" +
                        "     </box>" +
                        "   </boxes>" +
                        "</shipment>");

        assertEquals(1, actual.getBoxes().get(0).getItems().size());
        ParcelBoxItem boxItem = actual.getBoxes().get(0).getItems().get(0);
        assertEquals(123456, boxItem.getItemId());
        assertEquals(47, boxItem.getCount());
    }

    @Test
    void testParcelItem() throws Exception {
        Parcel actual = XmlTestUtil.deserialize(
                deserializer,
                "<shipment>" +
                        "   <items>" +
                        "     <item " +
                        "       itemId='2'" +
                        "       shipmentDateTimeBySupplier='01-01-2020 12:30:00'" +
                        "     />" +
                        "   </items>" +
                        "</shipment>");

        assertEquals(1, actual.getParcelItems().size());
        ParcelItem item = actual.getParcelItems().get(0);
        assertEquals(2, item.getItemId().longValue());
        assertEquals(LocalDateTime.of(2020, Month.JANUARY, 1, 12, 30), item.getShipmentDateTimeBySupplier());
    }
}
