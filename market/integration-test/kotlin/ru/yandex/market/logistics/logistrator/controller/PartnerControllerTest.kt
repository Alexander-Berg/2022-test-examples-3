package ru.yandex.market.logistics.logistrator.controller

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.management.entity.page.PageRequest
import ru.yandex.market.logistics.management.entity.page.PageResult
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse
import ru.yandex.market.logistics.management.entity.type.PartnerStatus
import ru.yandex.market.logistics.management.entity.type.PartnerType
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent
import java.util.stream.Stream

internal class PartnerControllerTest : AbstractContextualTest() {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchPartnersProvider")
    @DisplayName("Поиск партнеров")
    fun searchPartners(
        caseName: String,
        searchPartnerFilter: Map<String, List<String>>,
        sortByPath: String?,
        expectedFilter: SearchPartnerFilter,
        pageRequest: PageRequest,
        lmsClientResponse: PageResult<PartnerResponse>,
        expectedResultPath: String
    ) {
        whenever(lmsClient.searchPartners(eq(expectedFilter), eq(pageRequest))).thenReturn(lmsClientResponse)

        val sortBy = sortByPath?.let { extractFileContent(sortByPath) }

        mockMvc.perform(get("/partner")
            .queryParams(LinkedMultiValueMap(searchPartnerFilter))
            .queryParam("sortBy", sortBy)
            .queryParam("page", pageRequest.page.toString())
            .queryParam("size", pageRequest.size.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent(expectedResultPath)))
    }

    companion object {
        @JvmStatic
        private fun searchPartnersProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Пустой результат",
                mapOf("id" to listOf("1")),
                null,
                SearchPartnerFilter.builder()
                    .setIds(setOf(1L))
                    .build(),
                PageRequest(0, 10),
                createPageResult(0, 0, 0, 0, emptyList<PartnerResponse>()),
                "response/partner_search/empty.json"
            ),
            Arguments.of(
                "Одна сущность, без сортировки",
                mapOf("id" to listOf("1")),
                null,
                SearchPartnerFilter.builder()
                    .setIds(setOf(1L))
                    .build(),
                PageRequest(0, 10),
                createPageResult(0, 1, 1, 1, listOf(
                    createPartnerResponse(
                        1L,
                        PartnerType.FULFILLMENT,
                        null,
                        "FulfillmentService1",
                        "Fulfillment service 1",
                        PartnerStatus.ACTIVE
                    )
                )),
                "response/partner_search/single_partner.json"
            ),
            Arguments.of(
                "Несколько сущностей, сортировка по id, типу и статусу",
                emptyMap<String, List<String>>(),
                "request/partner_search/order_by_type_status.json",
                SearchPartnerFilter.builder().build(),
                PageRequest(0, 10),
                createPageResult(0, 3, 1, 3, listOf(
                    createPartnerResponse(
                        2L,
                        PartnerType.DROPSHIP,
                        null,
                        "DropshipService1",
                        "Dropship service 1",
                        PartnerStatus.ACTIVE
                    ),
                    createPartnerResponse(
                        1L,
                        PartnerType.FULFILLMENT,
                        null,
                        "FulfillmentService1",
                        "Fulfillment service 1",
                        PartnerStatus.ACTIVE
                    ),
                    createPartnerResponse(
                        3L,
                        PartnerType.DROPSHIP,
                        null,
                        "DropshipService2",
                        "Dropship service 2",
                        PartnerStatus.INACTIVE
                    )
                )),
                "response/partner_search/multiple_partners_order_by_id_type_status.json"
            ),
            Arguments.of(
                "Поиск по статусу и имени, сортировка по идентификатору",
                mapOf(
                    "name" to listOf("Dropship"),
                    "status" to listOf("ACTIVE")
                ),
                "request/partner_search/order_by_id.json",
                SearchPartnerFilter.builder()
                    .setSearchQuery("Dropship")
                    .setStatuses(setOf(PartnerStatus.ACTIVE))
                    .build(),
                PageRequest(0, 10),
                createPageResult(0, 2, 1, 2, listOf(
                    createPartnerResponse(
                        3L,
                        PartnerType.DROPSHIP,
                        null,
                        "DropshipService2",
                        "Dropship service 2",
                        PartnerStatus.ACTIVE
                    ),
                    createPartnerResponse(
                        2L,
                        PartnerType.DROPSHIP,
                        null,
                        "DropshipService1",
                        "Dropship service 1",
                        PartnerStatus.ACTIVE
                    )
                )),
                "response/partner_search/multiple_partners_dropship_order_by_id.json"
            ),
            Arguments.of(
                "Поиск по имени, сортировка по типу и подтипу",
                mapOf("name" to listOf("Delivery")),
                "request/partner_search/order_by_type_subtype.json",
                SearchPartnerFilter.builder()
                    .setSearchQuery("Delivery")
                    .build(),
                PageRequest(2, 4),
                createPageResult(2, 4, 3, 12, listOf(
                    createPartnerResponse(
                        1L,
                        PartnerType.OWN_DELIVERY,
                        null,
                        "OwnDeliveryService1",
                        "Own Delivery service 1",
                        PartnerStatus.ACTIVE
                    ),
                    createPartnerResponse(
                        2L,
                        PartnerType.DELIVERY,
                        null,
                        "DeliveryService1",
                        "Delivery service 1",
                        PartnerStatus.ACTIVE
                    ),
                    createPartnerResponse(
                        3L,
                        PartnerType.DELIVERY,
                        PartnerSubtypeResponse.newBuilder()
                            .id(4)
                            .name("Subtype 4")
                            .build(),
                        "DeliveryService2",
                        "Delivery service 2",
                        PartnerStatus.ACTIVE
                    ),
                    createPartnerResponse(
                        4L,
                        PartnerType.DELIVERY,
                        PartnerSubtypeResponse.newBuilder()
                            .id(1)
                            .name("Subtype 1")
                            .build(),
                        "DeliveryService3",
                        "Delivery service 3",
                        PartnerStatus.ACTIVE
                    )
                )),
                "response/partner_search/multiple_partners_delivery_order_by_type_subtype.json"
            )
        )

        private fun createPartnerResponse(
            id: Long,
            partnerType: PartnerType,
            partnerSubtype: PartnerSubtypeResponse?,
            name: String,
            readableName: String,
            status: PartnerStatus
        ) = PartnerResponse.newBuilder()
            .id(id)
            .partnerType(partnerType)
            .subtype(partnerSubtype)
            .name(name)
            .readableName(readableName)
            .status(status)
            .locationId(255)
            .build()

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
