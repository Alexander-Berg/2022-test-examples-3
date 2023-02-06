package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.sameXmlAs;

class ParcelXmlSerializerTest {

    private final ParcelXmlSerializer serializer = new ParcelXmlSerializer();

    private Parcel parcel;

    @BeforeEach
    void setUp() {
        parcel = buildParcel();
    }

    @Test
    void shouldSerializeParcel() throws Exception {
        assertThat(XmlTestUtil.serialize(serializer, parcel), sameXmlAs(
                "<shipment id='1234' shipmentDate='21-01-2020' shipmentTime='12:35' weight='4' height='2' width='3' depth='1' status='NEW'>" +
                        "  <boxes/>" +
                        "  <items/>" +
                        "</shipment>"
                )
        );
    }

    @Test
    void shouldSerializeParcelBox() throws Exception {
        parcel.setBoxes(Collections.singletonList(buildParcelBox()));
        assertThat(XmlTestUtil.serialize(serializer, parcel), sameXmlAs(
                "<shipment id='1234' shipmentDate='21-01-2020' shipmentTime='12:35' weight='4' height='2' width='3' depth='1' status='NEW'>" +
                        "   <boxes>" +
                        "     <box " +
                        "       id='456'" +
                        "       weight='1'" +
                        "       width='2'" +
                        "       height='3'" +
                        "       depth='4'>" +
                        "         <items/>" +
                        "     </box>" +
                        "   </boxes>" +
                        "   <items/>" +
                        "</shipment>"
                )
        );
    }

    @Test
    void shouldSerializeParcelBoxItem() throws Exception {
        ParcelBox box = buildParcelBox();
        box.setItems(Collections.singletonList(new ParcelBoxItem() {{
            setItemId(78);
            setCount(41);
        }}));
        parcel.setBoxes(Collections.singletonList(box));
        assertThat(XmlTestUtil.serialize(serializer, parcel), sameXmlAs(
                "<shipment id='1234' shipmentDate='21-01-2020' shipmentTime='12:35' weight='4' height='2' width='3' depth='1' status='NEW'>" +
                        "   <boxes>" +
                        "     <box " +
                        "       id='456'" +
                        "       weight='1'" +
                        "       width='2'" +
                        "       height='3'" +
                        "       depth='4'>" +
                        "         <items>" +
                        "           <item id='78' count='41'/>" +
                        "         </items>" +
                        "     </box>" +
                        "   </boxes>" +
                        "   <items/>" +
                        "</shipment>"
                )
        );
    }

    @Test
    void shouldSerializeParcelItem() throws Exception {
        parcel.setParcelItems(Collections.singletonList(buildParcelItem()));
        assertThat(XmlTestUtil.serialize(serializer, parcel), sameXmlAs(
                "<shipment id='1234' shipmentDate='21-01-2020' shipmentTime='12:35' weight='4' height='2' width='3' depth='1' status='NEW'>" +
                        "   <boxes/>" +
                        "   <items>" +
                        "     <item " +
                        "       itemId='2'" +
                        "       shipmentDateTimeBySupplier='01-01-2020 12:30:00'" +
                        "     />" +
                        "   </items>" +
                        "</shipment>"
                )
        );
    }

    @Nonnull
    private ParcelBox buildParcelBox() {
        return new ParcelBox() {{
            setId(456L);
            setWeight(1L);
            setWidth(2L);
            setHeight(3L);
            setDepth(4L);
        }};
    }

    @Nonnull
    private ParcelItem buildParcelItem() {
        return new ParcelItem() {{
            setItemId(2L);
            setShipmentDateTimeBySupplier(LocalDateTime.of(2020, Month.JANUARY, 1, 12, 30));
        }};
    }

    @Nonnull
    private Parcel buildParcel() {
        return new Parcel() {{
            setId(1234L);
            setShipmentDate(LocalDate.of(2020, 1, 21));
            setShipmentTime(LocalDateTime.of(2020, 1, 21,12,35));
            setWeight(4L);
            setWidth(3L);
            setHeight(2L);
            setDepth(1L);
            setStatus(ParcelStatus.NEW);
        }};
    }
}
