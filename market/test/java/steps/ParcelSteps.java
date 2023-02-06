package steps;

import java.time.LocalDate;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;

public class ParcelSteps {

    private static final Long SHIPMENT_ID = 12L;
    private static final Long SHOP_SHIPMENT_ID = 123L;
    private static final Long WEIGHT = 1L;
    private static final Long WIDTH = 1L;
    private static final Long HEIGHT = 1L;
    private static final Long DEPTH = 1L;
    private static final ParcelStatus STATUS = ParcelStatus.CREATED;
    private static final String LABEL_URL = "https://test.url";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String ROUTE = "{\n" +
        "  \"route\": {\n" +
        "    " +
        "\"points\": [],\n" +
        "    " +
        "\"paths\": [],\n" +
        "    " +
        "\"tariff_id\": 4181,\n" +
        "    " +
        "\"cost_for_shop\": 249,\n" +
        "    " +
        "\"date_from\": {\n" +
        "      " +
        "\"day\": 16,\n" +
        "      " +
        "\"month\": 6,\n" +
        "      " +
        "\"year\": 2020\n" +
        "    " +
        "},\n" +
        "    " +
        "\"date_to\": {\n" +
        "      " +
        "\"day\": 17,\n" +
        "      " +
        "\"month\": 6,\n" +
        "      " +
        "\"year\": 2020\n" +
        "    " +
        "}\n" +
        "  " +
        "}\n" +
        "}\n";

    private ParcelSteps() {
    }

    public static Parcel getParcel() {
        Parcel parcel = new Parcel();

        parcel.setId(SHIPMENT_ID);
        parcel.setShipmentId(SHOP_SHIPMENT_ID);
        parcel.setWeight(WEIGHT);
        parcel.setWidth(WIDTH);
        parcel.setDepth(HEIGHT);
        parcel.setHeight(DEPTH);
        parcel.setStatus(STATUS);
        parcel.setLabelURL(LABEL_URL);
        parcel.setShipmentDate(LocalDate.parse("2016-03-21"));

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(123L);
        parcelBox.setFulfilmentId("13");

        parcel.setBoxes(Collections.singletonList(parcelBox));

        return parcel;
    }

    public static void addRoute(Parcel parcel) {
        parcel.setRoute(OBJECT_MAPPER.valueToTree(ParcelSteps.ROUTE));
    }
}
