package ru.yandex.market.logistics.logistrator.controller

import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.management.entity.page.PageRequest
import ru.yandex.market.logistics.management.entity.page.PageResult
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.PartnerType
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils
import java.util.stream.Stream

class PartnerRelationControllerSearchTest : AbstractContextualTest() {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchPartnerRelationsProvider")
    @DisplayName("Поиск связей партнеров")
    fun searchPartnerRelations(
        caseName: String,
        searchPartnerRelationFilter: Map<String, List<String>>,
        sortByPath: String?,
        expectedFilter: LogisticSegmentFilter,
        pageRequest: PageRequest,
        lmsClientResponse: PageResult<LogisticSegmentDto>,
        partnersToMock: List<Pair<Long, PartnerType>>,
        segmentsToMock: List<LogisticSegmentDto>,
        expectedResultPath: String
    ) {
        whenever(lmsClient.searchLogisticSegments(eq(expectedFilter), eq(pageRequest))).thenReturn(lmsClientResponse)
        mockGetPartners(partnersToMock)
        mockGetSegments(segmentsToMock)

        val sortBy = sortByPath?.let { IntegrationTestUtils.extractFileContent(sortByPath) }

        mockMvc.perform(
            MockMvcRequestBuilders.get("/partner-relations")
                .queryParams(LinkedMultiValueMap(searchPartnerRelationFilter))
                .queryParam("sortBy", sortBy)
                .queryParam("page", pageRequest.page.toString())
                .queryParam("size", pageRequest.size.toString())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(json().isEqualTo(IntegrationTestUtils.extractFileContent(expectedResultPath)))
    }

    private fun mockGetPartners(partners: List<Pair<Long, PartnerType>>) {
        whenever(lmsClient.searchPartners(
            argThat(ArgumentMatcher<SearchPartnerFilter> {
                it != null && it.ids.containsAll(partners.map { it.first })
            })
        ))
            .thenReturn(
                partners.map {
                    PartnerResponse.newBuilder()
                        .id(it.first)
                        .name("Partner ${it.first}")
                        .partnerType(it.second)
                        .build()
                }
            )
    }

    private fun mockGetSegments(segments: List<LogisticSegmentDto>) {
        whenever(lmsClient.searchLogisticSegments(
            argThat(ArgumentMatcher<LogisticSegmentFilter> {
                it != null && it.ids.containsAll(segments.map { it.id })
            })
        ))
            .thenReturn(
                segments.map { it.setLogisticsPointId(it.id).setName("Segment ${it.id}") }
            )
    }

    companion object {
        @JvmStatic
        private fun searchPartnerRelationsProvider() = Stream.of(
            Arguments.of(
                "Пустой результат",
                mapOf("id" to listOf("1")),
                null,
                LogisticSegmentFilter.builder()
                    .setIds(setOf(1L))
                    .setTypes(setOf(LogisticSegmentType.MOVEMENT))
                    .build(),
                PageRequest(0, 1),
                createPageResult<LogisticSegmentDto>(0, 0, 0, 0, emptyList()),
                emptyList<Pair<Long, PartnerType>>(),
                emptyList<Pair<Long, LogisticSegmentType>>(),
                "response/partner_relation_search/empty.json"
            ),
            Arguments.of(
                "Одна сущность, без сортировки",
                mapOf("id" to listOf("1")),
                null,
                LogisticSegmentFilter.builder()
                    .setIds(setOf(1L))
                    .setTypes(setOf(LogisticSegmentType.MOVEMENT))
                    .build(),
                PageRequest(0, 1),
                createPageResult<LogisticSegmentDto>(0, 1, 1, 1, listOf(
                    createPartnerRelationResponse(1, 100, 1, 2, 2, 3,true)
                )),
                listOf(
                    Pair(1L, PartnerType.FULFILLMENT),
                    Pair(2L, PartnerType.DELIVERY),
                    Pair(100L, PartnerType.DELIVERY)
                ),
                listOf(
                    LogisticSegmentDto().setId(2L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(1L),
                    LogisticSegmentDto().setId(3L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(2L)
                ),
                "response/partner_relation_search/single_partner_relation.json"
            ),
            Arguments.of(
                "Одна сущность, сложный фильтр поиска",
                mapOf(
                    "searchPartnerFromQuery" to listOf("1"),
                    "partnerToType" to listOf("DELIVERY"),
                    "active" to listOf("true")
                ),
                null,
                LogisticSegmentFilter.builder()
                    .setTypes(setOf(LogisticSegmentType.MOVEMENT))
                    .setSearchPartnerFromQuery("1")
                    .setPartnerToType(PartnerType.DELIVERY)
                    .setActive(true)
                    .build(),
                PageRequest(0, 1),
                createPageResult<LogisticSegmentDto>(0, 1, 1, 1, listOf(
                    createPartnerRelationResponse(1, 100, 1, 2, 2, 3, true)
                )),
                listOf(
                    Pair(1L, PartnerType.FULFILLMENT),
                    Pair(2L, PartnerType.DELIVERY),
                    Pair(100L, PartnerType.DELIVERY)
                ),
                listOf(
                    LogisticSegmentDto().setId(2L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(1L),
                    LogisticSegmentDto().setId(3L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(2L)
                ),
                "response/partner_relation_search/single_partner_relation.json"
            ),
            Arguments.of(
                "Несколько сущностей, сортировка по id",
                mapOf("active" to listOf("true")),
                "request/partner_relation_search/order_by_id.json",
                LogisticSegmentFilter.builder()
                    .setTypes(setOf(LogisticSegmentType.MOVEMENT))
                    .setActive(true)
                    .build(),
                PageRequest(2, 4),
                createPageResult<LogisticSegmentDto>(2, 3, 3, 11, listOf(
                    createPartnerRelationResponse(1, 100, 1, 2, 4, 5, true),
                    createPartnerRelationResponse(3, 100, 1, 3, 6, 7, true),
                    createPartnerRelationResponse(2, 100, 2, 3, 8, 9, true)
                )),
                listOf(
                    Pair(1L, PartnerType.FULFILLMENT),
                    Pair(2L, PartnerType.DELIVERY),
                    Pair(3L, PartnerType.DELIVERY),
                    Pair(100L, PartnerType.DELIVERY)
                ),
                listOf(
                    LogisticSegmentDto().setId(4L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(1L),
                    LogisticSegmentDto().setId(5L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(2L),
                    LogisticSegmentDto().setId(6L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(1L),
                    LogisticSegmentDto().setId(7L).setType(LogisticSegmentType.LINEHAUL).setPartnerId(3L),
                    LogisticSegmentDto().setId(8L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(2L),
                    LogisticSegmentDto().setId(9L).setType(LogisticSegmentType.LINEHAUL).setPartnerId(3L)
                ),
                "response/partner_relation_search/multiple_partner_relations_order_by_id.json"
            ),
            Arguments.of(
                "Несколько сущностей, сортировка по типам партнеров и enabled",
                emptyMap<String, List<String>>(),
                "request/partner_relation_search/order_by_from_to_partner_type_enabled.json",
                LogisticSegmentFilter.builder()
                    .setTypes(setOf(LogisticSegmentType.MOVEMENT))
                    .build(),
                PageRequest(0, 10),
                createPageResult<LogisticSegmentDto>(0, 4, 1, 4, listOf(
                    createPartnerRelationResponse(4, 100, 1, 2, 5, 6, true),
                    createPartnerRelationResponse(3, 100, 1, 3, 7, 8, true),
                    createPartnerRelationResponse(1, 100, 2, 5, 9, 10, false),
                    createPartnerRelationResponse(2, 100, 2, 4, 11, 12, true)
                )),
                listOf(
                    Pair(1L, PartnerType.SORTING_CENTER),
                    Pair(2L, PartnerType.FULFILLMENT),
                    Pair(3L, PartnerType.DELIVERY),
                    Pair(4L, PartnerType.DROPSHIP),
                    Pair(5L, PartnerType.DROPSHIP),
                    Pair(100L, PartnerType.DELIVERY)
                ),
                listOf(
                    LogisticSegmentDto().setId(5L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(1L),
                    LogisticSegmentDto().setId(6L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(2L),
                    LogisticSegmentDto().setId(7L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(1L),
                    LogisticSegmentDto().setId(8L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(3L),
                    LogisticSegmentDto().setId(9L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(2L),
                    LogisticSegmentDto().setId(10L).setType(LogisticSegmentType.LINEHAUL).setPartnerId(5L),
                    LogisticSegmentDto().setId(11L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(2L),
                    LogisticSegmentDto().setId(12L).setType(LogisticSegmentType.LINEHAUL).setPartnerId(4L),
                ),
                "response/partner_relation_search/multiple_partner_relations_order_by_from_to_partner_type.json"
            ),
                Arguments.of(
                        "Несколько сущностей, для которых не вернулись сегменты справа или слева",
                        emptyMap<String, List<String>>(),
                        null,
                        LogisticSegmentFilter.builder()
                                .setTypes(setOf(LogisticSegmentType.MOVEMENT))
                                .build(),
                        PageRequest(2, 2),
                        createPageResult<LogisticSegmentDto>(2, 2, 3, 2, listOf(
                                createPartnerRelationResponse(1, 100, 1, 3, 6, 7, true),
                                createPartnerRelationResponse(2, 100, 2, 3, 8, 9, true)
                        )),
                        listOf(
                                Pair(1L, PartnerType.FULFILLMENT),
                                Pair(2L, PartnerType.DELIVERY),
                                Pair(3L, PartnerType.DELIVERY),
                                Pair(100L, PartnerType.DELIVERY)
                        ),
                        listOf(
                                LogisticSegmentDto().setId(6L).setType(LogisticSegmentType.WAREHOUSE).setPartnerId(1L),
                                LogisticSegmentDto().setId(9L).setType(LogisticSegmentType.LINEHAUL).setPartnerId(3L)
                        ),
                        "response/partner_relation_search/partner_relations_without_edges.json"
                )
        )

        private fun createPartnerRelationResponse(
            id: Long,
            partnerId: Long,
            fromPartnerId: Long,
            toPartnerId: Long,
            fromSegmentId: Long,
            toSegmentId: Long,
            active: Boolean
        ) = LogisticSegmentDto()
            .setId(id)
            .setName("Segment $id")
            .setType(LogisticSegmentType.MOVEMENT)
            .setPartnerId(partnerId)
            .setPreviousSegmentPartnerIds(listOf(fromPartnerId))
            .setNextSegmentPartnerIds(listOf(toPartnerId))
            .setPreviousSegmentIds(listOf(fromSegmentId))
            .setNextSegmentIds(listOf(toSegmentId))
            .setServices(listOf(
                LogisticSegmentServiceDto.builder()
                    .setId(1)
                    .setStatus(if (active) ActivityStatus.ACTIVE else ActivityStatus.INACTIVE)
                    .build()
            ))

        private fun <T> createPageResult(
            page: Int,
            size: Int,
            totalPages: Int,
            totalElements: Long,
            elements: List<T>
        ): PageResult<T> {
            val pageResult = PageResult<T>()
            pageResult.page = page
            pageResult.size = size
            pageResult.totalPages = totalPages
            pageResult.totalElements = totalElements
            pageResult.data = elements
            return pageResult
        }
    }
}
