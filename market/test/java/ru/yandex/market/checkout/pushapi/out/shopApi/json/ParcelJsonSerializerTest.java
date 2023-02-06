package ru.yandex.market.checkout.pushapi.out.shopApi.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * @author mmetlov
 */
public class ParcelJsonSerializerTest {

    private final static String PARCEL_ATTR = "'id': 123," +
            "'weight': 1000," +
            "'width': 100," +
            "'height': 200," +
            "'depth': 50," +
            "'status': 'NEW',";
    private final static String PARCEL_BOX_ATTR = "'id': 456," +
            "'weight': 100," +
            "'width': 1," +
            "'height': 2," +
            "'depth': 3,";

    private ParcelJsonSerializer serializer = new ParcelJsonSerializer();
    private Parcel parcel;

    @BeforeEach
    public void setUp() {
        parcel = buildParcel();
    }

    @Test
    public void testSerialize() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                parcel,
                "{" +
                        PARCEL_ATTR +
                        "'boxes' : []" +
                        "}");
    }

    @Test
    public void testSerializeShipmentDate() throws Exception {
        parcel.setShipmentDate(LocalDate.of(2020, 1, 17));
        JsonTestUtil.assertJsonSerialize(
                serializer,
                parcel,
                "{" +
                        PARCEL_ATTR +
                        "'boxes' : []," +
                        "'shipmentDate' : '17-01-2020'" +
                        "}");
    }

    @Test
    public void testSerializeShipmentTime() throws Exception {
        parcel.setShipmentTime(LocalDateTime.of(2021, 10, 25, 18, 25));
        JsonTestUtil.assertJsonSerialize(
                serializer,
                parcel,
                "{" +
                        PARCEL_ATTR +
                        "'boxes' : []," +
                        "'shipmentTime' : '18:25'" +
                        "}");
    }

    @Test
    public void testSerializeBoxes() throws Exception {
        ParcelBox parcelBox = buildParcelBox();
        parcel.setBoxes(Collections.singletonList(parcelBox));
        JsonTestUtil.assertJsonSerialize(
                serializer,
                parcel,
                "{" +
                        PARCEL_ATTR +
                        "'boxes' : [" +
                        "    {" +
                        PARCEL_BOX_ATTR +
                        "    'items': []" +
                        "    }" +
                        "]" +
                        "}");
    }

    @Test
    public void testSerializeBoxItems() throws Exception {
        ParcelBox parcelBox = buildParcelBox();
        ParcelBoxItem boxItem = new ParcelBoxItem() {{
            setItemId(123456L);
            setCount(4);
        }};
        parcelBox.setItems(Collections.singletonList(boxItem));
        parcel.setBoxes(Collections.singletonList(parcelBox));
        JsonTestUtil.assertJsonSerialize(
                serializer,
                parcel,
                "{" +
                        PARCEL_ATTR +
                        "'boxes' : [" +
                        "    {" +
                        PARCEL_BOX_ATTR +
                        "    'items': [" +
                        "        {" +
                        "        'id': 123456," +
                        "        'count': 4" +
                        "        }" +
                        "    ]" +
                        "    }" +
                        "]" +
                        "}");
    }

    @Nonnull
    private ParcelBox buildParcelBox() {
        return new ParcelBox() {{
            setId(456L);
            setWeight(100L);
            setWidth(1L);
            setHeight(2L);
            setDepth(3L);
        }};
    }


    @Nonnull
    private Parcel buildParcel() {
        return new Parcel() {{
            setId(123L);
            setWeight(1000L);
            setWidth(100L);
            setHeight(200L);
            setDepth(50L);
            setStatus(ParcelStatus.NEW);

            setLabelURL("http://mds.yandex.ru/label.pdf");
            setShipmentId(456L);
        }};
    }
}
