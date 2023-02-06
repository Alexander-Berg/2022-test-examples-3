package ru.yandex.market.logistics.management.controller.segment;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.SearchDropshipLogisticMovementsFilter;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
@DatabaseSetup("/data/controller/logisticSegment/before/prepare_data.xml")
@DatabaseSetup(
    value = "/data/controller/logisticSegment/before/dropship_logistic_movements_additional_data.xml",
    type = DatabaseOperation.REFRESH
)
@DisplayName("Поиск информации о логистических перемещениях (связях партнёров в новой модели) из дропшипов")
public class LogisticSegmentControllerSearchDropshipLogisticMovementsTest
    extends AbstractContextualAspectValidationTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchDropshipLogisticMovementsArguments")
    @DisplayName("Поиск информации о логистических перемещениях (связях партнёров в новой модели) из дропшипов")
    void testSearchDropshipLogisticMovements(
        @SuppressWarnings("unused") String name,
        SearchDropshipLogisticMovementsFilter filter,
        String responseFilePath
    ) throws Exception {
        mockMvc.perform(
                put("/externalApi/logistic-segments/search/dropship-logistic-movements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(filter))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responseFilePath, JSONCompareMode.NON_EXTENSIBLE));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchDropshipLogisticMovementsPagedArguments")
    @DisplayName(
        "Постраничный поиск информации о логистических перемещениях (связях партнёров в новой модели) из дропшипов"
    )
    void testSearchDropshipLogisticMovementsPaged(
        @SuppressWarnings("unused") String name,
        SearchDropshipLogisticMovementsFilter filter,
        Pageable pageable,
        String responseFilePath
    ) throws Exception {
        mockMvc.perform(
                put("/externalApi/logistic-segments/search-paged/dropship-logistic-movements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(filter))
                    .param("size", String.valueOf(pageable.getPageSize()))
                    .param("page", String.valueOf(pageable.getPageNumber()))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responseFilePath, JSONCompareMode.NON_EXTENSIBLE));
    }

    @Nonnull
    private static Stream<Arguments> searchDropshipLogisticMovementsArguments() {
        return Stream.of(
            Arguments.of(
                "Поиск с атрибутами - возвращаются перемещения",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .attributes(Set.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                        SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
                    ))
                    .build(),
                "data/controller/logisticSegment/response/dropship_logistic_movements/with_attributes.json"
            ),
            Arguments.of(
                "Поиск для перемещения не от дропшипа - возвращается пустой список",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(2L))
                    .toPartnerIds(Set.of(3L))
                    .toPartnerTypes(EnumSet.of(PartnerType.DELIVERY))
                    .status(ActivityStatus.INACTIVE)
                    .attributes(Set.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                        SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
                    ))
                    .build(),
                "data/controller/logisticSegment/response/dropship_logistic_movements/empty.json"
            ),
            Arguments.of(
                "Поиск с неправильным статусом перемещения - возвращается пустой список",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.INACTIVE)
                    .attributes(Set.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                        SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
                    ))
                    .build(),
                "data/controller/logisticSegment/response/dropship_logistic_movements/empty.json"
            ),
            Arguments.of(
                "Поиск без атрибутов - возвращаются перемещения",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .build(),
                "data/controller/logisticSegment/response/dropship_logistic_movements/without_attributes.json"
            ),
            Arguments.of(
                "Схлопываем перемещения по партнёру и логистической точке назначения",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .attributes(EnumSet.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.TO_PARTNER_LOGISTIC_POINT_DISTINCT
                    ))
                    .build(),
                "data/controller/logisticSegment/response/dropship_logistic_movements/"
                    + "to_partner_logistic_point_distinct.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> searchDropshipLogisticMovementsPagedArguments() {
        return Stream.of(
            Arguments.of(
                "Постраничный поиск с атрибутами - возвращаются перемещения",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .attributes(Set.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                        SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
                    ))
                    .build(),
                PageRequest.of(0, 1),
                "data/controller/logisticSegment/response/dropship_logistic_movements/paged_with_attributes.json"
            ),
            Arguments.of(
                "Постраничный поиск с атрибутами и слишком большим номером страницы - возвращается пустая страница",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .attributes(Set.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                        SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
                    ))
                    .build(),
                PageRequest.of(1, 1),
                "data/controller/logisticSegment/response/dropship_logistic_movements/paged_too_large_page_number.json"
            ),
            Arguments.of(
                "Постраничный поиск для перемещения не от дропшипа - возвращается пустая страница",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(2L))
                    .toPartnerIds(Set.of(3L))
                    .toPartnerTypes(EnumSet.of(PartnerType.DELIVERY))
                    .status(ActivityStatus.INACTIVE)
                    .attributes(Set.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                        SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
                    ))
                    .build(),
                PageRequest.of(0, 1),
                "data/controller/logisticSegment/response/dropship_logistic_movements/paged_empty.json"
            ),
            Arguments.of(
                "Постраничный поиск с неправильным статусом перемещения - возвращается пустая страница",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.INACTIVE)
                    .attributes(Set.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                        SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
                    ))
                    .build(),
                PageRequest.of(0, 1),
                "data/controller/logisticSegment/response/dropship_logistic_movements/paged_empty.json"
            ),
            Arguments.of(
                "Постраничный поиск без атрибутов - возвращаются перемещения",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .build(),
                PageRequest.of(0, 1),
                "data/controller/logisticSegment/response/dropship_logistic_movements/paged_without_attributes.json"
            ),
            Arguments.of(
                "Постраничный поиск без атрибутов и слишком большим номером страницы - возвращается пустая страница",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .build(),
                PageRequest.of(1, 1),
                "data/controller/logisticSegment/response/dropship_logistic_movements/paged_too_large_page_number.json"
            ),
            Arguments.of(
                "Схлопываем перемещения по партнёру и логистической точке назначения",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .attributes(EnumSet.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.TO_PARTNER_LOGISTIC_POINT_DISTINCT
                    ))
                    .build(),
                PageRequest.of(0, 1),
                "data/controller/logisticSegment/response/dropship_logistic_movements/"
                    + "paged_to_partner_logistic_point_distinct.json"
            ),
            Arguments.of(
                "Схлопываем перемещения по партнёру и логистической точке назначения - слишком большой номер страницы",
                SearchDropshipLogisticMovementsFilter.builder()
                    .fromPartnerIds(Set.of(1L))
                    .toPartnerIds(Set.of(2L))
                    .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
                    .status(ActivityStatus.ACTIVE)
                    .attributes(EnumSet.of(
                        SearchDropshipLogisticMovementsFilter.Attribute.TO_PARTNER_LOGISTIC_POINT_DISTINCT
                    ))
                    .build(),
                PageRequest.of(1, 1),
                "data/controller/logisticSegment/response/dropship_logistic_movements/paged_too_large_page_number.json"
            )
        );
    }
}
