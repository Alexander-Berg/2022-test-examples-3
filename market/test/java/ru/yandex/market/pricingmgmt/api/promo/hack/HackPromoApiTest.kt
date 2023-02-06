package ru.yandex.market.pricingmgmt.api.promo.hack

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.client.promo.api.PromoApiClient
import ru.yandex.market.pricingmgmt.model.promo.ExcelImportingError
import ru.yandex.market.pricingmgmt.model.promo.hack.HackListStringItem
import ru.yandex.market.pricingmgmt.repository.postgres.HackPromoAuditRepository
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.service.excel.core.ExcelHelper
import ru.yandex.market.pricingmgmt.service.excel.meta.ExcelImportingErrorMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.HackListMetaData
import ru.yandex.market.pricingmgmt.service.promo.hack.HackPromoService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2Sort
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2SrcCiface
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchResult
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchResultItem
import ru.yandex.mj.generated.client.promoservice.model.SourceType
import ru.yandex.mj.generated.server.model.CatmanDto
import ru.yandex.mj.generated.server.model.HackPromoDto
import ru.yandex.mj.generated.server.model.HackPromoType
import ru.yandex.mj.generated.server.model.IdDto
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class HackPromoApiTest(
    @Autowired val hackListImportingExcelHelper: ExcelHelper<ExcelImportingError>,
    @Autowired private val timeService: TimeService
) : ControllerTest() {

    @Autowired
    private val excelHelper: ExcelHelper<HackListStringItem>? = null

    @Autowired
    private val auditRepository: HackPromoAuditRepository? = null

    @MockBean
    private lateinit var promoApiClient: PromoApiClient

    @BeforeEach
    fun setup() {
        Mockito.reset(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    @DbUnitDataSet(
        after = ["HackPromoApiTest.createDirectDiscountPromo.after.csv"]
    )
    fun createDirectDiscountPromo() {
        val hackPromoDto = buildDirectDiscountPromoDto()

        val expectedResultDto = IdDto().id(1)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResultDto))
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    @DbUnitDataSet(
        after = ["HackPromoApiTest.createDirectDiscountPromoWithCifacePromoId.after.csv"]
    )
    fun createDirectDiscountPromoWithCifacePromoId() {
        val hackPromoDto = buildDirectDiscountPromoDto()
            .promoId("cf_123456")

        `when`(promoApiClient.searchPromo(notNull())).thenReturn(
            PromoSearchResult()
                .totalCount(1)
                .promos(
                    listOf(
                        PromoSearchResultItem()
                            .promoId("cf_123456")
                    )
                )
        )

        val expectedResultDto = IdDto().id(1)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResultDto))
            )

        verify(promoApiClient).searchPromo(buildPromoSearchRequestDto("cf_123456"))
        verifyNoMoreInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    @DbUnitDataSet(
        after = ["HackPromoApiTest.createDirectDiscountPromoWithAnaplanPromoId.after.csv"]
    )
    fun createDirectDiscountPromoWithAnaplanPromoId() {
        val hackPromoDto = buildDirectDiscountPromoDto()
            .promoId("#123456")

        val expectedResultDto = IdDto().id(1)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResultDto))
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    @DbUnitDataSet(
        before = ["HackPromoApiTest.createDirectDiscountPromoWithExistedName.after.csv"],
        after = ["HackPromoApiTest.createDirectDiscountPromoWithExistedName.before.csv"]
    )
    fun createDirectDiscountPromoWithExistedName() {
        `when`(timeService.getNowDateTimeUtc()).thenReturn(
            LocalDateTime.of(2022, 7, 25, 8, 0, 0)
        )

        val hackPromoDto = buildDirectDiscountPromoDto()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().string("Акция с названием 23 февраля уже существует"))

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createDirectDiscountPromoWithEmptyFields() {
        val hackPromoDto = HackPromoDto()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "[" +
                            "Незаполнено название акции;" +
                            "Незаполнена дата начала акции;" +
                            "Незаполнена дата окончания акции;" +
                            "Незаполнено название родительской акции;" +
                            "Незаполнена дата начала родительской акции;" +
                            "Незаполнена дата окончания родительской акции;" +
                            "Незаполнен дедлайн обновления ассортимента акции;" +
                            "Незаполнен тип акции" +
                            "]"
                    )
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createDirectDiscountPromoWithNotFoundCifacePromoId() {
        val hackPromoDto = buildDirectDiscountPromoDto()
            .promoId("cf_123456")

        `when`(promoApiClient.searchPromo(notNull())).thenReturn(
            PromoSearchResult()
                .totalCount(0)
                .promos(emptyList())
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "PromoId cf_123456 not found"
                    )
            )

        verify(promoApiClient).searchPromo(buildPromoSearchRequestDto("cf_123456"))
        verifyNoMoreInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createDirectDiscountPromoWithUnknownPromoId() {
        `when`(timeService.getNowDateTimeUtc()).thenReturn(
            LocalDateTime.of(2022, 7, 25, 8, 0, 0)
        )

        val hackPromoDto = buildDirectDiscountPromoDto()
            .promoId("unknown")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "Unknown source of promoId: unknown"
                    )
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createDirectDiscountPromoWithWrongDates() {
        `when`(timeService.getNowDateTimeUtc()).thenReturn(
            LocalDateTime.of(2022, 7, 25, 8, 0, 0)
        )

        val hackPromoDto =
            buildDirectDiscountPromoDtoWithWrongDates(DateTimeTestingUtil.createOffsetDateTime(2022, 7, 25, 8, 0, 0))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "[" +
                            "Дата старта акции не может быть в прошлом;" +
                            "Дата окончания акции не может быть в прошлом;" +
                            "Дата старта акции не может быть после даты окончания;" +
                            "Дата старта родительской акции не может быть после даты ее окончания;" +
                            "Дата дедлайна должна быть больше текущей даты;" +
                            "Дата дедлайна должна быть меньше даты старта акции;" +
                            "Даты старта и окончания акции должны быть внутри дат родительской акции" +
                            "]"
                    )
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    @DbUnitDataSet(
        after = ["HackPromoApiTest.createFlashPromo.after.csv"]
    )
    fun createFlashPromo() {
        val hackPromoDto = buildFlashPromoDto()

        val expectedResultDto = IdDto().id(1)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResultDto))
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    @DbUnitDataSet(
        after = ["HackPromoApiTest.createFlashPromoWithCifacePromoId.after.csv"]
    )
    fun createFlashPromoWithCifacePromoId() {
        val hackPromoDto = buildFlashPromoDto()
            .promoId("cf_123456")

        `when`(promoApiClient.searchPromo(notNull())).thenReturn(
            PromoSearchResult()
                .totalCount(1)
                .promos(
                    listOf(
                        PromoSearchResultItem()
                            .promoId("cf_123456")
                    )
                )
        )

        val expectedResultDto = IdDto().id(1)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResultDto))
            )

        verify(promoApiClient).searchPromo(buildPromoSearchRequestDto("cf_123456"))
        verifyNoMoreInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    @DbUnitDataSet(
        after = ["HackPromoApiTest.createFlashPromoWithAnaplanPromoId.after.csv"]
    )
    fun createFlashPromoWithAnaplanPromoId() {
        val hackPromoDto = buildFlashPromoDto()
            .promoId("#123456")

        val expectedResultDto = IdDto().id(1)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResultDto))
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createFlashPromoWithEmptyFields() {
        val hackPromoDto = HackPromoDto()
        hackPromoDto.promoType(HackPromoType.FLASH)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "[" +
                            "Незаполнено название акции;" +
                            "Незаполнена дата начала акции;" +
                            "Незаполнена дата окончания акции;" +
                            "Незаполнено название родительской акции;" +
                            "Незаполнена дата начала родительской акции;" +
                            "Незаполнена дата окончания родительской акции;" +
                            "Незаполнен дедлайн обновления ассортимента акции;" +
                            "Незаполнено название флеша;" +
                            "Незаполнена дата начала флеша;" +
                            "Незаполнена дата окончания флеша" +
                            "]"
                    )
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createFlashPromoWithWrongDates() {
        `when`(timeService.getNowDateTimeUtc()).thenReturn(
            LocalDateTime.of(2022, 7, 25, 8, 0, 0)
        )

        val hackPromoDto =
            buildFlashPromoDtoWithWrongDates(DateTimeTestingUtil.createOffsetDateTime(2022, 7, 25, 8, 0, 0))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "[" +
                            "Дата старта акции не может быть в прошлом;" +
                            "Дата окончания акции не может быть в прошлом;" +
                            "Дата старта акции не может быть после даты окончания;" +
                            "Дата старта родительской акции не может быть после даты ее окончания;" +
                            "Дата дедлайна должна быть больше текущей даты;" +
                            "Дата дедлайна должна быть меньше даты старта акции;" +
                            "Даты старта и окончания акции должны быть внутри дат родительской акции;" +
                            "Даты старта и окончания акции должны быть внутри дат флеш акции" +
                            "]"
                    )
            )
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createFlashPromoWithNotFoundCifacePromoId() {
        val hackPromoDto = buildFlashPromoDto()
            .promoId("cf_123456")

        `when`(promoApiClient.searchPromo(notNull())).thenReturn(
            PromoSearchResult()
                .totalCount(0)
                .promos(emptyList())
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "PromoId cf_123456 not found"
                    )
            )

        verify(promoApiClient).searchPromo(buildPromoSearchRequestDto("cf_123456"))
        verifyNoMoreInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createFlashPromoWithUnknownPromoId() {
        val hackPromoDto = buildFlashPromoDto()
            .promoId("unknown")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "Unknown source of promoId: unknown"
                    )
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    @DbUnitDataSet(
        after = ["HackPromoApiTest.createDirectDiscountPromoWithFreezeDates.after.csv"]
    )
    fun createDirectDiscountPromoWithFreezeDates() {
        `when`(timeService.getNowDateTimeUtc()).thenReturn(
            LocalDateTime.of(2022, 7, 25, 8, 0, 0)
        )

        val hackPromoDto = buildDirectDiscountPromoDto(DateTimeTestingUtil.createOffsetDateTime(2022, 7, 27, 11, 12, 0))
            .freezeStartDate(DateTimeTestingUtil.createOffsetDateTime(2022, 7, 26, 11, 12, 0))
            .freezeEndDate(DateTimeTestingUtil.createOffsetDateTime(2022, 7, 28, 11, 12, 0))

        val expectedResultDto = IdDto().id(1)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResultDto))
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun createDirectDiscountPromoWithFreezeDates_wrongDates() {
        `when`(timeService.getNowDateTimeUtc()).thenReturn(
            LocalDateTime.of(2022, 7, 25, 8, 0, 0)
        )

        val dt = DateTimeTestingUtil.createOffsetDateTime(2022, 7, 27, 11, 12, 0)

        val hackPromoDto = buildDirectDiscountPromoDto(dt)
            .freezeStartDate(dt.plusHours(1))
            .freezeEndDate(dt)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string(
                        "[" +
                            "Дата начального фриза должна быть меньше даты старта акции;" +
                            "Дата конечного фриза должна быть больше даты окончания акции" +
                            "]"
                    )
            )

        verifyZeroInteractions(promoApiClient)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.deptPromosCreated.before.csv"],
        after = ["HackPromoApiTest.deptPromosCreated.after.csv"]
    )
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deptPromosCreated() {
        val hackPromoDto = buildDirectDiscountPromoDto()

        val expectedResultDto = IdDto().id(1)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResultDto))
            )
    }

    @Test
    @DbUnitDataSet(before = ["HackPromoApiTest.roles.csv"])
    fun createPromoWithoutPermissions() {
        val hackPromoDto = buildDirectDiscountPromoDto()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(hackPromoDto))
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.deletePromoSuccessTest.before.csv"],
        after = ["HackPromoApiTest.deletePromoSuccessTest.after.csv"]
    )
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoSuccessTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo")
        ).andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.deletePromoWhenDepExistsTest.csv"],
        after = ["HackPromoApiTest.deletePromoWhenDepExistsTest.csv"],
    )
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoWhenDepExistsTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.deletePromoWhenListExistsTest.csv"],
        after = ["HackPromoApiTest.deletePromoWhenListExistsTest.csv"],
    )
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoWhenListExistsTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoNotFoundTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.deletePromoDepSuccessTest.before.csv"],
        after = ["HackPromoApiTest.deletePromoDepSuccessTest.after.csv"]
    )
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoDepSuccessTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo_dep")
        ).andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.deletePromoWhenListExistsTest.csv"],
        after = ["HackPromoApiTest.deletePromoWhenListExistsTest.csv"],
    )
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoDepWhenListExistsTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo_dep")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.deletePromoDepNotFoundTest.csv"],
        after = ["HackPromoApiTest.deletePromoDepNotFoundTest.csv"]

    )
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoDepNotFoundTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "2")
                .param("entity", "promo_dep")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DbUnitDataSet(before = ["HackPromoApiTest.roles.csv"])
    fun deletePromoWithoutPermissionTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo")
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoWithoutIdParamBadRequestTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("entity", "promo")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoWithoutEntityParamBadRequestTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @WithMockUser(roles = ["COLLECT_PROMO_ASSORTMENT_CREATE_PROMO"])
    fun deletePromoWithUnknownEntityTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/delete")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "unknown")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.getPromos.before.csv"]
    )
    fun getHackPromos() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos")
                .contentType("application/json")
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful).andExpect(
                MockMvcResultMatchers.content()
                    .json(expectedGetResponse(), true)
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.getHackPromosWithHistory.before.csv"]
    )
    fun getHackPromosWithHistory() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos")
                .contentType("application/json")
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful).andExpect(
                MockMvcResultMatchers.content()
                    .json(expectedGetResponseWithHistory(), true)
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListItems.before.csv"],
        after = ["HackPromoApiTest.uploadListItems.after.csv"]
    )
    fun uploadListItems() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        val actualHistory = auditRepository?.getListsHistory(listOf(1L))
        //Все поля кроме operationTime проверяются через DbUnit
        Assertions.assertThat(actualHistory?.get(0)?.operationTime).isNotNull
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListItems.journalsCreated.before.csv"],
        after = ["HackPromoApiTest.uploadListItems.journalsCreated.after.csv"]
    )
    fun uploadListItems_journalsCreated() {
        // в таблицах journal, price, axapta_export_log будут созданы записи

        `when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0)
        )

        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        val actualHistory = auditRepository?.getListsHistory(listOf(1L))
        //Все поля кроме operationTime проверяются через DbUnit
        Assertions.assertThat(actualHistory?.get(0)?.operationTime).isNotNull
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListItemsWithEmptyOptionalFields.before.csv"],
        after = ["HackPromoApiTest.uploadListItemsWithEmptyOptionalFields.after.csv"]
    )
    fun uploadListItemsWithEmptyOptionalFields() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-empty-optional-fields.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        val actualHistory = auditRepository?.getListsHistory(listOf(1L))
        //Все поля кроме operationTime проверяются через DbUnit
        Assertions.assertThat(actualHistory?.get(0)?.operationTime).isNotNull
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListItemsAfterDeadline.before.csv"]
    )
    fun uploadListItemAfterDeadline() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListItems.before.csv"],
        after = ["HackPromoApiTest.uploadEmptyListItems.after.csv"]
    )
    fun uploadEmptyListItems() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/empty-hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListItemsWrongCatteam.before.csv"],
        after = ["HackPromoApiTest.uploadListItemsWrongCatteam.after.csv"]
    )
    fun uploadListWithWrongCatteams() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadWrongListItems.before.csv"],
        after = ["HackPromoApiTest.uploadWrongListItemsWithWrongDataTypes.after.csv"]
    )
    fun uploadListItemsWithWrongDataTypes() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-wrong-types.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadWrongListItems.before.csv"],
        after = ["HackPromoApiTest.uploadWrongListItems.after.csv"]
    )
    fun uploadWrongListItems() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-wrong-ssku.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListWithWrongMonetaryAndPercentFields.before.csv"],
        after = ["HackPromoApiTest.uploadListWithWrongMonetaryAndPercentFields.after.csv"]
    )
    fun uploadListWithWrongMonetaryAndPercentFields() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-wrong-monetary-and-percent-fields.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListWithWrongSalesPlanFields.before.csv"],
        after = ["HackPromoApiTest.uploadListWithWrongSalesPlanFields.after.csv"]
    )
    fun uploadListWithWrongSalesPlanFields() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-wrong-sales-plan-fields.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    fun uploadListItemsToNotExistentDept() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun uploadListWithoutListName() {
        uploadList(
            1L,
            null,
            null,
            "promo_dep",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Название листа не указано.")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListAsCatdirForAnotherCatmen.before.csv"],
        after = ["HackPromoApiTest.uploadListAsCatdirForAnotherCatmen.after.csv"]
    )
    fun uploadListWithoutListNameAsCatdirForAnotherCatmen() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-catdir.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        val actualHistory = auditRepository?.getListsHistory(listOf(1L))
        //Все поля кроме operationTime проверяются через DbUnit
        Assertions.assertThat(actualHistory?.get(0)?.operationTime).isNotNull
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListAsCatdirForAnotherCatmenWrongData.before.csv"],
        after = ["HackPromoApiTest.uploadListAsCatdirForAnotherCatmenWrongData.after.csv"]
    )
    fun uploadListWithoutListNameAsCatdirForAnotherCatmenWrongData() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-catdir-wrong-data.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListAsCatdirForAnotherCatmenNotExist.before.csv"],
        after = ["HackPromoApiTest.uploadListAsCatdirForAnotherCatmenNotExist.after.csv"]
    )
    fun uploadListWithoutListNameAsCatdirForAnotherCatmenNotExist() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-catdir.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListAsCatdirForAnotherCatmenAnotherDept.before.csv"],
        after = ["HackPromoApiTest.uploadListAsCatdirForAnotherCatmenAnotherDept.after.csv"]
    )
    fun uploadListWithoutListNameAsCatdirForAnotherCatmenAnotherDept() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list-catdir.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun reUploadNotExistentList() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListItems.before.csv"],
        after = ["HackPromoApiTest.reUploadListItems.after.csv"]
    )
    fun reUploadListItems() {
        `when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2021, 12, 23, 8, 0, 0)
        )
        `when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0)
        )

        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-reUpload.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListItems.journalsCreated.before.csv"],
        after = ["HackPromoApiTest.reUploadListItems.journalsCreated.after.csv"]
    )
    fun reUploadListItems_journalsCreated() {
        `when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2021, 12, 23, 8, 0, 0)
        )
        `when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0)
        )

        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-reUpload.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListItemsDiff.before.csv"],
    )
    fun getReuploadListItemsDiff() {
        `when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2021, 12, 23, 8, 0, 0)
        )
        `when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0)
        )

        uploadListDiff(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.deletedSskuCount").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.addedSskuCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.changedSskuCount").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListItems.before.flash.csv"],
        after = ["HackPromoApiTest.reUploadListItems.after.flash.csv"]
    )
    fun reUploadListItemsFlash() {
        `when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2021, 12, 23, 8, 0, 0)
        )
        `when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0)
        )

        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-reUpload.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListItemsWithSskuUploadedAnotherCatman.before.csv"],
        after = ["HackPromoApiTest.reUploadListItemsWithSskuUploadedAnotherCatman.after.csv"]
    )
    fun reUploadListItemsWithSskuUploadedAnotherCatman() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListItemsAfterDeadline.before.csv"],
    )
    fun reUploadListItemsAfterDeadline() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListItems.before.csv"],
        after = ["HackPromoApiTest.reUploadEmptyListItems.after.csv"]
    )
    fun reUploadEmptyListItems() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/empty-hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListWithWrongCatteam.before.csv"],
        after = ["HackPromoApiTest.reUploadListWithWrongCatteam.after.csv"]
    )
    fun reUploadListWithWrongCatteams() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadWrongListItems.before.csv"],
        after = ["HackPromoApiTest.reUploadWrongListItems.after.csv"]
    )
    fun reUploadWrongListItems() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-wrong-ssku.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе ассортимента содержатся ошибки\",\"excelLink\":\"/api/v1/hack/promos/items/errors/excel\"}")
            )
    }

    @Test
    fun reUploadListItemsWithoutComment() {
        uploadList(
            1L,
            null,
            null,
            "promo_list",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Комментарий не указан.")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListAsCatdirForAnotherCatmen.before.csv"],
        after = ["HackPromoApiTest.reUploadListAsCatdirForAnotherCatmen.after.csv"]
    )
    fun reUploadListWithoutListNameAsCatdirForAnotherCatmen() {
        `when`(timeService.getNowOffsetDateTime())
            .thenReturn(DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0))

        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-catdir.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        val actualHistory = auditRepository?.getListsHistory(listOf(1L))
        //Все поля кроме operationTime проверяются через DbUnit
        Assertions.assertThat(actualHistory?.get(0)?.operationTime).isNotNull
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListAsCatdirForAnotherCatmenWrongData.before.csv"],
        after = ["HackPromoApiTest.reUploadListAsCatdirForAnotherCatmenWrongData.after.csv"]
    )
    fun reUploadListWithoutListNameAsCatdirForAnotherCatmenWrongData() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-catdir-wrong-data.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListAsCatdirForAnotherCatmenNotExist.before.csv"],
        after = ["HackPromoApiTest.reUploadListAsCatdirForAnotherCatmenNotExist.after.csv"]
    )
    fun reUploadListWithoutListNameAsCatdirForAnotherCatmenNotExist() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-catdir.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.reUploadListAsCatdirForAnotherCatmenAnotherUser.before.csv"],
        after = ["HackPromoApiTest.reUploadListAsCatdirForAnotherCatmenAnotherUser.after.csv"]
    )
    fun reUploadListWithoutListNameAsCatdirForAnotherCatmenAnotherUser() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-catdir.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.uploadListWithExistentName.before.csv"]
    )
    fun uploadListWithExistentName() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Лист с названием Детские вещи уже создан в выбранном департаменте.")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.exportListItems.before.csv"]
    )
    fun exportListItems() {
        val actualSskus: List<HackListStringItem> = performExportItemsRequestAndReadFile(1L, "promo_list")
        Assertions.assertThat(actualSskus.isNotEmpty()).isTrue
        assertCorrectExportResults(actualSskus, buildExportList(listOf(0, 1)))
    }

    @Test
    fun exportNotExistentList() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos/items/")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo_list")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.exportListItems.before.csv"]
    )
    fun exportDeptPromoItems() {
        val deptPromoId = 1L
        val actualSskus: List<HackListStringItem> = performExportItemsRequestAndReadFile(deptPromoId, "promo_dep")
        Assertions.assertThat(actualSskus.isNotEmpty()).isTrue
        assertCorrectExportResults(actualSskus, buildExportList())
    }

    @Test
    fun exportNotExistentDeptPromo() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos/items/")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo_dep")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.exportListItems.before.csv"]
    )
    fun exportPromoItems() {
        val promoId = 5L
        val actualSskus: List<HackListStringItem> = performExportItemsRequestAndReadFile(promoId, "promo")
        Assertions.assertThat(actualSskus.isNotEmpty()).isTrue
        assertCorrectExportResults(actualSskus, buildExportList(listOf(1, 0)))
    }

    @Test
    @WithMockUser(roles = ["PROMOHACK_APPROVE_PROMOS"])
    @DbUnitDataSet(
        before = ["HackPromoApiTest.approve.before.csv"],
        after = ["HackPromoApiTest.approve.after.csv"]
    )
    fun approveSuccess() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/{id}/approve", 1L)
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DbUnitDataSet(before = ["HackPromoApiTest.roles.csv"])
    fun approveForbidden() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/{id}/approve", 1L)
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DbUnitDataSet(before = ["HackPromoApiTest.roles.csv"])
    fun releaseForbidden() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/{id}/create-price-journals", 1L)
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["PROMOHACK_RELEASE_PRICES"])
    @DbUnitDataSet(
        before = ["HackPromoApiTest.releaseSuccess.before.csv"],
        after = ["HackPromoApiTest.releaseSuccess.after.csv"]
    )
    fun releaseSuccess() {
        `when`(timeService.getNowOffsetDateTime())
            .thenReturn(DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0))
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/{id}/create-price-journals", 1L)
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @WithMockUser(roles = ["PROMOHACK_RELEASE_PRICES"])
    @DbUnitDataSet(
        before = ["HackPromoApiTest.releaseFreeze.before.csv"],
        after = ["HackPromoApiTest.releaseFreeze.after.csv"]
    )
    fun releaseFreeze() {
        // создадутся журналы фриза до и после акции

        `when`(timeService.getNowOffsetDateTime())
            .thenReturn(DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/{id}/create-price-journals", 1L)
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @WithMockUser(roles = ["PROMOHACK_RELEASE_PRICES"])
    @DbUnitDataSet(
        before = ["HackPromoApiTest.releaseFreezeBeforeOnly.before.csv"],
        after = ["HackPromoApiTest.releaseFreezeBeforeOnly.after.csv"]
    )
    fun releaseFreezeBeforeOnly() {
        // создастся журнал фриза только до акции

        `when`(timeService.getNowOffsetDateTime())
            .thenReturn(DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/{id}/create-price-journals", 1L)
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @WithMockUser(roles = ["PROMOHACK_RELEASE_PRICES"])
    @DbUnitDataSet(
        before = ["HackPromoApiTest.releaseFreezeAfterOnly.before.csv"],
        after = ["HackPromoApiTest.releaseFreezeAfterOnly.after.csv"]
    )
    fun releaseFreezeAfterOnly() {
        // создастся журнал фриза только после акции

        `when`(timeService.getNowOffsetDateTime())
            .thenReturn(DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/hack/promos/{id}/create-price-journals", 1L)
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    fun exportNotExistentPromo() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos/items/")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "promo")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun exportItemsWithWrongEntity() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos/items/")
                .contentType("application/json")
                .param("id", "1")
                .param("entity", "unknown_entity")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.getCatmans.before.csv"]
    )
    fun getCatmans() {
        val expectedResult = listOf(
            CatmanDto().id(1).login("ivan"),
            CatmanDto().id(2).login("sergey"),
            CatmanDto().id(3).login("pasha"),
            CatmanDto().id(5).login("igor"),
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/catmans/")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful).andExpect(
            MockMvcResultMatchers.content()
                .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResult))
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.deleteListItem.before.csv"],
        after = ["HackPromoApiTest.deleteListItem.after.csv"]
    )
    fun deleteListItem() {
        `when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2022, 12, 23, 8, 0, 0)
        )

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/items/?id=1")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    fun deleteListItemNotFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/hack/promos/items/?id=1")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiTest.exportErrors.before.csv"],
    )
    fun exportErrors() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos/items/errors/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = ExcelImportingErrorMetaData.getColumnMetaData()
        val actual = hackListImportingExcelHelper.read(excelData.inputStream(), headers)
        val expected = listOf(
            ExcelImportingError(rowNumber = 8, message = "James Hetfield"),
            ExcelImportingError(rowNumber = 8, message = "Lars Ulrich"),
            ExcelImportingError(rowNumber = 9, message = "Kirk Hammett"),
            ExcelImportingError(message = "Robert Trujillo"),
        )
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    private fun buildPromoSearchRequestDto(promoId: String): PromoSearchRequestDtoV2 {
        return PromoSearchRequestDtoV2()
            .sort(
                listOf(
                    PromoSearchRequestDtoV2Sort()
                        .field(PromoSearchRequestDtoV2Sort.FieldEnum.PROMOID)
                        .direction(PromoSearchRequestDtoV2Sort.DirectionEnum.DESC)
                )
            )
            .promoId(listOf(promoId))
            .sourceType(
                listOf(
                    SourceType.CATEGORYIFACE
                )
            )
            .srcCiface(
                PromoSearchRequestDtoV2SrcCiface()
            )
    }

    private fun buildDirectDiscountPromoDto(onDate: OffsetDateTime? = null): HackPromoDto {
        val now = onDate ?: OffsetDateTime.now(ZoneOffset.UTC)

        val hackPromoDto = HackPromoDto()
        hackPromoDto.promoType = HackPromoType.DIRECT_DISCOUNT
        hackPromoDto.name = "23 февраля"
        hackPromoDto.startDate = now.plusHours(1)
        hackPromoDto.endDate = now.plusHours(2)
        hackPromoDto.parentPromoName = "Гендерные праздники"
        hackPromoDto.parentPromoStartDate = now.minusHours(1)
        hackPromoDto.parentPromoEndDate = now.plusHours(3)
        hackPromoDto.assortmentDeadline = hackPromoDto.startDate
        return hackPromoDto
    }

    private fun buildDirectDiscountPromoDtoWithWrongDates(onDate: OffsetDateTime? = null): HackPromoDto {
        val now = onDate ?: OffsetDateTime.now(ZoneOffset.UTC)

        val hackPromoDto = HackPromoDto()
        hackPromoDto.promoType = HackPromoType.DIRECT_DISCOUNT
        hackPromoDto.name = "23 февраля"
        hackPromoDto.startDate = now.minusHours(3)
        hackPromoDto.endDate = now.minusHours(4)
        hackPromoDto.parentPromoName = "Гендерные праздники"
        hackPromoDto.parentPromoStartDate = now.minusHours(1)
        hackPromoDto.parentPromoEndDate = now.minusHours(2)
        hackPromoDto.assortmentDeadline = now.minusHours(1)
        return hackPromoDto
    }

    private fun buildFlashPromoDto(): HackPromoDto {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val hackPromoDto = HackPromoDto()
        hackPromoDto.promoType = HackPromoType.FLASH
        hackPromoDto.name = "23 февраля"
        hackPromoDto.startDate = now.plusHours(1)
        hackPromoDto.endDate = now.plusHours(2)
        hackPromoDto.parentPromoName = "Гендерные праздники"
        hackPromoDto.parentPromoStartDate = now.minusHours(1)
        hackPromoDto.parentPromoEndDate = now.plusHours(3)
        hackPromoDto.assortmentDeadline = hackPromoDto.startDate
        hackPromoDto.flashWaveName = "Название волны"
        hackPromoDto.flashWaveStartDate = hackPromoDto.startDate
        hackPromoDto.flashWaveEndDate = hackPromoDto.endDate
        return hackPromoDto
    }

    private fun buildFlashPromoDtoWithWrongDates(onDate: OffsetDateTime? = null): HackPromoDto {
        val now = onDate ?: OffsetDateTime.now(ZoneOffset.UTC)

        val hackPromoDto = HackPromoDto()
        hackPromoDto.promoType = HackPromoType.FLASH
        hackPromoDto.name = "23 февраля"
        hackPromoDto.startDate = now.minusHours(3)
        hackPromoDto.endDate = now.minusHours(4)
        hackPromoDto.parentPromoName = "Гендерные праздники"
        hackPromoDto.parentPromoStartDate = now.minusHours(1)
        hackPromoDto.parentPromoEndDate = now.minusHours(2)
        hackPromoDto.assortmentDeadline = now.minusHours(1)
        hackPromoDto.flashWaveName = "Название волны"
        hackPromoDto.flashWaveStartDate = now.plusHours(2)
        hackPromoDto.flashWaveEndDate = now.plusHours(1)
        return hackPromoDto
    }

    private fun expectedGetResponse(): String {
        return """
            [{
                "id": "2",
                "status": "COLLECTION",
                "promo_id": "2",
                "name": "Акция [23 февраля]",
                "promoType": "flash",
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "parentPromoName": "Гендерные праздники",
                "parentPromoStartDate": "2022-01-01T09:15:30Z",
                "parentPromoEndDate": "2022-03-25T09:15:30Z",
                "flashWaveName": "Название волны",
                "flashWaveStartDate": "2022-01-05T09:15:30Z",
                "flashWaveEndDate": "2022-03-23T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "history": [],
                "entity": "promo",
                "grants": [],
                "sourcePromoId": "-"
            },{
                "id": "5",
                "promo_dep_id": "5",
                "promo_id": "2",
                "dept_id": "1",
                "name": "Акция [23 февраля] (ЭиБТ)",
                "promoType": "flash",
                "history": [],
                "entity": "promo_dep",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "6",
                "promo_dep_id": "6",
                "promo_id": "2",
                "dept_id": "2",
                "name": "Акция [23 февраля] (DIY & Auto)",
                "promoType": "flash",
                "history": [],
                "entity": "promo_dep",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "7",
                "promo_dep_id": "7",
                "promo_id": "2",
                "dept_id": "3",
                "name": "Акция [23 февраля] (FMCG)",
                "promoType": "flash",
                "history": [],
                "entity": "promo_dep",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "8",
                "promo_dep_id": "8",
                "promo_id": "2",
                "dept_id": "4",
                "name": "Акция [23 февраля] (Фарма)",
                "promoType": "flash",
                "history": [],
                "entity": "promo_dep",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "1",
                "status": "COLLECTION",
                "promo_id": "1",
                "name": "Акция [23 февраля]",
                "promoType": "direct_discount",
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "parentPromoName": "Гендерные праздники",
                "parentPromoStartDate": "2022-01-01T09:15:30Z",
                "parentPromoEndDate": "2022-03-25T09:15:30Z",
                "flashWaveName": null,
                "flashWaveStartDate": null,
                "flashWaveEndDate": null,
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "history": [],
                "entity": "promo",
                "grants": [],
                "sourcePromoId": "-"
            },{
                "id": "1",
                "promo_dep_id": "1",
                "promo_id": "1",
                "dept_id": "1",
                "name": "Акция [23 февраля] (ЭиБТ)",
                "promoType": "direct_discount",
                "history": [],
                "entity": "promo_dep",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "1",
                "promo_list_id": "1",
                "promo_id": "1",
                "promo_dept_id": "1",
                "dept_id": "1",
                "name": "Лист [23 февраля] (ЭиБТ) Название1",
                "promoType": "direct_discount",
                "catman": "James Hetfield",
                "history": [],
                "entity": "promo_list",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "2",
                "promo_list_id": "2",
                "promo_id": "1",
                "promo_dept_id": "1",
                "dept_id": "1",
                "name": "Лист [23 февраля] (ЭиБТ) Название2",
                "promoType": "direct_discount",
                "catman": "James Hetfield",
                "history": [],
                "entity": "promo_list",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "3",
                "promo_list_id": "3",
                "promo_id": "1",
                "promo_dept_id": "1",
                "dept_id": "1",
                "name": "Лист [23 февраля] (ЭиБТ) Название3",
                "promoType": "direct_discount",
                "catman": "James Hetfield",
                "history": [],
                "entity": "promo_list",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "4",
                "promo_list_id": "4",
                "promo_id": "1",
                "promo_dept_id": "1",
                "dept_id": "1",
                "name": "Лист [23 февраля] (ЭиБТ) Название4",
                "promoType": "direct_discount",
                "catman": "James Hetfield",
                "history": [],
                "entity": "promo_list",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "2",
                "promo_dep_id": "2",
                "promo_id": "1",
                "dept_id": "2",
                "name": "Акция [23 февраля] (DIY & Auto)",
                "promoType": "direct_discount",
                "history": [],
                "entity": "promo_dep",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "5",
                "promo_list_id": "5",
                "promo_id": "1",
                "promo_dept_id": "2",
                "dept_id": "2",
                "name": "Лист [23 февраля] (DIY & Auto) Название5",
                "promoType": "direct_discount",
                "catman": "James Hetfield",
                "history": [],
                "entity": "promo_list",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "3",
                "promo_dep_id": "3",
                "promo_id": "1",
                "dept_id": "3",
                "name": "Акция [23 февраля] (FMCG)",
                "promoType": "direct_discount",
                "history": [],
                "entity": "promo_dep",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            },{
                "id": "4",
                "promo_dep_id": "4",
                "promo_id": "1",
                "dept_id": "4",
                "name": "Акция [23 февраля] (Фарма)",
                "promoType": "direct_discount",
                "history": [],
                "entity": "promo_dep",
                "grants": [],
                "startDate": "2022-01-10T09:15:30Z",
                "endDate": "2022-03-20T09:15:30Z",
                "assortmentDeadline": "2022-01-25T09:15:30Z",
                "sourcePromoId": "-"
            }
        ]
        """.trimIndent()
    }

    private fun expectedGetResponseWithHistory(): String {
        return """
[
    {
        "id": "1",
        "status": null,
        "promo_id": "1",
        "name": "Акция [23 февраля]",
        "promoType": "direct_discount",
        "startDate": "2022-01-10T09:15:30Z",
        "endDate": "2022-03-20T09:15:30Z",
        "parentPromoName": "Гендерные праздники",
        "parentPromoStartDate": "2022-01-01T09:15:30Z",
        "parentPromoEndDate": "2022-03-25T09:15:30Z",
        "flashWaveName": null,
        "flashWaveStartDate": null,
        "flashWaveEndDate": null,
        "assortmentDeadline": "2022-01-25T09:15:30Z",
        "history": [
            "23.12.2022 08:00 petr@ - Удален \"Лист [23 февраля] (ЭиБТ) Детские вещи\"",
            "19.08.2022 07:16 pavel@ - Заменен \"Лист [23 февраля] (DIY) Лист4\" (Самые последние правки)",
            "19.08.2022 06:16 afanasiy@ - Заменен \"Лист [23 февраля] (DIY) Лист4\" (Последние правки)",
            "10.08.2022 03:16 igor@ - Создан \"Лист [23 февраля] (ЭиБТ) Лист2\"",
            "10.08.2022 02:30 pavel@ - Создан \"Лист [23 февраля] (DIY) Лист4\"",
            "10.01.2022 09:15 sergei@ - Заменен \"Лист [23 февраля] (ЭиБТ) Детские вещи\" (Очередные правки)",
            "10.01.2022 09:15 ivan@ - Создан \"Лист [23 февраля] (ЭиБТ) Детские вещи\"",
            "10.01.2022 02:16 taras@ - Заменен \"Лист [23 февраля] (ЭиБТ) Лист1\" (обновление цен)",
            "10.01.2022 01:15 taras@ - Создан \"Лист [23 февраля] (ЭиБТ) Лист1\"",
            "10.01.2022 01:10 anton@ - Создана \"Акция [23 февраля]\""
        ],
        "entity": "promo",
        "grants": [],
        "sourcePromoId": "-"
    },
    {
        "id": "1",
        "promo_dep_id": "1",
        "promo_id": "1",
        "dept_id": "1",
        "name": "Акция [23 февраля] (ЭиБТ)",
        "promoType": "direct_discount",
        "history": [
            "23.12.2022 08:00 petr@ - Удален \"Лист [23 февраля] (ЭиБТ) Детские вещи\"",
            "10.08.2022 03:16 igor@ - Создан \"Лист [23 февраля] (ЭиБТ) Лист2\"",
            "10.01.2022 09:15 sergei@ - Заменен \"Лист [23 февраля] (ЭиБТ) Детские вещи\" (Очередные правки)",
            "10.01.2022 09:15 ivan@ - Создан \"Лист [23 февраля] (ЭиБТ) Детские вещи\"",
            "10.01.2022 02:16 taras@ - Заменен \"Лист [23 февраля] (ЭиБТ) Лист1\" (обновление цен)",
            "10.01.2022 01:15 taras@ - Создан \"Лист [23 февраля] (ЭиБТ) Лист1\""
        ],
        "entity": "promo_dep",
        "grants": [],
        "startDate": "2022-01-10T09:15:30Z",
        "endDate": "2022-03-20T09:15:30Z",
        "assortmentDeadline": "2022-01-25T09:15:30Z",
        "sourcePromoId": "-"
    },
    {
        "id": "1",
        "promo_list_id": "1",
        "promo_id": "1",
        "promo_dept_id": "1",
        "dept_id": "1",
        "name": "Лист [23 февраля] (ЭиБТ) Лист1",
        "promoType": "direct_discount",
        "catman": "taras",
        "history": [
            "10.01.2022 02:16 taras@ - Заменен \"Лист [23 февраля] (ЭиБТ) Лист1\" (обновление цен)",
            "10.01.2022 01:15 taras@ - Создан \"Лист [23 февраля] (ЭиБТ) Лист1\""
        ],
        "entity": "promo_list",
        "grants": [],
        "startDate": "2022-01-10T09:15:30Z",
        "endDate": "2022-03-20T09:15:30Z",
        "assortmentDeadline": "2022-01-25T09:15:30Z",
        "sourcePromoId": "-"
    },
    {
        "id": "2",
        "promo_list_id": "2",
        "promo_id": "1",
        "promo_dept_id": "1",
        "dept_id": "1",
        "name": "Лист [23 февраля] (ЭиБТ) Лист2",
        "promoType": "direct_discount",
        "catman": "igor",
        "history": [
            "10.08.2022 03:16 igor@ - Создан \"Лист [23 февраля] (ЭиБТ) Лист2\""
        ],
        "entity": "promo_list",
        "grants": [],
        "startDate": "2022-01-10T09:15:30Z",
        "endDate": "2022-03-20T09:15:30Z",
        "assortmentDeadline": "2022-01-25T09:15:30Z",
        "sourcePromoId": "-"
    },
    {
        "id": "3",
        "promo_dep_id": "3",
        "promo_id": "1",
        "dept_id": "2",
        "name": "Акция [23 февраля] (DIY)",
        "promoType": "direct_discount",
        "history": [
            "19.08.2022 07:16 pavel@ - Заменен \"Лист [23 февраля] (DIY) Лист4\" (Самые последние правки)",
            "19.08.2022 06:16 afanasiy@ - Заменен \"Лист [23 февраля] (DIY) Лист4\" (Последние правки)",
            "10.08.2022 02:30 pavel@ - Создан \"Лист [23 февраля] (DIY) Лист4\""
        ],
        "entity": "promo_dep",
        "grants": [],
        "startDate": "2022-01-10T09:15:30Z",
        "endDate": "2022-03-20T09:15:30Z",
        "assortmentDeadline": "2022-01-25T09:15:30Z",
        "sourcePromoId": "-"
    },
    {
        "id": "4",
        "promo_list_id": "4",
        "promo_id": "1",
        "promo_dept_id": "3",
        "dept_id": "2",
        "name": "Лист [23 февраля] (DIY) Лист4",
        "promoType": "direct_discount",
        "catman": "pavel",
        "history": [
            "19.08.2022 07:16 pavel@ - Заменен \"Лист [23 февраля] (DIY) Лист4\" (Самые последние правки)",
            "19.08.2022 06:16 afanasiy@ - Заменен \"Лист [23 февраля] (DIY) Лист4\" (Последние правки)",
            "10.08.2022 02:30 pavel@ - Создан \"Лист [23 февраля] (DIY) Лист4\""
        ],
        "entity": "promo_list",
        "grants": [],
        "startDate": "2022-01-10T09:15:30Z",
        "endDate": "2022-03-20T09:15:30Z",
        "assortmentDeadline": "2022-01-25T09:15:30Z",
        "sourcePromoId": "-"
    }
]
        """.trimIndent()
    }

    private fun uploadList(id: Long, name: String?, comment: String?, entity: String, filename: String): ResultActions {
        val bytes = javaClass.getResourceAsStream(filename)?.readAllBytes()
        val file = MockMultipartFile("excelFile", filename, "application/vnd.ms-excel", bytes)
        return mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/hack/promos/items/")
                .file(file)
                .param("id", id.toString())
                .param("entity", entity)
                .param("name", name)
                .param("comment", comment)
        )
    }

    private fun uploadListDiff(
        id: Long,
        name: String?,
        comment: String?,
        entity: String,
        filename: String
    ): ResultActions {
        val bytes = javaClass.getResourceAsStream(filename)?.readAllBytes()
        val file = MockMultipartFile("excelFile", filename, "application/vnd.ms-excel", bytes)
        return mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/hack/promos/items/diff")
                .file(file)
                .param("id", id.toString())
                .param("entity", entity)
                .param("name", name)
                .param("comment", comment)
        )
    }

    private fun performExportItemsRequestAndReadFile(id: Long, entity: String): List<HackListStringItem> {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos/items/")
                .contentType("application/json")
                .param("id", id.toString())
                .param("entity", entity)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val columns = HackListMetaData.getColumns()
        return excelHelper!!.read(
            excelData.inputStream(), columns,
            HackPromoService.EXCEL_HEADER_ROW,
            HackPromoService.EXCEL_START_ROW
        )
    }

    private fun assertCorrectExportResults(actual: List<HackListStringItem>, expected: List<HackListStringItem>?) {
        Assertions.assertThat(actual).usingElementComparatorIgnoringFields("id", "listId", "promoId", "checkFormula")
            .isEqualTo(expected)
    }

    private fun buildExportList(indexes: List<Int> = (0..2).toList()): List<HackListStringItem> {
        val map = listOf(
            HackListStringItem(
                id = null,
                mechanics = "ФЛЭШ 18 марта",
                catteam = "FMCG",
                catman = "ivanivanon",
                category1 = "Товары для красоты",
                category2 = "Косметика, парфюмерия и уход",
                category3 = "Уход за волосами",
                categoryLeaf = "Шампуни",
                msku = "100256596649",
                ssku = "000145.65500970",
                name = "Шампунь для всех типов волос Березовый 400 мл",
                brand = "Бренд тестовы",
                isBrandA = "да",
                isCorefix = "нет",
                oldPrice = "166,93",
                currPrice = "128,41",
                promoPrice = "126,86",
                buyerDiscount = "24,0",
                recommendations = "128",
                finalPrice = "130,1",
                topOffer = "0",
                noAddedCompensation = "0",
                neededPurchase = "да",
                regPurchasePrice = "128,0",
                promoPurchasePrice = "1,0",
                promoPurchasePriceStartDate = "9/1/22",
                promoPurchasePriceFinishDate = "9/2/22",
                regMarketing = "7,0",
                regMarketingRub = "1,0",
                addMarketing = "23,0",
                addMarketingRub = "1,0",
                checkFormula = null,
                ticket = "QWERTY-123",
                deliveryDate = "7/1/22",
                localAssortmentHomeRegion = "Санкт-Петербург",
                promoSalesPlanMoscow = "44",
                deliverySchemeMoscow = "прямая",
                promoSalesPlanStPetersburg = "45",
                deliverySchemeStPetersburg = "xdoc",
                promoSalesPlanRostov = "46",
                deliverySchemeRostov = "mono-xdoc",
                promoSalesPlanSamara = "47",
                deliverySchemeSamara = "перемещение",
                promoSalesPlanYekaterinburg = "48",
                deliverySchemeYekaterinburg = "прямая",
                promoSalesPlanRub = "700,0",
                currStocks = "200",
                minref = "123,0",
                minref3p = "144,0",
                minrefDeviation = "155,0",
                listId = null,
                promoId = null,
                comment = null
            ), HackListStringItem(
                id = null,
                mechanics = "ФЛЭШ 18 марта",
                catteam = "FMCG",
                catman = "ivanivanon",
                category1 = "Товары для красоты",
                category2 = "Косметика, парфюмерия и уход",
                category3 = "Уход за волосами",
                categoryLeaf = "Шампуни",
                msku = "100256596649",
                ssku = "000145.35500970",
                name = "Шампунь для всех типов волос Березовый 400 мл",
                brand = "Бренд тестовы",
                isBrandA = "да",
                isCorefix = "нет",
                oldPrice = "166,93",
                currPrice = "128,41",
                promoPrice = "126,86",
                buyerDiscount = "24,0",
                recommendations = "128",
                finalPrice = "130,1",
                topOffer = "0",
                noAddedCompensation = "0",
                neededPurchase = "да",
                regPurchasePrice = "128,0",
                promoPurchasePrice = "1,0",
                promoPurchasePriceStartDate = "9/1/22",
                promoPurchasePriceFinishDate = "9/2/22",
                regMarketing = "7,0",
                regMarketingRub = "1,0",
                addMarketing = "23,0",
                addMarketingRub = "1,0",
                checkFormula = null,
                ticket = "QWERTY-123",
                deliveryDate = "7/1/22",
                localAssortmentHomeRegion = "Санкт-Петербург",
                promoSalesPlanMoscow = "44",
                deliverySchemeMoscow = "прямая",
                promoSalesPlanStPetersburg = "45",
                deliverySchemeStPetersburg = "xdoc",
                promoSalesPlanRostov = "46",
                deliverySchemeRostov = "mono-xdoc",
                promoSalesPlanSamara = "47",
                deliverySchemeSamara = "перемещение",
                promoSalesPlanYekaterinburg = "48",
                deliverySchemeYekaterinburg = "прямая",
                promoSalesPlanRub = "700,0",
                currStocks = "200",
                minref = "123,0",
                minref3p = "144,0",
                minrefDeviation = "155,0",
                listId = null,
                promoId = null,
                comment = "comment1"
            ),
            HackListStringItem(
                id = null,
                mechanics = "ФЛЭШ 18 марта 2",
                catteam = "FMCG",
                catman = "ivanivanon",
                category1 = "Товары для красоты",
                category2 = "Косметика, парфюмерия и уход",
                category3 = "Уход за волосами",
                categoryLeaf = "Шампуни",
                msku = "100256596649",
                ssku = "000145.65500970",
                name = "Шампунь для всех типов волос Березовый 500 мл",
                brand = "Бренд тестовы",
                isBrandA = "да",
                isCorefix = "нет",
                oldPrice = "166,93",
                currPrice = "128,41",
                promoPrice = "126,86",
                buyerDiscount = "24,0",
                recommendations = "128",
                finalPrice = "130,1",
                topOffer = "0",
                noAddedCompensation = "0",
                neededPurchase = "да",
                regPurchasePrice = "128,0",
                promoPurchasePrice = "1,0",
                promoPurchasePriceStartDate = "9/1/22",
                promoPurchasePriceFinishDate = "9/2/22",
                regMarketing = "7,0",
                regMarketingRub = "1,0",
                addMarketing = "23,0",
                addMarketingRub = "1,0",
                checkFormula = null,
                ticket = "QWERTY-123",
                deliveryDate = "7/1/22",
                localAssortmentHomeRegion = "Санкт-Петербург",
                promoSalesPlanMoscow = "44",
                deliverySchemeMoscow = "прямая",
                promoSalesPlanStPetersburg = "45",
                deliverySchemeStPetersburg = "xdoc",
                promoSalesPlanRostov = "46",
                deliverySchemeRostov = "mono-xdoc",
                promoSalesPlanSamara = "47",
                deliverySchemeSamara = "перемещение",
                promoSalesPlanYekaterinburg = "48",
                deliverySchemeYekaterinburg = "прямая",
                promoSalesPlanRub = "700,0",
                currStocks = "200",
                minref = "123,0",
                minref3p = "144,0",
                minrefDeviation = "155,0",
                listId = null,
                promoId = null,
                comment = null
            )
        ).withIndex()
            .associateBy(keySelector = { indexedValue: IndexedValue<HackListStringItem> -> indexedValue.index },
                valueTransform = { indexedValue -> indexedValue.value })

        return indexes.map { map.getValue(it) }
    }
}
