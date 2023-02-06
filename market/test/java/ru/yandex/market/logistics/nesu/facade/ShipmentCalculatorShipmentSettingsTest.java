package ru.yandex.market.logistics.nesu.facade;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import one.util.streamex.EntryStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.api.model.Partner;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.dto.DeliverySettingShipment;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryOptionShipment;

@DisplayName("Расчет варианта отгрузки, с учетом настроек отгрузки СД")
public class ShipmentCalculatorShipmentSettingsTest extends AbstractShipmentCalculatorTest {

    @BeforeEach
    void setupSortingCenter() {
        shipment.setIncludeNonDefault(true);
        sortingCenter = partner(SORTING_CENTER_ID, List.of(scheduleDay(1)), PartnerType.SORTING_CENTER);

        partnerWarehouses.put(DELIVERY_SERVICE_ID, warehouse(Set.of(scheduleDay(1))));
        partnerWarehouses.put(SORTING_CENTER_ID, warehouse(Set.of(scheduleDay(1))));
    }

    @DisplayName("Поиск опций с учетом настроек")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("defaultOnlySource")
    void defaultOnly(
        @SuppressWarnings("unused") String caseName,
        Set<Partner> importPartners,
        Set<Partner> withdrawPartners,
        List<Pair<Long, ShipmentType>> expectedOptionShipments
    ) {
        shipmentSettingsByPartnerId = createPartnerShipmentSettings(importPartners, withdrawPartners);
        List<DeliveryOptionShipment> deliveryOptionShipments = calculateShipments();

        softly.assertThat(deliveryOptionShipments).hasSize(expectedOptionShipments.size());
        EntryStream.zip(deliveryOptionShipments, expectedOptionShipments)
            .forKeyValue((deliveryOptionShipment, expectedOptionShipment) -> {
                softly.assertThat(deliveryOptionShipment.getType())
                    .isEqualTo(expectedOptionShipment.getRight());

                softly.assertThat(deliveryOptionShipment.getPartner().getId())
                    .isEqualTo(expectedOptionShipment.getLeft());
            });
    }

    @Nonnull
    private static Stream<Arguments> defaultOnlySource() {
        return Stream.of(
            Arguments.of(
                "Самопривоз только через СЦ, забор только напрямую",
                Set.of(SORTING_CENTER_PARTNER),
                Set.of(DELIVERY_SERVICE_PARTNER),
                List.of(
                    Pair.of(SORTING_CENTER_ID, ShipmentType.IMPORT),
                    Pair.of(DELIVERY_SERVICE_ID, ShipmentType.WITHDRAW)
                )
            ),
            Arguments.of(
                "Самопривоз только напрямую, забор только через СЦ",
                Set.of(DELIVERY_SERVICE_PARTNER),
                Set.of(SORTING_CENTER_PARTNER),
                List.of(
                    Pair.of(SORTING_CENTER_ID, ShipmentType.WITHDRAW),
                    Pair.of(DELIVERY_SERVICE_ID, ShipmentType.IMPORT)
                )
            ),
            Arguments.of(
                "Самопривоз неразрешен, забор напрямую или через СЦ",
                Set.of(),
                Set.of(DELIVERY_SERVICE_PARTNER, SORTING_CENTER_PARTNER),
                List.of(
                    Pair.of(SORTING_CENTER_ID, ShipmentType.WITHDRAW),
                    Pair.of(DELIVERY_SERVICE_ID, ShipmentType.WITHDRAW)
                )
            ),
            Arguments.of(
                "Самопривоз напрямую или через СЦ, забор неразрешен",
                Set.of(DELIVERY_SERVICE_PARTNER, SORTING_CENTER_PARTNER),
                Set.of(),
                List.of(
                    Pair.of(SORTING_CENTER_ID, ShipmentType.IMPORT),
                    Pair.of(DELIVERY_SERVICE_ID, ShipmentType.IMPORT)
                )
            ),
            Arguments.of(
                "Самопривоз неразрешен, забор неразрешен",
                Set.of(),
                Set.of(),
                List.of()
            ),
            Arguments.of(
                "Самопривоз напрямую или через СЦ, забор напрямую или через СЦ",
                Set.of(DELIVERY_SERVICE_PARTNER, SORTING_CENTER_PARTNER),
                Set.of(DELIVERY_SERVICE_PARTNER, SORTING_CENTER_PARTNER),
                List.of(
                    Pair.of(SORTING_CENTER_ID, ShipmentType.WITHDRAW),
                    Pair.of(SORTING_CENTER_ID, ShipmentType.IMPORT),
                    Pair.of(DELIVERY_SERVICE_ID, ShipmentType.WITHDRAW),
                    Pair.of(DELIVERY_SERVICE_ID, ShipmentType.IMPORT)
                )
            )
        );
    }

    @Nonnull
    private Map<Long, List<DeliverySettingShipment>> createPartnerShipmentSettings(
        Set<Partner> importPartners,
        Set<Partner> withdrawPartners
    ) {
        return Map.of(
            DELIVERY_SERVICE_ID,
            Stream.concat(
                importPartners.stream()
                    .map(
                        partner -> DeliverySettingShipment.builder()
                            .shipmentType(ShipmentType.IMPORT)
                            .partner(partner)
                            .build()
                    ),
                withdrawPartners.stream()
                    .map(
                        partner -> DeliverySettingShipment.builder()
                            .shipmentType(ShipmentType.WITHDRAW)
                            .partner(partner)
                            .build()
                    )
            )
                .collect(Collectors.toList())
        );
    }
}
