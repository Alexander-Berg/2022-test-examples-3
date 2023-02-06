package ru.yandex.market.pricingmgmt.api.journal

import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers.hasItems
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationFilter
import ru.yandex.market.pricingmgmt.model.dto.CrossedOutBoundsForDcoExcelDTO
import ru.yandex.market.pricingmgmt.model.dto.CrossedOutLowerPriceExcelDTO
import ru.yandex.market.pricingmgmt.model.dto.PriceDataDTO
import ru.yandex.market.pricingmgmt.model.dto.PriceErrorExcelDTO
import ru.yandex.market.pricingmgmt.model.dto.PriceExcelDTO
import ru.yandex.market.pricingmgmt.model.dto.PriorityPriceDTO
import ru.yandex.market.pricingmgmt.model.dto.RRPExcelDTO
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.service.excel.core.ExcelHelper
import ru.yandex.market.pricingmgmt.service.excel.meta.PriceErrorMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.PriceExcelMetaData
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil.createJsonDateTime
import ru.yandex.mj.generated.server.model.ApprovalParameters
import ru.yandex.mj.generated.server.model.CancelPricesDto
import ru.yandex.mj.generated.server.model.CreateJournalDto
import ru.yandex.mj.generated.server.model.PriceType
import ru.yandex.mj.generated.server.model.ScheduleDto
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
@WithMockUser(
    username = PassportAuthenticationFilter.LOCAL_DEV,
    roles = ["PRICING_MGMT_ACCESS", "VIEW_PRICES_JOURNALS_KOMANDA1", "VIEW_PRICES_JOURNALS_KOMANDA2"]
)
class JournalApiTest(
    @Autowired val excelHelper: ExcelHelper<PriceExcelDTO>,
    @Autowired val priceErrorExcelHelper: ExcelHelper<PriceErrorExcelDTO>,
    @Autowired private val timeService: TimeService,
) : ControllerTest() {

    @DbUnitDataSet(
        before = ["JournalApiTest.journalCreationTest.before.csv"],
        after = ["JournalApiTest.journalCreationTest.after.csv"]
    )
    @Test
    fun testCreateJournal() {
        val zoneOffset = OffsetDateTime.now().offset
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        val schedules = listOf(
            ScheduleDto().dayOfWeek(ScheduleDto.DayOfWeekEnum.MONDAY)
                .startAt(LocalTime.of(8, 0, 0).format(formatter))
                .endAt(LocalTime.of(17, 0, 0).format(formatter)),
            ScheduleDto().dayOfWeek(ScheduleDto.DayOfWeekEnum.FRIDAY)
                .startAt(LocalTime.of(10, 0, 0).format(formatter))
                .endAt(LocalTime.of(20, 0, 0).format(formatter))
        )

        val dto = CreateJournalDto()
            .startAt(OffsetDateTime.of(2021, 12, 23, 8, 0, 0, 0, zoneOffset))
            .endAt(OffsetDateTime.of(2022, 1, 23, 12, 0, 0, 0, zoneOffset))
            .priceType(PriceType.PRIORITY_PRICE)
            .warehouseIds(mutableListOf(1, 2))
            .schedules(schedules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/")
                .contentType("application/json")
                .content(dtoToString(dto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @DbUnitDataSet(
        before = ["JournalApiTest.journalCreationTest.before.csv"],
        after = ["JournalApiTest.journalCreationTest_WithoutWarehouses.after.csv"]
    )
    @Test
    fun testCreateJournal_NoWarehouses() {
        val zoneOffset = OffsetDateTime.now().offset
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        val schedules = listOf(
            ScheduleDto().dayOfWeek(ScheduleDto.DayOfWeekEnum.MONDAY)
                .startAt(LocalTime.of(8, 0, 0).format(formatter))
                .endAt(LocalTime.of(17, 0, 0).format(formatter)),
            ScheduleDto().dayOfWeek(ScheduleDto.DayOfWeekEnum.FRIDAY)
                .startAt(LocalTime.of(10, 0, 0).format(formatter))
                .endAt(LocalTime.of(20, 0, 0).format(formatter))
        )

        val dto = CreateJournalDto()
            .startAt(OffsetDateTime.of(2021, 12, 23, 8, 0, 0, 0, zoneOffset))
            .endAt(OffsetDateTime.of(2022, 1, 23, 12, 0, 0, 0, zoneOffset))
            .priceType(PriceType.PRIORITY_PRICE)
            .schedules(schedules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/")
                .contentType("application/json")
                .content(dtoToString(dto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @DbUnitDataSet(
        before = ["JournalApiTest.journalCreationTest.dco.before.csv"],
        after = ["JournalApiTest.journalCreationTest.dco.after.csv"]
    )
    @Test
    fun testCreateJournal_dcoLowerUpperBounds() {
        val zoneOffset = OffsetDateTime.now().offset
        val types = listOf(PriceType.DCO_LOWER_BOUND, PriceType.DCO_UPPER_BOUND)

        for (type in types) {
            val dto = CreateJournalDto()
                .startAt(OffsetDateTime.of(2021, 12, 23, 8, 0, 0, 0, zoneOffset))
                .endAt(OffsetDateTime.of(2022, 1, 23, 12, 0, 0, 0, zoneOffset))
                .priceType(type)
                .warehouseIds(mutableListOf(1, 2))

            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/journals/")
                    .contentType("application/json")
                    .content(dtoToString(dto))
            ).andExpect(MockMvcResultMatchers.status().isOk)
        }
    }

    @DbUnitDataSet(
        before = ["JournalApiTest.journalCreationTest.before.csv"]
    )
    @Test
    fun testCreateJournal_SourcePromoId_Forbidden() {
        val dto = CreateJournalDto()
            .priceType(PriceType.PRIORITY_PRICE)
            .sourcePromoId("sourcePromoId")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/")
                .contentType("application/json")
                .content(dtoToString(dto))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Идентификатор промо не доступен для указанного типа загрузки")
            )
    }

    @DbUnitDataSet(
        before = ["JournalApiTest.journalCreationTest.before.csv"]
    )
    @Test
    fun testCreateJournal_SourcePromoId_Incorrect() {
        val dto = CreateJournalDto()
            .priceType(PriceType.CROSSED_OUT_AND_SALE_PRICE)
            .sourcePromoId("incorrect")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/")
                .contentType("application/json")
                .content(dtoToString(dto))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Unknown source of promoId: incorrect")
            )
    }

    @DbUnitDataSet(
        before = ["JournalApiTest.journalCreationTest.before.csv"]
    )
    @Test
    fun testCreateJournal_SourcePromoId_Success() {
        val dto = CreateJournalDto()
            .priceType(PriceType.CROSSED_OUT_AND_SALE_PRICE)
            .sourcePromoId("#123456")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/")
                .contentType("application/json")
                .content(dtoToString(dto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetJournals() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals?departmentId=2")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].priceType").value("PRIORITY_PRICE"))
            .andExpect(jsonPath("$[0].warehouseIds[0]").value(1))
            .andExpect(jsonPath("$[0].warehouseIds[1]").value(2))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].login").value("ivan"))
            .andExpect(jsonPath("$[0].startAt").value(createJsonDateTime(2021, 12, 23, 8, 0, 0)))
            .andExpect(jsonPath("$[0].endAt").value(createJsonDateTime(2022, 1, 23, 12, 0, 0)))
            .andExpect(jsonPath("$[0].createdAt").value(createJsonDateTime(2021, 12, 23, 1, 0, 0)))
            .andExpect(jsonPath("$[0].sourcePromoId").value("#123"))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetJournalsByIds() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals?ids=1")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].priceType").value("PRIORITY_PRICE"))
            .andExpect(jsonPath("$[0].warehouseIds[0]").value(1))
            .andExpect(jsonPath("$[0].warehouseIds[1]").value(2))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].login").value("ivan"))
            .andExpect(jsonPath("$[0].startAt").value(createJsonDateTime(2021, 12, 23, 8, 0, 0)))
            .andExpect(jsonPath("$[0].endAt").value(createJsonDateTime(2022, 1, 23, 12, 0, 0)))
            .andExpect(jsonPath("$[0].createdAt").value(createJsonDateTime(2021, 12, 23, 1, 0, 0)))
            .andExpect(jsonPath("$[0].sourcePromoId").value("#123"))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetJournalsByPrefix() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals?prefix=test")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].priceType").value("PRIORITY_PRICE"))
            .andExpect(jsonPath("$[0].warehouseIds[0]").value(1))
            .andExpect(jsonPath("$[0].warehouseIds[1]").value(2))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].login").value("ivan"))
            .andExpect(jsonPath("$[0].startAt").value(createJsonDateTime(2021, 12, 23, 8, 0, 0)))
            .andExpect(jsonPath("$[0].endAt").value(createJsonDateTime(2022, 1, 23, 12, 0, 0)))
            .andExpect(jsonPath("$[0].createdAt").value(createJsonDateTime(2021, 12, 23, 1, 0, 0)))
            .andExpect(jsonPath("$[0].sourcePromoId").value("#123"))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.nulldept.before.csv"])
    @Test
    fun testGetJournalsByIdsNullDepartment() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals?ids=1")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].priceType").value("PRIORITY_PRICE"))
            .andExpect(jsonPath("$[0].warehouseIds[0]").value(1))
            .andExpect(jsonPath("$[0].warehouseIds[1]").value(2))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].login").value("ivan"))
            .andExpect(jsonPath("$[0].startAt").value(createJsonDateTime(2021, 12, 23, 8, 0, 0)))
            .andExpect(jsonPath("$[0].endAt").value(createJsonDateTime(2022, 1, 23, 12, 0, 0)))
            .andExpect(jsonPath("$[0].createdAt").value(createJsonDateTime(2021, 12, 23, 1, 0, 0)))
            .andExpect(jsonPath("$[0].sourcePromoId").value("#123"))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.roleMissing.before.csv"])
    @Test
    fun testGetJournalsMissedRole() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/")
                .contentType("application/json")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(jsonPath("$").value("Пожалуйста выберите департамент"))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetJournalPagination() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/?departmentId=1&page=2&count=2")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].id", hasItems(2, 3)))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetJournalForUser() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/?departmentId=1&catmanId=1")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andDo { println(it.response.contentAsString) }
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].id", hasItems(3, 5)))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetFilteredJournalsByWarehouseId() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/?warehouseId=1&departmentId=1")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(3L))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetJournalCount() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/count")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(0))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetJournalCountById() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/count?ids=1")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(1))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetJournalCountByPrefix() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/count?prefix=test")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(1))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetFilteredJournalsCountByWarehouseId() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/count/?departmentId=2&warehouseId=1")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(1))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.before.csv"])
    @Test
    fun testGetFilteredJournalsCountByDepartmentId() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/count/?departmentId=2")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(1))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournals.roleMissing.before.csv"])
    @Test
    fun testGetFilteredJournalsCountMissedRole() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/count/?departmentId=1")
                .contentType("application/json")
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(jsonPath("$.rolesMissing.length()").value(1))
            .andExpect(jsonPath("$.rolesMissing[0].name").value("ROLE_VIEW_PRICES_JOURNALS_KOMANDA1"))
            .andExpect(jsonPath("$.rolesMissing[0].description").value("Просмотр ценовых журналов команда1"))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournalHistory.before.csv"])
    @Test
    fun testGetJournalHistory() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/history")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].createdAt").value("2021-11-29 08:00:00"))
    }

    @DbUnitDataSet(before = ["JournalApiTest.getJournalSchedule.before.csv"])
    @Test
    fun testGetJournalSchedule() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/schedule")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].journalId").value(1))
            .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
            .andExpect(jsonPath("$[0].startAt").value("08:00:00"))
            .andExpect(jsonPath("$[0].endAt").value("17:00:00"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].journalId").value(1))
            .andExpect(jsonPath("$[1].dayOfWeek").value("FRIDAY"))
            .andExpect(jsonPath("$[1].startAt").value("10:00:00"))
            .andExpect(jsonPath("$[1].endAt").value("20:00:00"))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices.before.csv"],
        after = ["JournalApiTest.importPrices_rrp.after.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_rrp() {
        uploadFileWithComment(
            1,
            "/xlsx-template/rrp.xlsx",
            "comment 1"
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices.before.csv"],
        after = ["JournalApiTest.importPrices_sskuDuplicates.after.csv"],
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_sskuDuplicatesFound() {
        uploadFileWithComment(
            1,
            "/xlsx-template/rrp-duplicates.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk).andExpect(jsonPath("$.id").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.testImportPricesRrpHasLowMarginality.before.csv"],
        after = ["JournalApiTest.testImportPricesRrpHasLowMarginality.after.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPricesRrpHasLowMarginality() {
        uploadFileWithComment(
            1,
            "/xlsx-template/rrp.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_failed.before.csv"],
        after = ["JournalApiTest.importPrices_failed.after.csv"],
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_rrp_validation_failed() {
        uploadFileWithComment(
            1,
            "/xlsx-template/rrp.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk).andExpect(jsonPath("$.id").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_failed_no_pp.before.csv"],
        after = ["JournalApiTest.importPrices_failed_no_pp.after.csv"],
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_rrp_validation_failed_no_pp() {
        uploadFileWithComment(
            1,
            "/xlsx-template/rrp.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk).andExpect(jsonPath("$.id").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_crossedOutLower_failed.before.csv"],
        after = ["JournalApiTest.importPrices_crossedOutLower_failed.after.csv"],
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_crossedOutLower_validation_failed() {
        uploadFileWithComment(
            1,
            "/xlsx-template/crossed_out_and_sale_price_failed.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk).andExpect(jsonPath("$.id").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices.before.csv"]
    )
    fun testImportPrices_wrongFile() {
        uploadFileWithComment(
            2,
            "/xlsx-template/rrp.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(jsonPath("$").value("Неправильная структура файла"))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices.noRoles_before.csv"],
        after = ["JournalApiTest.importPrices.noRoles_after.csv"],
    )
    fun testImportPrices_noCatteamRoles() {
        uploadFileWithComment(
            1,
            "/xlsx-template/rrp.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk).andExpect(jsonPath("$.id").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_bounds_for_dco.before.csv"],
        after = ["JournalApiTest.importPrices_bounds_for_dco.after.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_crossed_out_bounds_for_dco() {
        uploadFileWithComment(
            2,
            "/xlsx-template/crossed_out_and_bounds_for_dco.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_sale_price.before.csv"],
        after = ["JournalApiTest.importPrices_sale_price.after.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_crossed_out_sale_price() {
        uploadFileWithComment(
            3,
            "/xlsx-template/crossed_out_and_sale_price.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    private fun uploadFileWithComment(id: Long, filename: String, comment: String?): ResultActions {
        val bytes = javaClass.getResourceAsStream(filename)?.readAllBytes()
        val file = MockMultipartFile("excelFile", filename, "application/vnd.ms-excel", bytes)
        val commentFile = MockMultipartFile(
            "comment", null, "text/plain", comment?.toByteArray()
        )
        return mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/journals/${id}/excel")
                .file(file).file(commentFile)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_rrp.after.csv"],
    )
    fun testExportPricesRrp() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = PriceExcelMetaData.getColumnsForExportPrices(PriceType.RRP)
        val exportedPrices = excelHelper.read(excelData.inputStream(), headers)
        val prices = listOf(
            PriceExcelDTO(
                ssku = "111",
                title = "title1",
                data = PriceDataDTO(rrp = RRPExcelDTO(rrp = BigDecimal(123)))
            ),
            PriceExcelDTO(
                ssku = "112",
                title = "title1",
                data = PriceDataDTO(rrp = RRPExcelDTO(rrp = BigDecimal(124)))
            ),
            PriceExcelDTO(
                ssku = "113",
                title = "title1",
                data = PriceDataDTO(rrp = RRPExcelDTO(rrp = BigDecimal(125)))
            ),
        )
        Assertions.assertThat(exportedPrices).isEqualTo(prices)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_bounds_for_dco.after.csv"],
    )
    fun testExportPricesCrossedOutBoundsFroDco() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/2/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = PriceExcelMetaData.getColumnsForExportPrices(PriceType.CROSSED_OUT_AND_BOUNDS_FOR_DCO)
        val exportedPrices = excelHelper.read(excelData.inputStream(), headers)

        val prices = listOf(
            PriceExcelDTO(
                ssku = "111",
                title = "title1",
                data = PriceDataDTO(
                    crossedOutBoundsForDco = CrossedOutBoundsForDcoExcelDTO(
                        topPrice = BigDecimal(10),
                        upperBound = BigDecimal(30),
                        lowerBound = BigDecimal(20)
                    )
                )
            ),
            PriceExcelDTO(
                ssku = "112",
                title = "title1",
                data = PriceDataDTO(
                    crossedOutBoundsForDco = CrossedOutBoundsForDcoExcelDTO(
                        topPrice = BigDecimal(11),
                        upperBound = BigDecimal(31),
                        lowerBound = BigDecimal(21)
                    )
                )
            ),
            PriceExcelDTO(
                ssku = "113",
                title = "title1",
                data = PriceDataDTO(
                    crossedOutBoundsForDco = CrossedOutBoundsForDcoExcelDTO(
                        topPrice = BigDecimal(12),
                        upperBound = BigDecimal(32),
                        lowerBound = BigDecimal(22)
                    )
                )
            )
        )

        Assertions.assertThat(exportedPrices).isEqualTo(prices)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_sale_price.after.csv"],
    )
    fun testExportPricesCrossedOutLowerPrice() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/3/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = PriceExcelMetaData.getColumnsForExportPrices(PriceType.CROSSED_OUT_AND_SALE_PRICE)
        val exportedPrices = excelHelper.read(excelData.inputStream(), headers)

        val prices = listOf(
            PriceExcelDTO(
                ssku = "111",
                title = "title1",
                data = PriceDataDTO(
                    crossedOutLowerPrice = CrossedOutLowerPriceExcelDTO(
                        topPrice = BigDecimal(10),
                        lowerPrice = BigDecimal(20)
                    )
                )
            ),
            PriceExcelDTO(
                ssku = "112",
                title = "title1",
                data = PriceDataDTO(
                    crossedOutLowerPrice = CrossedOutLowerPriceExcelDTO(
                        topPrice = BigDecimal(11),
                        lowerPrice = BigDecimal(21)
                    )
                )
            ),
            PriceExcelDTO(
                ssku = "113",
                title = "title1",
                data = PriceDataDTO(
                    crossedOutLowerPrice = CrossedOutLowerPriceExcelDTO(
                        topPrice = BigDecimal(12),
                        lowerPrice = BigDecimal(22)
                    )
                )
            )
        )

        Assertions.assertThat(exportedPrices).isEqualTo(prices)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.journalCreationTest.before.csv"],
        after = ["JournalApiTest.removeJournalTest.after.csv"]
    )
    fun testDeleteJournal() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/journals/2")
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_invalid.before.csv"],
        after = ["JournalApiTest.importPrices_invalid.after.csv"]
    )
    fun testImportPrices_catteam_validation_failed() {
        uploadFileWithComment(
            1,
            "/xlsx-template/rrp.xlsx",
            "comment 1"
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.hasLowMarginalSskus_rrp.before.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testHasLowMarginalSskus_rrp() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/low-margin-sskus")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.hasLowMarginSskus").value(true))
            .andExpect(jsonPath("$.departmentId").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.hasLowMarginalSskus_priority.before.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testHasLowMarginalSskus_priority() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/low-margin-sskus")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.hasLowMarginSskus").value(true))
            .andExpect(jsonPath("$.departmentId").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.hasLowMarginalSskus_rrp.before.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testDownloadLowMarginalSskus_rrp() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/low-margin-sskus/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = PriceExcelMetaData.getColumnsForExportLowMarginSskus(PriceType.RRP)
        val data = excelHelper.read(excelData.inputStream(), headers)
        val prices = listOf(
            PriceExcelDTO(
                ssku = "112",
                title = "title1",
                purchasePrice = 90.0,
                data = PriceDataDTO(rrp = RRPExcelDTO(rrp = BigDecimal(124)))
            ),
            PriceExcelDTO(
                ssku = "113",
                title = "title1",
                purchasePrice = 90.0,
                data = PriceDataDTO(rrp = RRPExcelDTO(rrp = BigDecimal(125)))
            )
        )
        Assertions.assertThat(data).isEqualTo(prices)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_rrp.after.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testHasNoLowMarginalSskus_rrp() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/low-margin-sskus")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.hasLowMarginSskus").value(false))
            .andExpect(jsonPath("$.departmentId").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_rrp_withDefaulCatteamSettings.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testHasNoLowMarginalSskus_rrp_withDefaultCatteamSettings() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/low-margin-sskus")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.hasLowMarginSskus").value(false))
            .andExpect(jsonPath("$.departmentId").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_bounds_for_dco.after.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    //bounds_for_dco
    fun testHasNoLowMarginalSskus2() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/2/low-margin-sskus")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.hasLowMarginSskus").value(false))
            .andExpect(jsonPath("$.departmentId").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_sale_price.after.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    //sale_price.after.csv
    fun testDownloadLowMarginalSskus2() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/3/low-margin-sskus/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = PriceExcelMetaData.getColumnsForExportLowMarginSskus(PriceType.CROSSED_OUT_AND_SALE_PRICE)
        val data = excelHelper.read(excelData.inputStream(), headers)
        val prices = listOf(
            PriceExcelDTO(
                ssku = "112",
                title = "title1",
                purchasePrice = 17.0,
                data = PriceDataDTO(
                    crossedOutLowerPrice = CrossedOutLowerPriceExcelDTO(
                        lowerPrice = BigDecimal(21),
                        topPrice = BigDecimal(11)
                    )
                )
            ),
            PriceExcelDTO(
                ssku = "113",
                title = "title1",
                purchasePrice = 18.0,
                data = PriceDataDTO(
                    crossedOutLowerPrice = CrossedOutLowerPriceExcelDTO(
                        lowerPrice = BigDecimal(22),
                        topPrice = BigDecimal(12)
                    )
                )
            ),
        )
        Assertions.assertThat(data).isEqualTo(prices)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.exportErrors.before.csv"],
    )
    fun testExportErrors() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/errors/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = PriceErrorMetaData.getColumnMetaData()
        val actual = priceErrorExcelHelper.read(excelData.inputStream(), headers)
        val expected = listOf(
            PriceErrorExcelDTO(ssku = "112", message = "Не существует"),
            PriceErrorExcelDTO(ssku = "113", message = "Не существует"),
        )
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_priorityPrice.before.csv"],
        after = ["JournalApiTest.importPrices_priorityPrice.after.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_priorityPrice() {
        uploadFileWithComment(
            1,
            "/xlsx-template/priority_price.xlsx",
            "comment 1"
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices_priorityPrice.after.csv"],
    )
    fun testExportPrices_PriorityPrice() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/journals/1/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = PriceExcelMetaData.getColumnsForExportPrices(PriceType.PRIORITY_PRICE)
        val exportedPrices = excelHelper.read(excelData.inputStream(), headers)
        val prices = listOf(
            PriceExcelDTO(
                ssku = "111",
                title = "title1",
                data = PriceDataDTO(priorityPrice = PriorityPriceDTO(priorityPrice = BigDecimal(123)))
            ),
            PriceExcelDTO(
                ssku = "112",
                title = "title1",
                data = PriceDataDTO(priorityPrice = PriorityPriceDTO(priorityPrice = BigDecimal(124)))
            ),
            PriceExcelDTO(
                ssku = "113",
                title = "title1",
                data = PriceDataDTO(priorityPrice = PriorityPriceDTO(priorityPrice = BigDecimal(125)))
            )
        )
        Assertions.assertThat(exportedPrices).isEqualTo(prices)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices.dco_bounds.before.csv"],
        after = ["JournalApiTest.importPrices.dco_bounds.after.upper.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_dcoUpperBound() {
        uploadFileWithComment(
            1,
            "/xlsx-template/dco-upper-bound.xlsx",
            "comment 1"
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.importPrices.dco_bounds.before.csv"],
        after = ["JournalApiTest.importPrices.dco_bounds.after.lower.csv"]
    )
    @WithMockUser(
        username = PassportAuthenticationFilter.LOCAL_DEV,
        roles = ["PRICING_MGMT_ACCESS", "EDIT_PRICES_JOURNALS_KOMANDA1"]
    )
    fun testImportPrices_dcoLowerBound() {
        uploadFileWithComment(
            2,
            "/xlsx-template/dco-lower-bound.xlsx",
            "comment 1"
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(before = ["JournalApiTest.cancel_promo.before.csv"])
    fun testCancelJournals_promoNotAllowedToCancel() {
        val dto = CancelPricesDto()
        dto.addIdsItem(1)   // 1 == PROMO

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/cancel-prices")
                .contentType("application/json")
                .content(dtoToString(dto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Промо-журналы отменяются через веб-интерфейс Промохака"))
            .andExpect(jsonPath("$.errorFields.length()").value(1))
            .andExpect(jsonPath("$.errorFields[0]").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.cancel_promo.before.csv"],
        after = ["JournalApiTest.cancel_promo.after.csv"],
    )
    fun testCancelJournals_othersAllowedToCancel() {
        Mockito.`when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2021, 12, 23, 8, 0, 0)
        )

        val dto = CancelPricesDto()
        dto.addIdsItem(2)   // 2 == RRP

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/cancel-prices")
                .contentType("application/json")
                .content(dtoToString(dto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["JournalApiTest.createApproval.before.csv"],
        after = ["JournalApiTest.createApproval.after.csv"],
    )
    fun testCreateApproval() {
        val dto = ApprovalParameters()
        dto.comment = "test comment for CatDir"

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/1/approval")
                .contentType("application/json")
                .content(dtoToString(dto))
        ).andExpect(MockMvcResultMatchers.status().isNoContent)
    }
}
