package ru.yandex.market.core.order.model;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Тесты для {@link Parcel}.
 *
 * @author ivmelnik
 * @since 15.08.18
 */
class ParcelTest {

    private static final Long SHIPMENT_ID = 1L;
    private static final Long SHIPMENT_WEIGHT = 1000L;
    private static final Long SHIPMENT_HEIGHT = 10L;
    private static final Long SHIPMENT_WIDTH = 20L;
    private static final Long SHIPMENT_DEPTH = 30L;

    private static final WeightAndSize PARCEL_WEIGHT_AND_SIZE = WeightAndSize.builder()
            .withWeight(SHIPMENT_WEIGHT)
            .withHeight(SHIPMENT_HEIGHT)
            .withWidth(SHIPMENT_WIDTH)
            .withDepth(SHIPMENT_DEPTH)
            .build();

    private static final Long BOX1_ID = 11L;
    private static final Long BOX1_WEIGHT = 500L;
    private static final Long BOX1_HEIGHT = 3L;
    private static final Long BOX1_WIDTH = 4L;
    private static final Long BOX1_DEPTH = 5L;

    private static final WeightAndSize BOX1_WEIGHT_AND_SIZE = WeightAndSize.builder()
            .withWeight(BOX1_WEIGHT)
            .withHeight(BOX1_HEIGHT)
            .withWidth(BOX1_WIDTH)
            .withDepth(BOX1_DEPTH)
            .build();

    private static final ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox CHECKOUTER_BOX1 =
            new CheckouterParcelBoxBuilder(BOX1_ID, BOX1_WEIGHT_AND_SIZE).build();

    private static final ParcelBox BOX1 = ParcelBox.builder(BOX1_ID)
            .withWeightAndSize(BOX1_WEIGHT_AND_SIZE)
            .withParcelId(1L)
            .build();

    private static final Long BOX2_ID = 12L;
    private static final Long BOX2_WEIGHT = 500L;
    private static final Long BOX2_HEIGHT = 6L;
    private static final Long BOX2_WIDTH = 7L;
    private static final Long BOX2_DEPTH = 8L;

    private static final WeightAndSize BOX2_WEIGHT_AND_SIZE = WeightAndSize.builder()
            .withWeight(BOX2_WEIGHT)
            .withHeight(BOX2_HEIGHT)
            .withWidth(BOX2_WIDTH)
            .withDepth(BOX2_DEPTH)
            .build();

    private static final ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox CHECKOUTER_BOX2 =
            new CheckouterParcelBoxBuilder(BOX2_ID, BOX2_WEIGHT_AND_SIZE).build();

    private static final ParcelBox BOX2 = ParcelBox.builder(BOX2_ID)
            .withWeightAndSize(BOX2_WEIGHT_AND_SIZE)
            .withParcelId(1L)
            .build();

    private ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel getOrderShipmentNoBoxes() {
        ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel shipment =
                new ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel();
        shipment.setId(SHIPMENT_ID);
        shipment.setWeight(SHIPMENT_WEIGHT);
        shipment.setHeight(SHIPMENT_HEIGHT);
        shipment.setWidth(SHIPMENT_WIDTH);
        shipment.setDepth(SHIPMENT_DEPTH);
        return shipment;
    }

    private ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel getOrderShipment() {
        ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel shipment = getOrderShipmentNoBoxes();
        shipment.setBoxes(asList(
                CHECKOUTER_BOX1,
                CHECKOUTER_BOX2
        ));
        return shipment;
    }

    private void checkParcelEmpty(Parcel parcel) {
        assertNull(parcel.getWeightAndSize().getWeight());
        assertNull(parcel.getWeightAndSize().getHeight());
        assertNull(parcel.getWeightAndSize().getWidth());
        assertNull(parcel.getWeightAndSize().getDepth());
        assertEquals(Collections.emptyList(), parcel.getBoxes());
    }

    private void checkParcelNoBoxes(Parcel parcel) {
        assertEquals(SHIPMENT_WEIGHT, parcel.getWeightAndSize().getWeight());
        assertEquals(SHIPMENT_HEIGHT, parcel.getWeightAndSize().getHeight());
        assertEquals(SHIPMENT_WIDTH, parcel.getWeightAndSize().getWidth());
        assertEquals(SHIPMENT_DEPTH, parcel.getWeightAndSize().getDepth());
        assertEquals(Collections.emptyList(), parcel.getBoxes());
    }

    private void checkParcel(Parcel parcel) {
        assertEquals(SHIPMENT_WEIGHT, parcel.getWeightAndSize().getWeight());
        assertEquals(SHIPMENT_HEIGHT, parcel.getWeightAndSize().getHeight());
        assertEquals(SHIPMENT_WIDTH, parcel.getWeightAndSize().getWidth());
        assertEquals(SHIPMENT_DEPTH, parcel.getWeightAndSize().getDepth());
        assertThat(parcel.getBoxes(), containsInAnyOrder(
                samePropertyValuesAs(BOX1),
                samePropertyValuesAs(BOX2)
        ));
    }

    /// Tests

    @Test
    void getEmptyParcel() {
        checkParcelEmpty(Parcel.EMPTY);
    }

    @Test
    void createParcelNoBoxes() {
        Parcel parcel = new Parcel.Builder().withWeightAndSize(PARCEL_WEIGHT_AND_SIZE).build();
        checkParcelNoBoxes(parcel);
    }

    @Test
    void createParcel() {
        Parcel parcel = new Parcel.Builder()
                .withWeightAndSize(PARCEL_WEIGHT_AND_SIZE)
                .withBoxes(asList(BOX1, BOX2))
                .build();
        checkParcel(parcel);
    }

    @Test
    void createParcelOfNull() {
        Parcel parcel = Parcel.transform(null, -1);
        checkParcelEmpty(parcel);
    }

    @Test
    void createParcelOfShipmentNoBoxes() {
        ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel shipment = getOrderShipmentNoBoxes();
        Parcel parcel = Parcel.transform(shipment, -1);
        checkParcelNoBoxes(parcel);
    }

    @Test
    void createParcelOfShipment() {
        ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel shipment = getOrderShipment();
        Parcel parcel = Parcel.transform(shipment, -1);
        checkParcel(parcel);
    }
}
