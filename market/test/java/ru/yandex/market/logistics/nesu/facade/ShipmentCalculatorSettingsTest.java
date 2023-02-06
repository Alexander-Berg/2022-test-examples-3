package ru.yandex.market.logistics.nesu.facade;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.model.entity.SenderDeliverySettings;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryOptionShipment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Расчет варианта отгрузки, соответствующего настройкам магазина")
class ShipmentCalculatorSettingsTest extends AbstractShipmentCalculatorTest {

    @BeforeEach
    void setupSortingCenter() {
        sortingCenter = partner(SORTING_CENTER_ID, List.of(scheduleDay(1)), PartnerType.SORTING_CENTER);

        partnerWarehouses.put(DELIVERY_SERVICE_ID, warehouse(Set.of(scheduleDay(1))));
        partnerWarehouses.put(SORTING_CENTER_ID, warehouse(Set.of(scheduleDay(1))));
    }

    @DisplayName("Полный результат")
    @ParameterizedTest
    @MethodSource("fullResultSource")
    void fullResult(boolean useWithdraw, boolean useSortingCenter, int defaultIndex) {
        mockSettings(useWithdraw, useSortingCenter);

        List<DeliveryOptionShipment> result = calculateShipments();
        long foundIndex = StreamEx.of(result)
            .indexOf(DeliveryOptionShipment::isSettingsDefault)
            .orElseThrow(AssertionError::new);
        softly.assertThat(foundIndex).isEqualTo(defaultIndex);
    }

    @Nonnull
    private static Stream<Arguments> fullResultSource() {
        return Stream.of(
            Arguments.of(true, true, 0),
            Arguments.of(false, true, 1),
            Arguments.of(true, false, 2),
            Arguments.of(false, false, 3)
        );
    }

    @DisplayName("Только отгрузка по умолчанию")
    @ParameterizedTest
    @MethodSource("defaultOnlySource")
    void defaultOnly(boolean useWithdraw, boolean useSortingCenter, ShipmentType shipmentType, Long partnerId) {
        shipment.setIncludeNonDefault(null);
        mockSettings(useWithdraw, useSortingCenter);

        List<DeliveryOptionShipment> shipments = calculateShipments();

        assertThat(shipments).hasSize(1);
        DeliveryOptionShipment shipment = shipments.get(0);
        softly.assertThat(shipment.getType()).isEqualTo(shipmentType);
        softly.assertThat(shipment.getPartner().getId()).isEqualTo(partnerId);
    }

    @Nonnull
    private static Stream<Arguments> defaultOnlySource() {
        return Stream.of(
            Arguments.of(true, true, ShipmentType.WITHDRAW, SORTING_CENTER_ID),
            Arguments.of(false, true, ShipmentType.IMPORT, SORTING_CENTER_ID),
            Arguments.of(true, false, ShipmentType.WITHDRAW, DELIVERY_SERVICE_ID),
            Arguments.of(false, false, ShipmentType.IMPORT, DELIVERY_SERVICE_ID)
        );
    }

    @DisplayName("Не учитывать отгрузки в СД без складов")
    @Test
    void producingNoImportShipmentsForDeliveryServicesWithoutWarehouses() {
        shipment.setIncludeNonDefault(null);
        List<DeliveryOptionShipment> shipments = calculateShipments();
        assertThat(shipments).hasSize(1);
        DeliveryOptionShipment shipment = shipments.get(0);
        softly.assertThat(shipment.getType()).isEqualTo(ShipmentType.IMPORT);
        softly.assertThat(shipment.getPartner().getId()).isEqualTo(DELIVERY_SERVICE_ID);

        partnerWarehouses.clear();

        shipments = calculateShipments();
        softly.assertThat(shipments).hasSize(0);
    }

    private void mockSettings(boolean useWithdraw, boolean useSortingCenter) {
        deliverySettings.put(
            DELIVERY_SERVICE_ID,
            new SenderDeliverySettings()
                .setUseCourierDelivery(useWithdraw)
                .setUseSortingCenter(useSortingCenter)
        );
    }

}
