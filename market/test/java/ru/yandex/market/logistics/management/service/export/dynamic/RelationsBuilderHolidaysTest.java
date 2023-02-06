package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.domain.entity.type.ShipmentType;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.LogisticsPointDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.ScheduleDayDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

class RelationsBuilderHolidaysTest extends AbstractDynamicBuilderTest {

    @ParameterizedTest(name = "{index} {0}")
    @ArgumentsSource(TestArgumentsProvider.class)
    void mapWarehouseAndDelivery(
        String testName,
        Set<Integer> fulfilmentLogisticPointScheduleDays,
        LogisticsPointDto deliveryServiceLogisticPoint,
        List<ScheduleDayDto> shipmentSchedule,
        ShipmentType shipmentType,
        PartnerType partnerType,
        String expectedJsonPath
    ) {
        List<PartnerRelationDto> warehouseToDeliveryRelations = createPartnerRelations();
        PartnerRelationDto firstPartnerRelation = warehouseToDeliveryRelations.get(0);

        if (fulfilmentLogisticPointScheduleDays != null) {
            Optional.of(firstPartnerRelation.getFromPartner().getActiveWarehouses().iterator())
                .filter(Iterator::hasNext)
                .ifPresent(iterator -> iterator.next().setScheduleDays(fulfilmentLogisticPointScheduleDays));
        }
        firstPartnerRelation.setToPartnerLogisticsPoint(deliveryServiceLogisticPoint);
        firstPartnerRelation.setShipmentScheduleDays(shipmentSchedule);
        firstPartnerRelation.setShipmentType(shipmentType);
        Optional.ofNullable(partnerType)
            .ifPresent(pt -> firstPartnerRelation.getFromPartner().setPartnerType(pt));

        Mockito.when(partnerRelationRepository.findAllForDynamic(any(), anySet(), anySet(), any()))
            .thenReturn(warehouseToDeliveryRelations)
            .thenReturn(Collections.emptyList());

        Logistics.MetaInfo metaInfo = buildReport();

        softly.assertThat(metaInfo).as("Relation days set should be equal")
            .hasSameDaySetAs(expectedJsonPath);
    }

    static class TestArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.arguments(
                    "Only FF with no Schedule",
                    null,
                    null,
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only FF with no ScheduleDays",
                    Set.of(),
                    null,
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only FF with all ScheduleDays",
                    Set.of(1, 2, 3, 4, 5, 6, 7),
                    null,
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only FF with some ScheduleDays",
                    Set.of(1, 2, 4, 5, 6),
                    null,
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_3_7_holidays.json"
                ),
                Arguments.arguments(
                    "Only DS with no Schedule",
                    null,
                    createLogisticsPoint(null, PointType.PICKUP_POINT),
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only DS with no ScheduleDays",
                    null,
                    createLogisticsPoint(Set.of(), PointType.PICKUP_POINT),
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only DS with all ScheduleDays",
                    null,
                    createLogisticsPoint(Set.of(6, 7, 5, 2, 3, 4, 1), PointType.PICKUP_POINT),
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only DS with some ScheduleDays",
                    null,
                    createLogisticsPoint(Set.of(3, 4, 5), PointType.PICKUP_POINT),
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_1_2_6_7_holidays.json"
                ),
                Arguments.arguments(
                    "FF and DS with no ScheduleDays intersection",
                    Set.of(1, 2),
                    createLogisticsPoint(Set.of(6, 7), PointType.PICKUP_POINT),
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_all_holidays.json"
                ),
                Arguments.arguments(
                    "FF and DS with some ScheduleDays intersection",
                    Set.of(1, 2, 4, 5, 6),
                    createLogisticsPoint(Set.of(1, 2, 4, 5, 6, 7), PointType.PICKUP_POINT),
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_3_7_holidays.json"
                ),
                Arguments.arguments(
                    "FF and DS with all ScheduleDays",
                    Set.of(1, 2, 3, 4, 5, 6, 7),
                    createLogisticsPoint(Set.of(1, 2, 3, 4, 5, 6, 7), PointType.PICKUP_POINT),
                    null,
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Dropship partner relation has ImportSchedule",
                    Set.of(1, 2, 3, 4, 5, 6, 7),
                    createLogisticsPoint(Set.of(1, 2, 3, 4, 5, 6, 7), PointType.WAREHOUSE),
                    createScheduleDays(3, 4, 5),
                    ShipmentType.IMPORT,
                    PartnerType.DROPSHIP,
                    "data/mds/relation_holidays/relation_with_1_2_6_7_holidays.json"
                ),
                Arguments.arguments(
                    "Dropship partner relation with no ScheduleDays",
                    Set.of(1, 2, 3, 4, 5, 6, 7),
                    createLogisticsPoint(Set.of(1, 2, 3, 4, 5, 6, 7), PointType.WAREHOUSE),
                    List.of(),
                    ShipmentType.WITHDRAW,
                    PartnerType.DROPSHIP,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Crossdock partner relation has IntakeSchedule",
                    Set.of(1, 2, 3, 4, 5, 6, 7),
                    createLogisticsPoint(Set.of(1, 2, 3, 4, 5, 6, 7), PointType.WAREHOUSE),
                    createScheduleDays(1, 2, 4, 5, 6),
                    ShipmentType.WITHDRAW,
                    PartnerType.SUPPLIER,
                    "data/mds/relation_holidays/relation_with_3_7_holidays.json"
                )
            );
        }

        private LogisticsPointDto createLogisticsPoint(Set<Integer> days, PointType pointType) {
            return new LogisticsPointDto()
                .setType(pointType)
                .setScheduleDays(days);
        }

        private List<ScheduleDayDto> createScheduleDays(int... days) {
            List<ScheduleDayDto> result = new ArrayList<>();
            for (int day : days) {
                result.add(new ScheduleDayDto(
                    day, LocalTime.of(12, 0), LocalTime.of(13, 0))
                );
            }

            return result;
        }
    }
}
