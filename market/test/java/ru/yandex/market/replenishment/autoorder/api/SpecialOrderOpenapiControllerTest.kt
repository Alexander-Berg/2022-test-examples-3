package ru.yandex.market.replenishment.autoorder.api

import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.isIn
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.ControllerTest
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.ApprovalItem
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.ApproveSpecialOrderRequest
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.CreateSpecialOrderItem
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.CreateSpecialOrderRequest
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.DeclineSpecialOrderRequest
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.SpecialOrderApprovalStatus
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.SpecialOrderCreateKey
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.SpecialOrderDateType
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.SpecialOrderType
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.StarTrekTicketUpdateRequest
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.StarTrekTicketUpdateRequestSskuMap
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import ru.yandex.market.replenishment.autoorder.utils.TestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@WithMockLogin
open class SpecialOrderOpenapiControllerTest : ControllerTest() {
    companion object {
        val MOCK_DATE_TIME: LocalDateTime = LocalDateTime.of(2020, 10, 5, 12, 37, 8)
    }

    @Before
    open fun mockDate() {
        setTestTime(MOCK_DATE_TIME)
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.accept.after.csv"]
    )
    open fun testAcceptSpecialOrders_isOkAccepted() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticket1"
                status = SpecialOrderApprovalStatus.FINISHED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        accepted = listOf(
                            createApprovalItem("ssku1", 145),
                            createApprovalItem("ssku1", 147)
                        )
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.demandIds.size()").value(4))
            .andExpect(jsonPath("$.demandIds[0]").value(1))
            .andExpect(jsonPath("$.demandIds[1]").value(2))
            .andExpect(jsonPath("$.demandIds[2]").value(3))
            .andExpect(jsonPath("$.demandIds[3]").value(4))
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.partiallyAccepted.after.csv"]
    )
    open fun testAcceptSpecialOrders_isOkPartially() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticket1"
                status = SpecialOrderApprovalStatus.FINISHED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        accepted = listOf(createApprovalItem("ssku1", 145))
                        declined = listOf(
                            createApprovalItem("ssku1", 147),
                            createApprovalItem("ssku2", 145),
                            createApprovalItem("ssku2", 147)
                        )
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.demandIds.size()").value(2))
            .andExpect(jsonPath("$.demandIds[0]").value(1))
            .andExpect(jsonPath("$.demandIds[1]").value(2))
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.cancel.after.csv"]
    )
    open fun testAcceptSpecialOrders_isOkDeclined() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticket1"
                status = SpecialOrderApprovalStatus.FINISHED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        declined = listOf(createApprovalItem("ssku2", 145))
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.demandIds").isEmpty)
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.cancel.after.csv"]
    )
    open fun testCancelSpecialOrders_isOkDeclined() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticket1"
                status = SpecialOrderApprovalStatus.CANCELLED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        accepted = listOf(createApprovalItem("ssku1", 145))
                        declined = listOf(createApprovalItem("ssku2", 145))
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.demandIds").isEmpty)
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
    )
    open fun testAcceptSpecialOrdersSlowTrack_isGone() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticket2"
                status = SpecialOrderApprovalStatus.FINISHED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        accepted = listOf(createApprovalItem("ssku1", 145))
                        declined = listOf(createApprovalItem("ssku2", 145))
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isGone)
            .andExpect(jsonPath("$.demandIds").isEmpty())
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
    )
    open fun testAcceptSpecialOrdersSskusAbsence_isBadRequest() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticket1"
                status = SpecialOrderApprovalStatus.FINISHED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isIAmATeapot)
            .andExpect(
                jsonPath("$.message").value(
                    "There must be accepted of declined sskus for FINISHED status of the ticket"
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
    )
    open fun testAcceptSpecialOrdersTicketIdIsNull_isBadRequest() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                status = SpecialOrderApprovalStatus.FINISHED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        accepted = listOf(createApprovalItem("ssku1", 145))
                        declined = listOf(createApprovalItem("ssku2", 145))
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Поле starTrekTicketUpdateRequest.id must not be null"))
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
    )
    open fun testAcceptSpecialOrdersIntersectOfAcceptedAndDeclinedSskus_isBadRequest() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticket1"
                status = SpecialOrderApprovalStatus.FINISHED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        accepted = listOf(createApprovalItem("ssku1", 145))
                        declined = listOf(
                            createApprovalItem("ssku2", 145),
                            createApprovalItem("ssku1", 145)
                        )
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(
                jsonPath("$.message").value(
                    "Accepted sskus don't have to intersect to declined sskus!"
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
    )
    open fun testAcceptSpecialOrdersAbsenceOfTicketStatus_isBadRequest() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticket1"
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        accepted = listOf(createApprovalItem("ssku1", 145))
                        declined = listOf(createApprovalItem("ssku2", 145))
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                jsonPath("$.message").value(
                    "Поле starTrekTicketUpdateRequest.status must not be null"
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.before.csv"],
    )
    open fun testAcceptSpecialOrdersAbsenceOfTicketId_isBadRequest() {
        val dto = StarTrekTicketUpdateRequest()
            .apply {
                id = "ticketNotExists"
                status = SpecialOrderApprovalStatus.FINISHED
                sskuMap = StarTrekTicketUpdateRequestSskuMap()
                    .apply {
                        accepted = listOf(createApprovalItem("ssku1", 145))
                        declined = listOf(createApprovalItem("ssku2", 145))
                    }
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isIAmATeapot)
            .andExpect(
                jsonPath("$.message").value(
                    "Special order request for ticket with id ticketNotExists doesn't exist"
                )
            )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/finalize")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                jsonPath("$.message").value(
                    "Special order request for ticket with id ticketNotExists doesn't exist"
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.declineRest.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.declineRest.after.csv"]
    )
    open fun testAcceptSpecialOrders_declineRest_isOk() {
        val dto = DeclineSpecialOrderRequest()
            .apply {
                ticketId = "ticket1"
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/decline_rest")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.message").isEmpty)
    }

    @Test
    @DbUnitDataSet(before = ["SpecialOrderOpenapiControllerTest.declineRest.before.csv"])
    open fun testAcceptSpecialOrders_declineRestWithoutTicket_isBadRequest() {
        val dto = DeclineSpecialOrderRequest()
            .apply {
                ticketId = "ticketNotExists"
            }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/decline_rest")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isIAmATeapot)
            .andExpect(
                jsonPath("$.message")
                    .value("Special order request for ticket with id ticketNotExists doesn't exist")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.approve.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.approveAny.after.csv"]
    )
    open fun testAcceptSpecialOrders_approveAny_isOkPartially() {
        val dto = ApproveSpecialOrderRequest()
            .ticketId("ticket1")
            .keys(
                listOf(
                    SpecialOrderCreateKey().ssku("ssku1").warehouseId(145),
                    SpecialOrderCreateKey().ssku("ssku1").warehouseId(147)
                )
            )
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/approve_any")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.approvedItems.size()").value(2))
            .andExpect(jsonPath("$.declinedItems.size()").value(1))

            .andExpect(jsonPath("$.approvedItems[0].key.ssku").value("ssku1"))
            .andExpect(jsonPath("$.approvedItems[0].key.warehouseId").value(145))
            .andExpect(jsonPath("$.approvedItems[0].deliveryDate", notNullValue()))

            .andExpect(jsonPath("$.approvedItems[1].key.ssku").value("ssku1"))
            .andExpect(jsonPath("$.approvedItems[1].key.warehouseId").value(145))
            .andExpect(jsonPath("$.approvedItems[1].deliveryDate", notNullValue()))

            .andExpect(jsonPath("$.approvedItems[*].demandId", containsInAnyOrder(1, 2)))

            .andExpect(jsonPath("$.declinedItems[0].key.ssku").value("ssku1"))
            .andExpect(jsonPath("$.declinedItems[0].key.warehouseId").value(147))
            .andExpect(
                jsonPath("$.declinedItems[0].error")
                    .value("Для поставщика 'ООО Кораблик-Р', склада 'Ростов', типа поставки 'Прямая' БЕЗ группы отсутствуют логистические параметры")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.approve.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.approveAll.after.csv"]
    )
    open fun testAcceptSpecialOrders_approveAll_isOkWithDeclined() {
        val dto = ApproveSpecialOrderRequest()
            .ticketId("ticket1")
            .keys(
                listOf(
                    SpecialOrderCreateKey().ssku("ssku1").warehouseId(145),
                    SpecialOrderCreateKey().ssku("ssku1").warehouseId(147)
                )
            )
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/approve_all")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.approvedItems.size()").value(0))
            .andExpect(jsonPath("$.declinedItems.size()").value(1))

            .andExpect(jsonPath("$.declinedItems[0].key.ssku").value("ssku1"))
            .andExpect(jsonPath("$.declinedItems[0].key.warehouseId").value(147))
            .andExpect(
                jsonPath("$.declinedItems[0].error")
                    .value("Для поставщика 'ООО Кораблик-Р', склада 'Ростов', типа поставки 'Прямая' БЕЗ группы отсутствуют логистические параметры")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.create.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.createAny.after.csv"]
    )
    open fun testAcceptSpecialOrders_createAny_isOkPartially() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/create_any")
                .content(TestUtils.dtoToString(getCreateRequest()))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.approvedItems.size()").value(2))
            .andExpect(jsonPath("$.declinedItems.size()").value(2))

            .andExpect(jsonPath("$.approvedItems[*].demandId", containsInAnyOrder(1, 2)))
            .andExpect(jsonPath("$.approvedItems[*].deliveryDate", everyItem(not(isIn(listOf(nullValue()))))))

            .andExpect(
                jsonPath("$.declinedItems[0].error")
                    .value("Отсутствуют лог. параметры для поставщика ООО Кораблик-Р id 1, склада 147 без группы лог. параметров")
            )
            .andExpect(
                jsonPath("$.declinedItems[1].error")
                    .value("Отсутствуют лог. параметры для поставщика ООО Кораблик-Р id 1, склада 147 без группы лог. параметров")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.setSupplyRoute.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.setSupplyRoute.after.csv"]
    )
    open fun testAcceptSpecialOrders_setSupplyRoute() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/create_any")
                .content(TestUtils.dtoToString(getCreateRequest()))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.approvedItems.size()").value(4))
            .andExpect(jsonPath("$.approvedItems[*].demandId", containsInAnyOrder(1, 2, 3, 4)))
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.create.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.createAll.after.csv"]
    )
    open fun testAcceptSpecialOrders_createAll_isOk() {
        val dto = CreateSpecialOrderRequest()
            .ticketId("ticket1")
            .specialOrderItems(
                listOf(
                    CreateSpecialOrderItem()
                        .key(SpecialOrderCreateKey().ssku("000093.ssku1").warehouseId(145))
                        .orderType(SpecialOrderType.NEW)
                        .price(BigDecimal.valueOf(500.34))
                        .quantity(5000)
                        .quantum(0)
                        .orderDateType(SpecialOrderDateType.TODAY)
                        .deliveryDate(LocalDate.of(2020, 10, 7)),
                    CreateSpecialOrderItem()
                        .key(SpecialOrderCreateKey().ssku("000093.ssku2").warehouseId(145))
                        .orderType(SpecialOrderType.SEASONAL)
                        .price(BigDecimal.valueOf(500.34))
                        .quantity(100)
                        .quantum(null)
                        .orderDateType(SpecialOrderDateType.TODAY)
                        .deliveryDate(LocalDate.of(2020, 10, 14)),
                    CreateSpecialOrderItem()
                        .key(SpecialOrderCreateKey().ssku("000093.ssku3").warehouseId(145))
                        .orderType(SpecialOrderType.NEW)
                        .price(BigDecimal.valueOf(1.34))
                        .quantity(65)
                        .quantum(5)
                        .orderDateType(SpecialOrderDateType.TODAY),
                    CreateSpecialOrderItem()
                        .key(SpecialOrderCreateKey().ssku("000093.ssku4").warehouseId(145))
                        .orderType(SpecialOrderType.NEW)
                        .price(BigDecimal.valueOf(2.34))
                        .quantity(56)
                        .orderDateType(SpecialOrderDateType.LOG_PARAM)
                        .quantum(1)
                        .deliveryDate(LocalDate.of(2020, 10, 14)),
                )
            )
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/create_all")
                .content(TestUtils.dtoToString(dto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.approvedItems.size()").value(4))
            .andExpect(jsonPath("$.declinedItems.size()").value(0))
            .andExpect(jsonPath("$.approvedItems[*].demandId", containsInAnyOrder(1, 1, 2, 3)))
    }

    @Test
    @DbUnitDataSet(
        before = ["SpecialOrderOpenapiControllerTest.create.before.csv"],
        after = ["SpecialOrderOpenapiControllerTest.createAll_wrong.after.csv"]
    )
    open fun testAcceptSpecialOrders_createAll_isWrong() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/special-order-request/create_all")
                .content(TestUtils.dtoToString(getCreateRequest()))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.approvedItems.size()").value(0))
            .andExpect(jsonPath("$.declinedItems.size()").value(2))
            .andExpect(
                jsonPath("$.declinedItems[?(@.key.ssku=='000093.ssku1' && @.key.warehouseId==147)].error")
                    .value("Отсутствуют лог. параметры для поставщика ООО Кораблик-Р id 1, склада 147 без группы лог. параметров")
            )
            .andExpect(
                jsonPath("$.declinedItems[?(@.key.ssku=='000093.ssku2' && @.key.warehouseId==147)].error")
                    .value("Отсутствуют лог. параметры для поставщика ООО Кораблик-Р id 1, склада 147 без группы лог. параметров")
            )
    }

    private fun createApprovalItem(ssku: String, warehouseId: Long): ApprovalItem =
        ApprovalItem().apply {
            this.ssku = ssku
            this.warehouseId = warehouseId
        }

    private fun getCreateRequest(): CreateSpecialOrderRequest =
        CreateSpecialOrderRequest()
            .ticketId("ticket1")
            .specialOrderItems(
                listOf(
                    CreateSpecialOrderItem()
                        .key(SpecialOrderCreateKey().ssku("000093.ssku1").warehouseId(145))
                        .orderType(SpecialOrderType.NEW)
                        .price(BigDecimal.valueOf(500.34))
                        .quantity(5000)
                        .orderDateType(SpecialOrderDateType.TODAY)
                        .deliveryDate(LocalDate.of(2020, 9, 7)),
                    CreateSpecialOrderItem()
                        .key(SpecialOrderCreateKey().ssku("000093.ssku2").warehouseId(145))
                        .orderType(SpecialOrderType.SEASONAL)
                        .price(BigDecimal.valueOf(500.34))
                        .quantity(100)
                        .orderDateType(SpecialOrderDateType.TODAY)
                        .deliveryDate(LocalDate.of(2020, 9, 14)),
                    CreateSpecialOrderItem()
                        .key(SpecialOrderCreateKey().ssku("000093.ssku1").warehouseId(147))
                        .orderType(SpecialOrderType.NEW)
                        .price(BigDecimal.valueOf(32.12))
                        .quantity(5000)
                        .orderDateType(SpecialOrderDateType.LOG_PARAM)
                        .deliveryDate(LocalDate.of(2020, 9, 7)),
                    CreateSpecialOrderItem()
                        .key(SpecialOrderCreateKey().ssku("000093.ssku2").warehouseId(147))
                        .orderType(SpecialOrderType.NEW)
                        .price(BigDecimal.valueOf(87.91))
                        .quantity(100)
                        .orderDateType(SpecialOrderDateType.LOG_PARAM)
                        .deliveryDate(LocalDate.of(2020, 9, 14)),
                )
            )
}
