package ru.yandex.market.logistics.management.service.export.dynamic;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

class RelationsBuilderHolidaysWhWhTest extends AbstractDynamicBuilderTest {

    @ParameterizedTest(name = "{index} {0}")
    @ArgumentsSource(TestArgumentsProvider.class)
    void mapWarehouseAndWarehouse(String testName,
                                  Set<Integer> fromFulfilmentLogisticPointScheduleDays,
                                  Set<Integer> toFulfilmentLogisticPointScheduleDays,
                                  String expectedJsonPath) {
        PartnerRelationDto partnerRelation = createPartnerRelations(createFulfillments()).get(0);
        if (fromFulfilmentLogisticPointScheduleDays != null) {
            PartnerDto partner = partnerRelation.getFromPartner();
            partner.getActiveWarehouses().iterator().next()
                .setScheduleDays(fromFulfilmentLogisticPointScheduleDays);
        }
        if (toFulfilmentLogisticPointScheduleDays != null) {
            PartnerDto partner = partnerRelation.getToPartner();
            partner.getActiveWarehouses().iterator().next()
                .setScheduleDays(new HashSet<>(toFulfilmentLogisticPointScheduleDays));
        }

        Mockito.when(partnerRelationRepository.findAllForDynamic(any(), anySet(), anySet(), any()))
            .thenReturn(Collections.singletonList(partnerRelation))
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
                    "With no Schedule",
                    null,
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only supplier with no ScheduleDays",
                    Set.of(),
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only supplier with all ScheduleDays",
                    Set.of(1, 2, 3, 4, 5, 6, 7),
                    null,
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only supplier with some ScheduleDays",
                    Set.of(1, 2, 4, 5, 6),
                    null,
                    "data/mds/relation_holidays/relation_with_3_7_holidays.json"
                ),
                Arguments.arguments(
                    "Only fulfillment with no ScheduleDays",
                    null,
                    Set.of(),
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only fulfillment with all ScheduleDays",
                    null,
                    Set.of(6, 7, 5, 2, 3, 4, 1),
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                ),
                Arguments.arguments(
                    "Only fulfillment with some ScheduleDays",
                    null,
                    Set.of(3, 4, 5),
                    "data/mds/relation_holidays/relation_with_1_2_6_7_holidays.json"
                ),
                Arguments.arguments(
                    "Both with no ScheduleDays intersection",
                    Set.of(1, 2),
                    Set.of(6, 7),
                    "data/mds/relation_holidays/relation_with_all_holidays.json"
                ),
                Arguments.arguments(
                    "Both with some ScheduleDays intersection",
                    Set.of(1, 2, 4, 5, 6),
                    Set.of(1, 2, 4, 5, 6, 7),
                    "data/mds/relation_holidays/relation_with_3_7_holidays.json"
                ),
                Arguments.arguments(
                    "Both with all ScheduleDays",
                    Set.of(1, 2, 3, 4, 5, 6, 7),
                    Set.of(1, 2, 3, 4, 5, 6, 7),
                    "data/mds/relation_holidays/relation_with_no_holidays.json"
                )
            );
        }


        private ScheduleDay createScheduleDay(int day) {
            return new ScheduleDay()
                .setDay(day);
        }
    }
}
