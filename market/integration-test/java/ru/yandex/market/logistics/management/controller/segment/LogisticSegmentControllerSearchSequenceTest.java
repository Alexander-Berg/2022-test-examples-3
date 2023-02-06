package ru.yandex.market.logistics.management.controller.segment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.BaseLogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentSequenceFilter;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;

@ParametersAreNonnullByDefault
@DisplayName("Тесты на поиск последовательности сегментов")
class LogisticSegmentControllerSearchSequenceTest extends AbstractContextualAspectValidationTest {

    @MethodSource
    @ParameterizedTest(name = "{0}")
    @DatabaseSetup("/data/controller/logisticSegment/before/sequence_segments_search.xml")
    @DisplayName("Успешный поиск последовательности сегментов")
    @SuppressWarnings("unused")
    void successSearchSequence(String name, LogisticSegmentSequenceFilter filter, String expectation) throws Exception {

        mockMvc.perform(TestUtil.request(
                HttpMethod.PUT,
                "/externalApi/logistic-segments/searchSequence",
                filter
            ))
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectation));
    }

    @Nonnull
    private static Stream<Arguments> successSearchSequence() {
        return Stream.of(
            Arguments.of(
                "Полный фильтр",
                LogisticSegmentSequenceFilter.builder().segmentSequence(List.of(
                    logisticSegmentFilter(1L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 10L),
                    logisticSegmentFilter(2L, LogisticSegmentType.MOVEMENT, ActivityStatus.ACTIVE, null),
                    logisticSegmentFilter(3L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 20L)
                )).build(),
                "data/controller/logisticSegment/response/segment_sequence_full_filter.json"
            ),
            Arguments.of(
                "Фильтр на Id",
                LogisticSegmentSequenceFilter.builder().segmentSequence(List.of(
                    logisticSegmentFilter(1L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 10L),
                    logisticSegmentFilter(null, LogisticSegmentType.MOVEMENT, ActivityStatus.ACTIVE, null),
                    logisticSegmentFilter(3L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 20L)
                )).build(),
                "data/controller/logisticSegment/response/segment_sequence_segment_id.json"
            ),
            Arguments.of(
                "Фильтр на LogisticSegmentType",
                LogisticSegmentSequenceFilter.builder().segmentSequence(List.of(
                    logisticSegmentFilter(1L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 10L),
                    logisticSegmentFilter(null, null, ActivityStatus.ACTIVE, null),
                    logisticSegmentFilter(3L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 20L)
                )).build(),
                "data/controller/logisticSegment/response/segment_sequence_logistic_segment_type.json"
            ),
            Arguments.of(
                "Фильтр на ActivityStatus",
                LogisticSegmentSequenceFilter.builder().segmentSequence(List.of(
                    logisticSegmentFilter(1L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 10L),
                    logisticSegmentFilter(null, LogisticSegmentType.MOVEMENT, null, null),
                    logisticSegmentFilter(3L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 20L)
                )).build(),
                "data/controller/logisticSegment/response/segment_sequence_activity_status.json"
            ),
            Arguments.of(
                "Фильтр на LogisticPointId",
                LogisticSegmentSequenceFilter.builder().segmentSequence(List.of(
                    logisticSegmentFilter(1L, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, 10L),
                    logisticSegmentFilter(2L, LogisticSegmentType.MOVEMENT, ActivityStatus.ACTIVE, null),
                    logisticSegmentFilter(null, LogisticSegmentType.WAREHOUSE, ActivityStatus.ACTIVE, null)
                )).build(),
                "data/controller/logisticSegment/response/segment_sequence_logistic_point_id.json"
            ),
            Arguments.of(
                "Пустой фильтр",
                LogisticSegmentSequenceFilter.builder().segmentSequence(List.of(
                        BaseLogisticSegmentFilter.builder().build(),
                        BaseLogisticSegmentFilter.builder().build(),
                        BaseLogisticSegmentFilter.builder().build()
                    )
                ).build(),
                "data/controller/logisticSegment/response/segment_sequence_empty_filter.json"
            ),
            Arguments.of(
                "Полный фильтр c пустыми коллекциями",
                LogisticSegmentSequenceFilter.builder().segmentSequence(List.of(
                        emptyCollectionFilter(),
                        emptyCollectionFilter(),
                        emptyCollectionFilter()
                    )
                ).build(),
                "data/controller/logisticSegment/response/segment_sequence_empty_filter.json"
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = "{0}")
    @DisplayName("Ошибка поиска последовательности сегментов")
    void failSearchSegmentSequence(
        @SuppressWarnings("unused") String name,
        List<BaseLogisticSegmentFilter> segmentFilters
    ) throws Exception {
        mockMvc.perform(TestUtil.request(
                HttpMethod.PUT,
                "/externalApi/logistic-segments/searchSequence",
                LogisticSegmentSequenceFilter.builder().segmentSequence(segmentFilters).build()
            ))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Validation failed for object='logisticSegmentSequenceFilter'. Error count: 1"));
    }

    @Nonnull
    private static Stream<Arguments> failSearchSegmentSequence() {
        return Stream.of(
            Arguments.of(
                "Ошибочный фильтр, поиск последовательности из 1 сегмента",
                List.of(
                    BaseLogisticSegmentFilter.builder().setIds(Set.of(1L)).build()
                )
            ),
            Arguments.of(
                "Ошибочный фильтр, поиск последовательности из 4 сегмента",
                List.of(
                    BaseLogisticSegmentFilter.builder().setIds(Set.of(1L)).build(),
                    BaseLogisticSegmentFilter.builder().setIds(Set.of(2L)).build(),
                    BaseLogisticSegmentFilter.builder().setIds(Set.of(3L)).build(),
                    BaseLogisticSegmentFilter.builder().setIds(Set.of(4L)).build()
                )
            ),
            Arguments.of(
                "Ошибочный фильтр, нет фильтров на сегменты",
                List.of()
            )
        );
    }

    @Nonnull
    private static BaseLogisticSegmentFilter logisticSegmentFilter(
        @Nullable Long id,
        @Nullable LogisticSegmentType type,
        @Nullable ActivityStatus status,
        @Nullable Long logisticPointId
    ) {
        return BaseLogisticSegmentFilter.builder()
            .setIds(Optional.ofNullable(id).map(Set::of).orElse(null))
            .setTypes(Optional.ofNullable(type).map(Set::of).orElse(null))
            .setServiceStatuses(Optional.ofNullable(status).map(Set::of).orElse(null))
            .setLogisticsPointIds(Optional.ofNullable(logisticPointId).map(Set::of).orElse(null))
            .build();
    }

    @Nonnull
    private static BaseLogisticSegmentFilter emptyCollectionFilter() {
        return BaseLogisticSegmentFilter.builder()
            .setIds(Set.of())
            .setLogisticsPointIds(Set.of())
            .setTypes(Set.of())
            .setServiceStatuses(Set.of())
            .build();
    }
}
