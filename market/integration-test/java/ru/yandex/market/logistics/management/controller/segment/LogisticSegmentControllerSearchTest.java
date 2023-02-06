package ru.yandex.market.logistics.management.controller.segment;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
@DatabaseSetup("/data/controller/logisticSegment/before/prepare_data.xml")
class LogisticSegmentControllerSearchTest extends AbstractContextualTest {

    @MethodSource
    @DisplayName("Поиск логистических сегментов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void searchSegments(
        @SuppressWarnings("unused") String name,
        UnaryOperator<LogisticSegmentFilter.Builder> filterModifier,
        String responsePath
    ) throws Exception {
        LogisticSegmentFilter filter = filterModifier.apply(LogisticSegmentFilter.builder()).build();
        mockMvc.perform(
            put("/externalApi/logistic-segments/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath, JSONCompareMode.NON_EXTENSIBLE));
    }

    @Nonnull
    private static Stream<Arguments> searchSegments() {
        return Stream.<Triple<String, UnaryOperator<LogisticSegmentFilter.Builder>, String>>of(
            Triple.of(
                "ids",
                f -> f.setIds(Set.of(10001L, 10002L, 10005L, 10006L, 10007L)),
                "data/controller/logisticSegment/response/by_ids.json"
            ),
            Triple.of(
                "logisticsPointIds",
                f -> f.setLogisticsPointIds(Set.of(101L, 102L)),
                "data/controller/logisticSegment/response/by_logistic_point_ids.json"
            ),
            Triple.of(
                "partnerIds",
                f -> f.setPartnerIds(Set.of(1L, 3L)),
                "data/controller/logisticSegment/response/by_partner_ids.json"
            ),
            Triple.of(
                "types",
                f -> f.setTypes(Set.of(LogisticSegmentType.WAREHOUSE, LogisticSegmentType.HANDING)),
                "data/controller/logisticSegment/response/by_types.json"
            ),
            Triple.of(
                "service statuses",
                f -> f.setServiceStatuses(Set.of(ActivityStatus.ACTIVE)),
                "data/controller/logisticSegment/response/by_service_statuses.json"
            ),
            Triple.of(
                "service codes",
                f -> f.setServiceCodes(Set.of(ServiceCodeName.MOVEMENT)),
                "data/controller/logisticSegment/response/by_service_codes.json"
            ),
            Triple.of(
                "service updated to",
                f -> f.setServiceUpdatedTo(LocalDateTime.of(2020, 1, 1, 12, 0, 0)),
                "data/controller/logisticSegment/response/by_service_updated_to.json"
            ),
            Triple.of(
                "service updated from",
                f -> f.setServiceUpdatedFrom(LocalDateTime.of(2018, 1, 1, 12, 0, 0)),
                "data/controller/logisticSegment/response/by_service_updated_from.json"
            ),
            Triple.of(
                "search query (partner id and name)",
                f -> f.setSearchQuery("foo").setPartnerIds(Set.of(3L)),
                "data/controller/logisticSegment/response/by_search_query_partner_id_and_name.json"
            ),
            Triple.of(
                "search query (segment id)",
                f -> f.setSearchQuery("10006"),
                "data/controller/logisticSegment/response/by_search_query_segment_id.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @MethodSource
    @DisplayName("Постраничный поиск логистических сегментов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void searchSegmentsWithPaging(
        @SuppressWarnings("unused") String name,
        Pageable pageable,
        String responsePath
    ) throws Exception {
        LogisticSegmentFilter filter = LogisticSegmentFilter
            .builder()
            .setIds(Set.of(10001L, 10002L, 10005L, 10006L, 10007L))
            .build();
        mockMvc.perform(
            put("/externalApi/logistic-segments/search-paged")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
                .param("size", String.valueOf(pageable.getPageSize()))
                .param("page", String.valueOf(pageable.getPageNumber()))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchSegmentsWithPaging() {
        return Stream.of(
            Arguments.of(
                "Первая страница",
                PageRequest.of(0, 2),
                "data/controller/logisticSegment/response/by_ids_first_page.json"
            ),
            Arguments.of(
                "Последняя страница",
                PageRequest.of(2, 2),
                "data/controller/logisticSegment/response/by_ids_last_page.json"
            ),
            Arguments.of(
                "Слишком большой размер страницы",
                PageRequest.of(0, 50),
                "data/controller/logisticSegment/response/by_ids_large_page_size.json"
            ),
            Arguments.of(
                "Слишком большой номер страницы",
                PageRequest.of(10, 10),
                "data/controller/logisticSegment/response/by_ids_large_page_number.json"
            )
        );
    }

    @MethodSource
    @DisplayName("Поиск мувмент сегментов со связями")
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/filtering_with_edges.xml",
        type = DatabaseOperation.INSERT
    )
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void searchSegmentsSegmentsWithNearbySegments(
        @SuppressWarnings("unused") String name,
        LogisticSegmentFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put("/externalApi/logistic-segments/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath, false));
    }

    @Nonnull
    private static Stream<Arguments> searchSegmentsSegmentsWithNearbySegments() {
        return Stream.of(
            Arguments.of(
                "Поиск неактивных сегментов",
                LogisticSegmentFilter.builder()
                    .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
                    .setActive(false)
                    .build(),
                "data/controller/logisticSegment/response/empty.json"
            ),
            Arguments.of(
                "Поиск по партнерам связанных сегментов",
                LogisticSegmentFilter.builder()
                    .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
                    .setActive(true)
                    .setSearchPartnerFromQuery("1")
                    .setSearchPartnerToQuery("rtner 2")
                    .build(),
                "data/controller/logisticSegment/response/by_partners_from_and_to.json"
            ),
            Arguments.of(
                "Поиск по типам партнерам связанных сегментов",
                LogisticSegmentFilter.builder()
                    .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
                    .setActive(true)
                    .setPartnerFromType(PartnerType.DROPSHIP)
                    .setPartnerToType(PartnerType.SORTING_CENTER)
                    .build(),
                "data/controller/logisticSegment/response/by_partners_from_and_to_types.json"
            ),
            Arguments.of(
                "Поиск по типу предыдущего сегмента",
                LogisticSegmentFilter.builder()
                    .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
                    .setActive(true)
                    .setSegmentFromType(LogisticSegmentType.WAREHOUSE)
                    .build(),
                "data/controller/logisticSegment/response/by_segment_from_type.json"
            ),
            Arguments.of(
                "Поиск по логточке предыдущего сегмента",
                LogisticSegmentFilter.builder()
                    .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
                    .setActive(true)
                    .setSearchFromLogisticPoint("Point 102")
                    .build(),
                "data/controller/logisticSegment/response/by_logistic_point_from.json"
            ),
            Arguments.of(
                "Конфликтующие фильтры",
                LogisticSegmentFilter.builder()
                    .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
                    .setActive(true)
                    .setSearchPartnerFromQuery("1")
                    .setSegmentToType(LogisticSegmentType.LINEHAUL)
                    .build(),
                "data/controller/logisticSegment/response/empty.json"
            ),
            Arguments.of(
                "Предыдущие связи (поиск по сегментам-назначениям)",
                LogisticSegmentFilter.builder()
                    .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
                    .setSegmentsToIds(Set.of(10003L))
                    .build(),
                "data/controller/logisticSegment/response/by_next_segments_ids.json"
            ),
            Arguments.of(
                "Следующие связи (поиск по сегментам-источникам)",
                LogisticSegmentFilter.builder()
                    .setTypes(Set.of(LogisticSegmentType.MOVEMENT))
                    .setSegmentsFromIds(Set.of(10003L))
                    .build(),
                "data/controller/logisticSegment/response/by_previous_segments_ids.json"
            )
        );
    }
}
