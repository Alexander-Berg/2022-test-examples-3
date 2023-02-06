package ru.yandex.market.deliverycalculator.workflow.util.converter.daas;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.OutletDimensions;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.OutletGroup;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.PickupPoints;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DaasPickupPointsConverterTest {

    private DaasPickupPointsConverter converter;
    private PickupPoints pickupPoints;
    private String json;
    private DeliveryCalcProtos.PickupDeliveryRegion proto;

    @BeforeEach
    void setUp() {
        converter = new DaasPickupPointsConverter();
        pickupPoints = createTestPickupPoints();
        json = "{\"outletGroups\":[{\"dimensions\":{\"dimensions\":[1.0,2.0,3.0],\"dimSum\":5.0},\"outletIds\":[11,22]},{\"dimensions\":{\"dimensions\":[3.0,4.0,5.0],\"dimSum\":8.0},\"outletIds\":[33,44]}]}";
        proto = createTestProto();
    }

    @Test
    void convertFromJson() {
        final PickupPoints actual = converter.convertFromJson(json);
        final PickupPoints expected = pickupPoints;
        assertEquals(expected, actual);
    }

    @Test
    void convertToJson() {
        final String actual = converter.convertToJson(pickupPoints);
        final String expected = json;
        assertEquals(expected, actual);
    }

    @Test
    void convertFromProtoToJson() {
        final String actual = converter.convertFromProtoToJson(proto);
        final String expected = json;
        assertEquals(expected, actual);
    }

    @Test
    void convertFromProto() {
        final PickupPoints actual = converter.convertFromProto(proto);
        final PickupPoints expected = pickupPoints;
        assertEquals(expected, actual);
    }

    private PickupPoints createTestPickupPoints() {
        final PickupPoints result = new PickupPoints();

        final OutletGroup outletGroup1 = new OutletGroup();

        final OutletDimensions outletDimensions1 = new OutletDimensions();
        outletDimensions1.setDimensions(new double[]{1, 2, 3});
        outletDimensions1.setDimSum(5);

        outletGroup1.setDimensions(outletDimensions1);
        outletGroup1.setOutletIds(Arrays.asList(11L, 22L));

        final OutletGroup outletGroup2 = new OutletGroup();

        final OutletDimensions outletDimensions2 = new OutletDimensions();
        outletDimensions2.setDimensions(new double[]{3, 4, 5});
        outletDimensions2.setDimSum(8);

        outletGroup2.setDimensions(outletDimensions2);
        outletGroup2.setOutletIds(Arrays.asList(33L, 44L));

        result.setOutletGroups(Arrays.asList(outletGroup1, outletGroup2));
        return result;
    }

    private DeliveryCalcProtos.PickupDeliveryRegion createTestProto() {
        return DeliveryCalcProtos.PickupDeliveryRegion.newBuilder()
                .setRegionId(1)
                .setOptionGroupId(1)
                .addOutletGroups(
                        DeliveryCalcProtos.OutletGroup.newBuilder()
                                .setDimensions(
                                        DeliveryCalcProtos.OutletDimensions.newBuilder()
                                                .setWidth(1)
                                                .setHeight(2)
                                                .setLength(3)
                                                .setDimSum(5)
                                )
                                .addAllOutletId(
                                        Arrays.asList(11L, 22L)
                                )
                )
                .addOutletGroups(
                        DeliveryCalcProtos.OutletGroup.newBuilder()
                                .setDimensions(
                                        DeliveryCalcProtos.OutletDimensions.newBuilder()
                                                .setWidth(3)
                                                .setHeight(4)
                                                .setLength(5)
                                                .setDimSum(8)
                                )
                                .addAllOutletId(
                                        Arrays.asList(33L, 44L)
                                )
                )
                .build();
    }
}
