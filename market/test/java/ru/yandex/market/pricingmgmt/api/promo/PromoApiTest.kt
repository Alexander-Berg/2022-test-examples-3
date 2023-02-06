package ru.yandex.market.pricingmgmt.api.promo

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.contains
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.util.NestedServletException
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.TestUtils
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.client.promo.api.PromoApiClient
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationFilter
import ru.yandex.market.pricingmgmt.exception.ApiErrorException
import ru.yandex.market.pricingmgmt.exception.ExceptionCode
import ru.yandex.market.pricingmgmt.exception.NotFoundException
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.model.dto.PriceExcelDTO
import ru.yandex.market.pricingmgmt.model.dto.PromoSearchResultDTO
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.CompensationReceiveMethod
import ru.yandex.market.pricingmgmt.model.promo.ExcelImportingError
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoDisplayStatus
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import ru.yandex.market.pricingmgmt.model.promo.restrictions.CategoryDiscountRestriction
import ru.yandex.market.pricingmgmt.model.promo.restrictions.Restriction
import ru.yandex.market.pricingmgmt.model.promo.restrictions.RestrictionType
import ru.yandex.market.pricingmgmt.repository.postgres.PriceRepository
import ru.yandex.market.pricingmgmt.service.ManagerService
import ru.yandex.market.pricingmgmt.service.excel.core.ColumnMetaData
import ru.yandex.market.pricingmgmt.service.excel.core.ExcelHelper
import ru.yandex.market.pricingmgmt.service.excel.meta.ExcelImportingErrorMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.PromoSearchResultMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.PromoSskuMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.restrictions.RestrictionCategoryDiscountMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.restrictions.RestrictionCategoryMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.restrictions.RestrictionMskuMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.restrictions.RestrictionPartnerMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.restrictions.RestrictionVendorMetaData
import ru.yandex.market.pricingmgmt.service.excel.meta.restrictions.RestrictionWarehouseMetaData
import ru.yandex.market.pricingmgmt.service.promo.PromoCheapestAsGiftSskuExcelService.Companion.EXCEL_HEADER_ROW
import ru.yandex.market.pricingmgmt.service.promo.PromoCheapestAsGiftSskuExcelService.Companion.EXCEL_START_ROW
import ru.yandex.market.pricingmgmt.service.promo.validators.PromoRestrictionsValidator
import ru.yandex.market.pricingmgmt.service.promo.validators.UpdatePromoValidator
import ru.yandex.market.pricingmgmt.util.DateTimeUtil.getOffsetDateTimeFromEpochSecond
import ru.yandex.market.pricingmgmt.util.s3.S3ClientFactory
import ru.yandex.mj.generated.client.promoservice.model.CategoryPromoConstraintDto
import ru.yandex.mj.generated.client.promoservice.model.CategoryPromoConstraintDtoCategories
import ru.yandex.mj.generated.client.promoservice.model.CheapestAsGift
import ru.yandex.mj.generated.client.promoservice.model.GenerateableUrlDto
import ru.yandex.mj.generated.client.promoservice.model.MechanicsType
import ru.yandex.mj.generated.client.promoservice.model.MskuPromoConstraintDto
import ru.yandex.mj.generated.client.promoservice.model.PromoMainRequestParams
import ru.yandex.mj.generated.client.promoservice.model.PromoMainResponseParams
import ru.yandex.mj.generated.client.promoservice.model.PromoMechanicsParams
import ru.yandex.mj.generated.client.promoservice.model.PromoRequestV2
import ru.yandex.mj.generated.client.promoservice.model.PromoResponseV2
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2Sort
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2SrcCiface
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchResult
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchResultItem
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchResultItemSrcCiface
import ru.yandex.mj.generated.client.promoservice.model.PromoSrcParams
import ru.yandex.mj.generated.client.promoservice.model.Promotion
import ru.yandex.mj.generated.client.promoservice.model.SourceType
import ru.yandex.mj.generated.client.promoservice.model.SrcCifaceDtoV2
import ru.yandex.mj.generated.client.promoservice.model.SupplierPromoConstraintsDto
import ru.yandex.mj.generated.client.promoservice.model.VendorPromoConstraintDto
import ru.yandex.mj.generated.client.promoservice.model.WarehousePromoConstraintDto
import ru.yandex.mj.generated.server.model.CheapestAsGiftDto
import ru.yandex.mj.generated.server.model.DashboardPromoDtoResponse
import ru.yandex.mj.generated.server.model.DashboardPromoItemDtoResponse
import ru.yandex.mj.generated.server.model.ErrorResponse
import ru.yandex.mj.generated.server.model.IdmRoleDto
import ru.yandex.mj.generated.server.model.MskuValidateDtoResponse
import ru.yandex.mj.generated.server.model.MskuValidateDtoResponseMsku
import ru.yandex.mj.generated.server.model.PromoCategoriesRestrictionItemDto
import ru.yandex.mj.generated.server.model.PromoCategoriesRestrictionItemShortDto
import ru.yandex.mj.generated.server.model.PromoChannelDto
import ru.yandex.mj.generated.server.model.PromoDtoRequest
import ru.yandex.mj.generated.server.model.PromoDtoResponse
import ru.yandex.mj.generated.server.model.PromoIdDto
import ru.yandex.mj.generated.server.model.PromoPromotionDto
import ru.yandex.mj.generated.server.model.PromoRestrictionItemDto
import ru.yandex.mj.generated.server.model.PromoRestrictionItemRequestDto
import ru.yandex.mj.generated.server.model.PromoRestrictionItemResponseDto
import ru.yandex.mj.generated.server.model.PromocodeDto
import java.io.RandomAccessFile
import java.nio.channels.Channels
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
@DbUnitDataSet(before = ["PromoApiTest.csv"])
@WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = ["PRICING_MGMT_ACCESS", "PROMO_USER"])
open class PromoApiTest : ControllerTest() {

    companion object {
        val startAtFrom: Long = OffsetDateTime.parse("2017-12-03T08:58:50Z").toEpochSecond()
        val startAtTo: Long = OffsetDateTime.parse("2017-12-03T09:32:10Z").toEpochSecond()
        val endAtFrom: Long = OffsetDateTime.parse("2017-12-03T08:58:50Z").toEpochSecond()
        val endAtTo: Long = OffsetDateTime.parse("2017-12-03T09:32:10Z").toEpochSecond()
        val startAt: Long = OffsetDateTime.parse("2017-12-03T09:15:30Z").toEpochSecond()
        val startAtExcel: OffsetDateTime =
            getOffsetDateTimeFromEpochSecond(startAt, "Europe/Moscow")!!.withOffsetSameLocal(ZoneOffset.UTC)
        val endAt: Long = OffsetDateTime.parse("2017-12-03T09:16:30Z").toEpochSecond()
        val endAtExcel: OffsetDateTime =
            getOffsetDateTimeFromEpochSecond(endAt, "Europe/Moscow")!!.withOffsetSameLocal(ZoneOffset.UTC)
        val piPublishDate: Long = OffsetDateTime.parse("2017-12-03T09:14:30Z").toEpochSecond()
        const val CURRENT_USER_LOGIN = PassportAuthenticationFilter.LOCAL_DEV
        const val PROMO_ID = "cf_100001"
        const val bucketName = "ciface-promo-mediaplans"
        const val s3TestFile = "/s3-test/test.txt"
    }

    @MockBean
    private lateinit var promoApiClient: PromoApiClient

    @MockBean
    private lateinit var managerService: ManagerService

    @MockBean
    private lateinit var updatePromoValidator: UpdatePromoValidator

    @Autowired
    private lateinit var promoRestrictionsValidator: PromoRestrictionsValidator

    @Autowired
    private lateinit var priceRepository: PriceRepository

    @Autowired
    private lateinit var excelHelper: ExcelHelper<PriceExcelDTO>

    @Autowired
    private lateinit var excelErrorsHelper: ExcelHelper<ExcelImportingError>

    @Autowired
    private lateinit var restrictionExcelHelper: ExcelHelper<Restriction>

    @Autowired
    private lateinit var categoryExcelHelper: ExcelHelper<CategoryDiscountRestriction>

    @Autowired
    private lateinit var promoSearchExcelHelper: ExcelHelper<PromoSearchResultDTO>

    @Autowired
    private lateinit var s3ClientFactory: S3ClientFactory

    private val s3Client: AmazonS3 get() = s3ClientFactory.s3Client

    @BeforeEach
    fun setUp() {
        reset(promoApiClient)
        reset(s3Client)
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun createPromoSuccessTest() {
        val requestDto = buildPromoDtoRequest()
        val expectedResponseDto = PromoIdDto()
        expectedResponseDto.id = PROMO_ID

        doNothing().`when`(promoApiClient)?.createPromo(notNull())

        doReturn(true).`when`(managerService)?.isUserTrade("sergey321")
        doReturn(true).`when`(managerService)?.isUserMarkom("dasha123")


        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        val promoRequest = PromoRequestV2()
        //region init promoRequest
        promoRequest
            .promoId(PROMO_ID)
            .modifiedBy("localDeveloper")
            .main(
                PromoMainRequestParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(false)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
                    .sourceType(SourceType.CATEGORYIFACE)
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(CheapestAsGift().count(2))
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author(CURRENT_USER_LOGIN)
                            .finalBudget(true)
                            .autoCompensation(false)
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .piPublishedAt(piPublishDate)
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto().exclude(false).suppliers(listOf()))
            .warehousesConstraints(WarehousePromoConstraintDto().exclude(false).warehouses(listOf(11L)))
            .vendorsConstraints(VendorPromoConstraintDto().exclude(false).vendors(listOf()))
            .mskusConstraints(MskuPromoConstraintDto().exclude(false).mskus(listOf()))
            .categoriesConstraints(CategoryPromoConstraintDto().categories(listOf()))
        //endregion

        verify(promoApiClient)?.createPromo(promoRequest)
    }

    @Test
    fun createPromoWithCorrectSuccessTest() {
        val requestDto = buildPromoDtoRequest()

        requestDto.promocode(
            PromocodeDto()
                .code("test")
        )

        val expectedResponseDto = PromoIdDto()
        expectedResponseDto.id = PROMO_ID

        doNothing().`when`(promoApiClient)?.createPromo(notNull())

        doReturn(true).`when`(managerService)?.isUserTrade("sergey321")
        doReturn(true).`when`(managerService)?.isUserMarkom("dasha123")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        val promoRequest = PromoRequestV2()
        //region init promoRequest
        promoRequest
            .promoId(PROMO_ID)
            .modifiedBy("localDeveloper")
            .main(
                PromoMainRequestParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(false)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
                    .sourceType(SourceType.CATEGORYIFACE)
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(CheapestAsGift().count(2))
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author(CURRENT_USER_LOGIN)
                            .finalBudget(true)
                            .autoCompensation(false)
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .piPublishedAt(piPublishDate)
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto().exclude(false).suppliers(listOf()))
            .warehousesConstraints(WarehousePromoConstraintDto().exclude(false).warehouses(listOf(11L)))
            .vendorsConstraints(VendorPromoConstraintDto().exclude(false).vendors(listOf()))
            .mskusConstraints(MskuPromoConstraintDto().exclude(false).mskus(listOf()))
            .categoriesConstraints(CategoryPromoConstraintDto().categories(listOf()))
        //endregion

        verify(promoApiClient)?.createPromo(promoRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"]
    )
    fun createPromoWithRestrictionsSuccessTest() {
        val requestDto = buildPromoDtoRequest()
            .partnersRestriction(listOf(1L, 1L, 2L))
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemShortDto().id(21L),
                    PromoCategoriesRestrictionItemShortDto().id(21L),
                    PromoCategoriesRestrictionItemShortDto().id(22L)
                )
            )
            .vendorsRestriction(listOf(31L, 31L, 32L))
            .mskusRestriction(listOf(41L, 41L, 42L))
            .warehouseRestriction(listOf(21L, 21L))
        val expectedResponseDto = PromoIdDto()
        expectedResponseDto.id = PROMO_ID

        doNothing().`when`(promoApiClient)?.createPromo(notNull())

        doReturn(true).`when`(managerService)?.isUserTrade("sergey321")
        doReturn(true).`when`(managerService)?.isUserMarkom("dasha123")


        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        val promoRequest = PromoRequestV2()
        promoRequest
            .promoId(PROMO_ID)
            .modifiedBy("localDeveloper")
            .main(
                PromoMainRequestParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(false)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
                    .sourceType(SourceType.CATEGORYIFACE)
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(CheapestAsGift().count(2))
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author(CURRENT_USER_LOGIN)
                            .finalBudget(true)
                            .autoCompensation(false)
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .piPublishedAt(piPublishDate)
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto().exclude(false).suppliers(listOf(1L, 2L)))
            .categoriesConstraints(
                CategoryPromoConstraintDto().categories(
                    listOf(
                        CategoryPromoConstraintDtoCategories().id("21"),
                        CategoryPromoConstraintDtoCategories().id("22")
                    )
                )
            )
            .vendorsConstraints(VendorPromoConstraintDto().exclude(false).vendors(listOf("31", "32")))
            .mskusConstraints(MskuPromoConstraintDto().exclude(false).mskus(listOf(41L, 42L)))
            .warehousesConstraints(WarehousePromoConstraintDto().exclude(false).warehouses(listOf(21L)))

        verify(promoApiClient)?.createPromo(promoRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"]
    )
    fun createDirectDiscountPromoWithRestrictionsSuccessTest() {
        val requestDto = buildPromoDtoRequest()
            .mechanicsType("DIRECT_DISCOUNT")
            .cheapestAsGift(null)
            .partnersRestriction(listOf(1L, 2L))
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemShortDto().id(21L).percent(10),
                    PromoCategoriesRestrictionItemShortDto().id(22L).percent(20)
                )
            )
            .vendorsRestriction(listOf(31L, 32L))
            .mskusRestriction(listOf(41L, 42L))
        val expectedResponseDto = PromoIdDto()
        expectedResponseDto.id = PROMO_ID

        doNothing().`when`(promoApiClient)?.createPromo(notNull())

        doReturn(true).`when`(managerService)?.isUserTrade("sergey321")
        doReturn(true).`when`(managerService)?.isUserMarkom("dasha123")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        val promoRequest = PromoRequestV2()
        promoRequest
            .promoId(PROMO_ID)
            .modifiedBy("localDeveloper")
            .main(
                PromoMainRequestParams()
                    .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(false)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
                    .sourceType(SourceType.CATEGORYIFACE)
            )
            .mechanics(
                PromoMechanicsParams()
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author(CURRENT_USER_LOGIN)
                            .finalBudget(true)
                            .autoCompensation(false)
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .piPublishedAt(piPublishDate)
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto().exclude(false).suppliers(listOf(1L, 2L)))
            .categoriesConstraints(
                CategoryPromoConstraintDto().categories(
                    listOf(
                        CategoryPromoConstraintDtoCategories().id("21").percent(10),
                        CategoryPromoConstraintDtoCategories().id("22").percent(20)
                    )
                )
            )
            .vendorsConstraints(VendorPromoConstraintDto().exclude(false).vendors(listOf("31", "32")))
            .mskusConstraints(MskuPromoConstraintDto().exclude(false).mskus(listOf(41L, 42L)))
            .warehousesConstraints(WarehousePromoConstraintDto().exclude(false).warehouses(listOf(11L)))

        verify(promoApiClient)?.createPromo(promoRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"]
    )
    fun createPromoWithRestrictionsWithIncorrectSupplierTest() {
        val requestDto = buildPromoDtoRequest()
            .partnersRestriction(listOf(998L, 999L))
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemShortDto().id(21L),
                    PromoCategoriesRestrictionItemShortDto().id(22L)
                )
            )
            .vendorsRestriction(listOf(31L, 32L))
            .mskusRestriction(listOf(41L, 42L))
        val expectedResponseDto = PromoIdDto()
        expectedResponseDto.id = PROMO_ID

        doNothing().`when`(promoApiClient)?.createPromo(notNull())

        doReturn(true).`when`(managerService)?.isUserTrade("sergey321")
        doReturn(true).`when`(managerService)?.isUserMarkom("dasha123")

        val error = ErrorResponse()
        error.errorCode = "PROMO_RESTRICTION_INTERNAL"
        error.message = "Партнеры не найдены: 998,999"
        error.errorFields = listOf("partnersRestriction")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun createPromoWithValidationException() {
        val requestDto = buildPromoDtoRequest()

        doThrow(
            ValidationException(
                code = ExceptionCode.PROMO_REQUIRED_FIELD_EMPTY,
                message = "Some exception",
                errorField = "errorField"
            )
        ).`when`(promoApiClient)?.createPromo(notNull())
        doReturn(true).`when`(managerService)?.isUserTrade("sergey321")
        doReturn(true).`when`(managerService)?.isUserMarkom("dasha123")

        val error = ErrorResponse()
        error.errorCode = "PROMO_REQUIRED_FIELD_EMPTY"
        error.message = "Some exception"
        error.errorFields = listOf("errorField")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/create")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    private fun buildPromoDtoRequest() = PromoDtoRequest()
        .promoKind("CROSS_CATEGORY")
        .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
        .parentPromoId("cf_000000000")
        .name("Promo name")
        .startDate(startAt)
        .endDate(endAt)
        .tradeManager("sergey321")
        .departments(listOf("HEALTH", "FMCG"))
        .streams(listOf("stream1", "stream2"))
        .markom("dasha123")
        .purpose("GMV_GENERATION")
        .budgetOwner("PRODUCT")
        .supplierType("1P")
        .compensationSource("PART_MARKET_PART_PARTNER")
        .rulesPageUrl("/rules/page/url")
        .landingPageUrl("/landing/page/url")
        .disabled(false)
        .hidden(false)
        .status("NEW")
        .cheapestAsGift(CheapestAsGiftDto().count(2))
        .compensationReceiveMethods(
            listOf(
                CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
            )
        )
        .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
        .assortmentLoadMethod("TRACKER")
        .piPublishedDate(piPublishDate)
        .promotion(
            PromoPromotionDto()
                .finalBudget(true)
                .autoCompensation(false)
                .channels(
                    listOf(
                        PromoChannelDto()
                            .id(111L.toString())
                            .catteamName("catteam")
                            .categoryName("category")
                            .name("channel")
                            .period(123L)
                            .unit("недели")
                            .plan(124L)
                            .fact(125L)
                            .canEdit(true)
                            .comment("comment")
                    )
                )
        )
        .categoriesRestriction(emptyList())
        .partnersRestriction(emptyList())
        .vendorsRestriction(emptyList())
        .mskusRestriction(emptyList())
        .warehouseRestriction(listOf(11L))

    @Test
    fun getPromoSuccessTest() {
        val promoResponse = PromoResponseV2()
        promoResponse
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(false)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(CheapestAsGift().count(2))
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author("author_login")
                            .finalBudget(true)
                            .autoCompensation(false)
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanFileName")
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        val expectedResponseDto = PromoDtoResponse()
            .promoId(PROMO_ID)
            .promoKind("CROSS_CATEGORY")
            .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
            .parentPromoId("cf_000000000")
            .name("Promo name")
            .startDate(startAt)
            .endDate(endAt)
            .tradeManager("sergey321")
            .departments(listOf("HEALTH", "FMCG"))
            .department("HEALTH")
            .streams(listOf("stream1", "stream2"))
            .markom("dasha123")
            .purpose("GMV_GENERATION")
            .budgetOwner("PRODUCT")
            .supplierType("1P")
            .compensationSource("PART_MARKET_PART_PARTNER")
            .rulesPageUrl("/rules/page/url")
            .rulesPageUrlAutogenerated(false)
            .landingPageUrl("/landing/page/url")
            .landingPageUrlAutogenerated(false)
            .disabled(false)
            .hidden(false)
            .status("NEW")
            .displayStatus("NEW")
            .cheapestAsGift(CheapestAsGiftDto().count(2))
            .author("author_login")
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)
            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
            .mediaPlanFileName("mediaPlanFileName")
            .assortmentLoadMethod("TRACKER")
            .promotion(
                PromoPromotionDto()
                    .finalBudget(true)
                    .autoCompensation(false)
                    .channels(
                        listOf(
                            PromoChannelDto()
                                .id(111L.toString())
                                .catteamName("catteam")
                                .categoryName("category")
                                .name("channel")
                                .period(123L)
                                .unit("недели")
                                .plan(124L)
                                .fact(125L)
                                .canEdit(true)
                                .comment("comment")
                        )
                    )
            )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    fun getPromoSuccessTestWithAutogeneratedLinks() {
        val promoResponse = PromoResponseV2()
        promoResponse
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(true)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(true)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(CheapestAsGift().count(2))
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author("author_login")
                            .finalBudget(true)
                            .autoCompensation(false)
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanFileName")
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        val expectedResponseDto = PromoDtoResponse()
            .promoId(PROMO_ID)
            .promoKind("CROSS_CATEGORY")
            .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
            .parentPromoId("cf_000000000")
            .name("Promo name")
            .startDate(startAt)
            .endDate(endAt)
            .tradeManager("sergey321")
            .departments(listOf("HEALTH", "FMCG"))
            .department("HEALTH")
            .streams(listOf("stream1", "stream2"))
            .markom("dasha123")
            .purpose("GMV_GENERATION")
            .budgetOwner("PRODUCT")
            .supplierType("1P")
            .compensationSource("PART_MARKET_PART_PARTNER")
            .rulesPageUrl("/rules/page/url")
            .rulesPageUrlAutogenerated(true)
            .landingPageUrl("/landing/page/url")
            .landingPageUrlAutogenerated(true)
            .disabled(false)
            .hidden(false)
            .status("NEW")
            .displayStatus("NEW")
            .cheapestAsGift(CheapestAsGiftDto().count(2))
            .author("author_login")
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)
            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
            .mediaPlanFileName("mediaPlanFileName")
            .assortmentLoadMethod("TRACKER")
            .promotion(
                PromoPromotionDto()
                    .finalBudget(true)
                    .autoCompensation(false)
                    .channels(
                        listOf(
                            PromoChannelDto()
                                .id(111L.toString())
                                .catteamName("catteam")
                                .categoryName("category")
                                .name("channel")
                                .period(123L)
                                .unit("недели")
                                .plan(124L)
                                .fact(125L)
                                .canEdit(true)
                                .comment("comment")
                        )
                    )
            )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"]
    )
    fun getPromoDirectDiscountSuccessTest() {
        val promoResponse = PromoResponseV2()
        promoResponse
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(false)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author("author_login")
                            .finalBudget(true)
                            .autoCompensation(false)
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanFileName")
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )
            .categoriesConstraints(
                CategoryPromoConstraintDto()
                    .categories(
                        listOf(
                            CategoryPromoConstraintDtoCategories().id("21").percent(11),
                            CategoryPromoConstraintDtoCategories().id("22").percent(22)
                        )
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        val expectedResponseDto = PromoDtoResponse()
            .promoId(PROMO_ID)
            .promoKind("CROSS_CATEGORY")
            .mechanicsType("DIRECT_DISCOUNT")
            .parentPromoId("cf_000000000")
            .name("Promo name")
            .startDate(startAt)
            .endDate(endAt)
            .tradeManager("sergey321")
            .departments(listOf("HEALTH", "FMCG"))
            .department("HEALTH")
            .streams(listOf("stream1", "stream2"))
            .markom("dasha123")
            .purpose("GMV_GENERATION")
            .budgetOwner("PRODUCT")
            .supplierType("1P")
            .compensationSource("PART_MARKET_PART_PARTNER")
            .rulesPageUrl("/rules/page/url")
            .rulesPageUrlAutogenerated(false)
            .landingPageUrl("/landing/page/url")
            .landingPageUrlAutogenerated(false)
            .disabled(false)
            .hidden(false)
            .status("NEW")
            .displayStatus("NEW")
            .author("author_login")
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)
            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
            .mediaPlanFileName("mediaPlanFileName")
            .assortmentLoadMethod("TRACKER")
            .promotion(
                PromoPromotionDto()
                    .finalBudget(true)
                    .autoCompensation(false)
                    .channels(
                        listOf(
                            PromoChannelDto()
                                .id(111L.toString())
                                .catteamName("catteam")
                                .categoryName("category")
                                .name("channel")
                                .period(123L)
                                .unit("недели")
                                .plan(124L)
                                .fact(125L)
                                .canEdit(true)
                                .comment("comment")
                        )
                    )
            )
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemDto().id(21).name("category_21").outdated(false).percent(11),
                    PromoCategoriesRestrictionItemDto().id(22).name("category_22").outdated(true).percent(22)
                )
            )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    fun getPromoNotFoundTest() {
        doThrow(
            NotFoundException(
                code = ExceptionCode.PROMOBOSS_INTERNAL,
                message = "Promo not found"
            )
        ).`when`(promoApiClient)?.getPromo(PROMO_ID)

        val error = ErrorResponse()
        error.errorCode = "PROMOBOSS_INTERNAL"
        error.message = "Promo not found"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun updatePromoSuccessTest() {
        val requestDto = buildPromoDtoRequest()
            .promoId(PROMO_ID)

        val promoGetResponse = buildGetPromoClientResponse()

        val promoUpdateResponse = buildUpdatePromoClientResponse()
        val updatePromoRequest = buildUpdatePromoClientRequest()
        `when`(promoApiClient.getPromo(PROMO_ID)).thenReturn(promoGetResponse, promoUpdateResponse)
        doNothing().`when`(promoApiClient)?.updatePromo(notNull())

        val expectedResponseDto = PromoDtoResponse()
        //region init expectedResponseDto
        expectedResponseDto.promoId(PROMO_ID)
            .promoKind("CROSS_CATEGORY")
            .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
            .parentPromoId("cf_000000000")
            .name("Promo name")
            .startDate(startAt)
            .endDate(endAt)
            .tradeManager("sergey321")
            .departments(listOf("HEALTH", "FMCG"))
            .department("HEALTH")
            .streams(listOf("stream1", "stream2"))
            .markom("dasha123")
            .purpose("GMV_GENERATION")
            .budgetOwner("PRODUCT")
            .supplierType("1P")
            .compensationSource("PART_MARKET_PART_PARTNER")
            .rulesPageUrl("/rules/page/url")
            .rulesPageUrlAutogenerated(false)
            .landingPageUrl("/landing/page/url")
            .landingPageUrlAutogenerated(false)
            .disabled(false)
            .hidden(false)
            .status("NEW")
            .displayStatus("NEW")
            .cheapestAsGift(CheapestAsGiftDto().count(2))
            .author("petr")
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)
            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
            .assortmentLoadMethod("TRACKER")
            .promotion(
                PromoPromotionDto()
                    .finalBudget(true)
                    .autoCompensation(false)
                    .channels(
                        listOf(
                            PromoChannelDto()
                                .id(111L.toString())
                                .catteamName("catteam")
                                .categoryName("category")
                                .name("channel")
                                .period(123L)
                                .unit("недели")
                                .plan(124L)
                                .fact(125L)
                                .canEdit(true)
                                .comment("comment")
                        )
                    )
            )
            .partnersRestriction(emptyList())
            .categoriesRestriction(emptyList())
            .vendorsRestriction(emptyList())
            .mskusRestriction(emptyList())
            .warehouseRestriction(listOf(11L))
            .warehouseRestrictionV2(
                listOf(
                    PromoRestrictionItemDto().id(11L).name("warehouse_11").outdated(false),
                )
            )
        //endregion

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/update")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        verify(promoApiClient)?.updatePromo(updatePromoRequest)
    }

    @Test
    fun updatePromoWithCorrectSuccessTest() {
        val requestDto = buildPromoDtoRequest()
            .promoId(PROMO_ID)

        requestDto.promocode(
            PromocodeDto()
                .code("Test")
        )

        val promoGetResponse = buildGetPromoClientResponse()

        val promoUpdateResponse = buildUpdatePromoClientResponse()
        val updatePromoRequest = buildUpdatePromoClientRequest()

        `when`(promoApiClient.getPromo(PROMO_ID)).thenReturn(promoGetResponse, promoUpdateResponse)
        doNothing().`when`(promoApiClient)?.updatePromo(updatePromoRequest)

        val expectedResponseDto = PromoDtoResponse()
        //region init expectedResponseDto
        expectedResponseDto.promoId(PROMO_ID)
            .promoKind("CROSS_CATEGORY")
            .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
            .parentPromoId("cf_000000000")
            .name("Promo name")
            .startDate(startAt)
            .endDate(endAt)
            .tradeManager("sergey321")
            .departments(listOf("HEALTH", "FMCG"))
            .streams(listOf("stream1", "stream2"))
            .markom("dasha123")
            .purpose("GMV_GENERATION")
            .budgetOwner("PRODUCT")
            .supplierType("1P")
            .compensationSource("PART_MARKET_PART_PARTNER")
            .rulesPageUrl("/rules/page/url")
            .rulesPageUrlAutogenerated(false)
            .landingPageUrl("/landing/page/url")
            .landingPageUrlAutogenerated(false)
            .disabled(false)
            .hidden(false)
            .status("NEW")
            .displayStatus("NEW")
            .cheapestAsGift(CheapestAsGiftDto().count(2))
            .author("petr")
            .departments(listOf("HEALTH", "FMCG"))
            .department("HEALTH")
            .streams(listOf("stream1", "stream2"))
            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
            .assortmentLoadMethod("TRACKER")
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)
            .promotion(
                PromoPromotionDto()
                    .finalBudget(true)
                    .autoCompensation(false)
                    .channels(
                        listOf(
                            PromoChannelDto()
                                .id(111L.toString())
                                .catteamName("catteam")
                                .categoryName("category")
                                .name("channel")
                                .period(123L)
                                .unit("недели")
                                .plan(124L)
                                .fact(125L)
                                .canEdit(true)
                                .comment("comment")
                        )
                    )
            )
            .partnersRestriction(emptyList())
            .categoriesRestriction(emptyList())
            .vendorsRestriction(emptyList())
            .mskusRestriction(emptyList())
            .warehouseRestriction(listOf(11L))
            .warehouseRestrictionV2(
                listOf(
                    PromoRestrictionItemDto().id(11L).name("warehouse_11").outdated(false),
                )
            )
        //endregion

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/update")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        verify(promoApiClient)?.updatePromo(updatePromoRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"]
    )
    fun updatePromoWithRestrictionsSuccessTest() {
        val requestDto = buildPromoDtoRequest()
            .promoId(PROMO_ID)
            .partnersRestriction(listOf(1L, 1L, 2L))
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemShortDto().id(21L).percent(10),
                    PromoCategoriesRestrictionItemShortDto().id(21L).percent(10),
                    PromoCategoriesRestrictionItemShortDto().id(22L).percent(20)
                )
            )
            .vendorsRestriction(listOf(31L, 31L, 32L))
            .mskusRestriction(listOf(41L, 41L, 42L))
            .warehouseRestriction(listOf(21L, 21L, 22L))

        val promoGetResponse = buildGetPromoClientResponse()

        val promoUpdateResponse = buildUpdatePromoClientResponse()
        promoUpdateResponse.suppliersConstraints!!.suppliers = listOf(1L, 2L)
        promoUpdateResponse.categoriesConstraints!!.categories =
            listOf(
                CategoryPromoConstraintDtoCategories().id("21").percent(10),
                CategoryPromoConstraintDtoCategories().id("22").percent(20)
            )
        promoUpdateResponse.vendorsConstraints!!.vendors = listOf("31", "32")
        promoUpdateResponse.mskusConstraints!!.mskus = listOf(41L, 42L)
        promoUpdateResponse.warehousesConstraints!!.warehouses = listOf(21L, 22L)

        val updatePromoRequest = buildUpdatePromoClientRequest()
        updatePromoRequest.suppliersConstraints!!.suppliers = listOf(1L, 2L)
        updatePromoRequest.categoriesConstraints!!.categories =
            listOf(
                CategoryPromoConstraintDtoCategories().id("21").percent(10),
                CategoryPromoConstraintDtoCategories().id("22").percent(20)
            )
        updatePromoRequest.vendorsConstraints!!.vendors = listOf("31", "32")
        updatePromoRequest.mskusConstraints!!.mskus = listOf(41L, 42L)
        updatePromoRequest.warehousesConstraints!!.warehouses = listOf(21L, 22L)

        `when`(promoApiClient.getPromo(PROMO_ID)).thenReturn(promoGetResponse, promoUpdateResponse)
        doNothing().`when`(promoApiClient)?.updatePromo(updatePromoRequest)

        doAnswer {
            promoRestrictionsValidator.validate(it.arguments[0] as Promo, null)
        }.`when`(updatePromoValidator)
            ?.validate(TestUtils.any(Promo::class.java), TestUtils.any(Promo::class.java))

        val expectedResponseDto = PromoDtoResponse()
        //region init expectedResponseDto
        expectedResponseDto.promoId(PROMO_ID)
            .promoKind("CROSS_CATEGORY")
            .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
            .parentPromoId("cf_000000000")
            .name("Promo name")
            .startDate(startAt)
            .endDate(endAt)
            .tradeManager("sergey321")
            .departments(listOf("HEALTH", "FMCG"))
            .department("HEALTH")
            .streams(listOf("stream1", "stream2"))
            .streams(listOf("stream1", "stream2"))
            .markom("dasha123")
            .purpose("GMV_GENERATION")
            .budgetOwner("PRODUCT")
            .supplierType("1P")
            .compensationSource("PART_MARKET_PART_PARTNER")
            .rulesPageUrl("/rules/page/url")
            .rulesPageUrlAutogenerated(false)
            .landingPageUrl("/landing/page/url")
            .landingPageUrlAutogenerated(false)
            .disabled(false)
            .hidden(false)
            .status("NEW")
            .displayStatus("NEW")
            .cheapestAsGift(CheapestAsGiftDto().count(2))
            .author("petr")
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)
            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
            .assortmentLoadMethod("TRACKER")
            .promotion(
                PromoPromotionDto()
                    .finalBudget(true)
                    .autoCompensation(false)
                    .channels(
                        listOf(
                            PromoChannelDto()
                                .id(111L.toString())
                                .catteamName("catteam")
                                .categoryName("category")
                                .name("channel")
                                .period(123L)
                                .unit("недели")
                                .plan(124L)
                                .fact(125L)
                                .canEdit(true)
                                .comment("comment")
                        )
                    )
            )
            .partnersRestriction(
                listOf(
                    PromoRestrictionItemDto().id(1L).name("supplier_3p_1").outdated(false),
                    PromoRestrictionItemDto().id(2L).name("supplier_3p_2").outdated(true)
                )
            )
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemDto().id(21L).name("category_21").outdated(false).percent(10),
                    PromoCategoriesRestrictionItemDto().id(22L).name("category_22").outdated(true).percent(20),
                )
            )
            .vendorsRestriction(
                listOf(
                    PromoRestrictionItemDto().id(31L).name("vendor_31").outdated(false),
                    PromoRestrictionItemDto().id(32L).name("vendor_32").outdated(true)
                )
            )
            .mskusRestriction(
                listOf(
                    PromoRestrictionItemDto().id(41L).name("msku_41").outdated(false),
                    PromoRestrictionItemDto().id(42L).name("msku_42").outdated(true)
                )
            )
            .warehouseRestriction(
                listOf(21L, 22L)
            )
            .warehouseRestrictionV2(
                listOf(
                    PromoRestrictionItemDto().id(21L).name("warehouse_21").outdated(false),
                    PromoRestrictionItemDto().id(22L).name("warehouse_22").outdated(true)
                )
            )
        //endregion

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/update")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        verify(promoApiClient)?.updatePromo(updatePromoRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"]
    )
    fun updateDirectDiscountPromoWithRestrictionsSuccessTest() {
        val requestDto = buildPromoDtoRequest()
            .mechanicsType("DIRECT_DISCOUNT")
            .cheapestAsGift(null)
            .promoId(PROMO_ID)
            .partnersRestriction(listOf(1L, 2L))
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemShortDto().id(21L).percent(10),
                    PromoCategoriesRestrictionItemShortDto().id(22L).percent(20)
                )
            )
            .vendorsRestriction(listOf(31L, 32L))
            .mskusRestriction(listOf(41L, 42L))
            .warehouseRestriction(listOf(21L, 22L))

        val promoGetResponse = buildGetPromoClientResponse()
        promoGetResponse.main!!.mechanicsType(MechanicsType.DIRECT_DISCOUNT)
        promoGetResponse.mechanics!!.cheapestAsGift(null)

        val promoUpdateResponse = buildUpdatePromoClientResponse()
        promoUpdateResponse.main!!.mechanicsType(MechanicsType.DIRECT_DISCOUNT)
        promoUpdateResponse.mechanics!!.cheapestAsGift(null)

        promoUpdateResponse.suppliersConstraints!!.suppliers = listOf(1L, 2L)
        promoUpdateResponse.categoriesConstraints!!.categories =
            listOf(
                CategoryPromoConstraintDtoCategories().id("21").percent(10),
                CategoryPromoConstraintDtoCategories().id("22").percent(20)
            )
        promoUpdateResponse.vendorsConstraints!!.vendors = listOf("31", "32")
        promoUpdateResponse.mskusConstraints!!.mskus = listOf(41L, 42L)
        promoUpdateResponse.warehousesConstraints!!.warehouses = listOf(21L, 22L)

        val updatePromoRequest = buildUpdatePromoClientRequest()
        updatePromoRequest.main?.mechanicsType(MechanicsType.DIRECT_DISCOUNT)
        updatePromoRequest.mechanics?.cheapestAsGift(null)
        updatePromoRequest.suppliersConstraints!!.suppliers = listOf(1L, 2L)
        updatePromoRequest.categoriesConstraints!!.categories =
            listOf(
                CategoryPromoConstraintDtoCategories().id("21").percent(10),
                CategoryPromoConstraintDtoCategories().id("22").percent(20)
            )
        updatePromoRequest.vendorsConstraints!!.vendors = listOf("31", "32")
        updatePromoRequest.mskusConstraints!!.mskus = listOf(41L, 42L)
        updatePromoRequest.warehousesConstraints!!.warehouses = listOf(21L, 22L)

        `when`(promoApiClient.getPromo(PROMO_ID)).thenReturn(promoGetResponse, promoUpdateResponse)
        doNothing().`when`(promoApiClient)?.updatePromo(notNull())

        doAnswer {
            promoRestrictionsValidator.validate(it.arguments[0] as Promo, null)
        }.`when`(updatePromoValidator)
            ?.validate(TestUtils.any(Promo::class.java), TestUtils.any(Promo::class.java))

        val expectedResponseDto = PromoDtoResponse()
        //region init expectedResponseDto
        expectedResponseDto.promoId(PROMO_ID)
            .promoKind("CROSS_CATEGORY")
            .mechanicsType("DIRECT_DISCOUNT")
            .parentPromoId("cf_000000000")
            .name("Promo name")
            .startDate(startAt)
            .endDate(endAt)
            .tradeManager("sergey321")
            .departments(listOf("HEALTH", "FMCG"))
            .department("HEALTH")
            .streams(listOf("stream1", "stream2"))
            .markom("dasha123")
            .purpose("GMV_GENERATION")
            .budgetOwner("PRODUCT")
            .supplierType("1P")
            .compensationSource("PART_MARKET_PART_PARTNER")
            .rulesPageUrl("/rules/page/url")
            .rulesPageUrlAutogenerated(false)
            .landingPageUrl("/landing/page/url")
            .landingPageUrlAutogenerated(false)
            .disabled(false)
            .hidden(false)
            .status("NEW")
            .displayStatus("NEW")
            .author("petr")
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)
            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
            .assortmentLoadMethod("TRACKER")
            .promotion(
                PromoPromotionDto()
                    .finalBudget(true)
                    .autoCompensation(false)
                    .channels(
                        listOf(
                            PromoChannelDto()
                                .id(111L.toString())
                                .catteamName("catteam")
                                .categoryName("category")
                                .name("channel")
                                .period(123L)
                                .unit("недели")
                                .plan(124L)
                                .fact(125L)
                                .canEdit(true)
                                .comment("comment")
                        )
                    )
            )
            .partnersRestriction(
                listOf(
                    PromoRestrictionItemDto().id(1L).name("supplier_3p_1").outdated(false),
                    PromoRestrictionItemDto().id(2L).name("supplier_3p_2").outdated(true)
                )
            )
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemDto().id(21L).name("category_21").outdated(false).percent(10),
                    PromoCategoriesRestrictionItemDto().id(22L).name("category_22").outdated(true).percent(20),
                )
            )
            .vendorsRestriction(
                listOf(
                    PromoRestrictionItemDto().id(31L).name("vendor_31").outdated(false),
                    PromoRestrictionItemDto().id(32L).name("vendor_32").outdated(true)
                )
            )
            .mskusRestriction(
                listOf(
                    PromoRestrictionItemDto().id(41L).name("msku_41").outdated(false),
                    PromoRestrictionItemDto().id(42L).name("msku_42").outdated(true)
                )
            )
            .warehouseRestriction(
                listOf(21L, 22L)
            )
            .warehouseRestrictionV2(
                listOf(
                    PromoRestrictionItemDto().id(21L).name("warehouse_21").outdated(false),
                    PromoRestrictionItemDto().id(22L).name("warehouse_22").outdated(true)
                )
            )
        //endregion

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/update")
                .contentType("application/json")
                .content(dtoToString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResponseDto)))

        verify(promoApiClient)?.updatePromo(updatePromoRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"]
    )
    fun updatePromoWithRestrictionsAndIncorrectSupplierTest() {
        val requestDto = buildPromoDtoRequest()
            .promoId(PROMO_ID)
            .partnersRestriction(listOf(998L, 999L))
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemShortDto().id(21L),
                    PromoCategoriesRestrictionItemShortDto().id(22L)
                )
            )
            .vendorsRestriction(listOf(31L, 32L))
            .mskusRestriction(listOf(41L, 42L))

        val promoGetResponse = buildGetPromoClientResponse()
        doReturn(promoGetResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        doAnswer {
            promoRestrictionsValidator.validate(it.arguments[0] as Promo, null)
        }.`when`(updatePromoValidator)
            ?.validate(TestUtils.any(Promo::class.java), TestUtils.any(Promo::class.java))

        val error = ErrorResponse()
        error.errorCode = "PROMO_RESTRICTION_INTERNAL"
        error.message = "Партнеры не найдены: 998,999"
        error.errorFields = listOf("partnersRestriction")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/update")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun updatePromoNotFoundTest() {
        val requestDto = buildPromoDtoRequest()
            .promoId(PROMO_ID)

        doThrow(
            NotFoundException(
                code = ExceptionCode.PROMOBOSS_INTERNAL,
                message = "Promo not found"
            )
        ).`when`(promoApiClient)?.getPromo(PROMO_ID)

        val error = ErrorResponse()
        error.errorCode = "PROMOBOSS_INTERNAL"
        error.message = "Promo not found"

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/update")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun updatePromoWithValidationErrorFromPromoMicroService() {
        val requestDto = buildPromoDtoRequest()
            .promoId(PROMO_ID)

        val getPromoClientResponse = buildGetPromoClientResponse()

        doReturn(getPromoClientResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)
        doNothing().`when`(updatePromoValidator)
            ?.validate(TestUtils.any(Promo::class.java), TestUtils.any(Promo::class.java))
        doThrow(
            ValidationException(
                code = ExceptionCode.PROMO_PROMOCODE_CODE_CHANGE,
                message = "Some validation error from service"
            )
        ).`when`(promoApiClient)?.updatePromo(notNull())

        val error = ErrorResponse()
        error.errorCode = "PROMO_PROMOCODE_CODE_CHANGE"
        error.message = "Some validation error from service"
        error.errorFields = Collections.emptyList()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/update")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun updatePromoWithValidationException() {
        val requestDto = buildPromoDtoRequest()
            .promoId(PROMO_ID)
            .mechanicsType(null)

        val getPromoClientResponse = buildGetPromoClientResponse()

        doReturn(getPromoClientResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)
        doThrow(
            ValidationException(
                code = ExceptionCode.PROMO_REQUIRED_FIELD_EMPTY,
                message = "MechanicsType can't be changed",
                errorField = "MechanicsType"
            )
        ).`when`(updatePromoValidator)
            ?.validate(TestUtils.any(Promo::class.java), TestUtils.any(Promo::class.java))

        val error = ErrorResponse()
        error.errorCode = "PROMO_REQUIRED_FIELD_EMPTY"
        error.message = "MechanicsType can't be changed"
        error.errorFields = listOf("MechanicsType")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/promos/update")
                .contentType("application/json")
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    private fun buildUpdatePromoClientRequest(): PromoRequestV2 {
        val updatePromoRequest = PromoRequestV2()
        //region init updatePromoRequest
        updatePromoRequest
            .promoId(PROMO_ID)
            .modifiedBy("localDeveloper")
            .main(
                PromoMainRequestParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(false)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
                    .sourceType(SourceType.CATEGORYIFACE)
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(CheapestAsGift().count(2))
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author("petr")
                            .finalBudget(true)
                            .autoCompensation(false)
                            .mediaPlanS3Key(null)
                            .mediaPlanS3FileName(null)
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .piPublishedAt(piPublishDate)
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto().exclude(false).suppliers(listOf()))
            .warehousesConstraints(WarehousePromoConstraintDto().exclude(false).warehouses(listOf(11L)))
            .categoriesConstraints(CategoryPromoConstraintDto().categories(listOf()))
            .vendorsConstraints(VendorPromoConstraintDto().exclude(false).vendors(listOf()))
            .mskusConstraints(MskuPromoConstraintDto().exclude(false).mskus(listOf()))
        //endregion
        return updatePromoRequest
    }

    private fun buildUpdatePromoClientResponse(): PromoResponseV2 {
        val promoUpdateResponse = PromoResponseV2()
        //region init promoUpdateResponse
        promoUpdateResponse
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                    .parentPromoId("cf_000000000")
                    .name("Promo name")
                    .startAt(startAt)
                    .endAt(endAt)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("/rules/page/url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("/landing/page/url")
                            .auto(false)
                    )
                    .active(true)
                    .hidden(false)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(CheapestAsGift().count(2))
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("sergey321")
                            .departments(listOf("HEALTH", "FMCG"))
                            .streams(listOf("stream1", "stream2"))
                            .markom("dasha123")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author("petr")
                            .finalBudget(true)
                            .autoCompensation(false)
                            .mediaPlanS3Key(null)
                            .mediaPlanS3FileName(null)
                            .compensationReceiveMethods(
                                listOf(
                                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                                )
                            )
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto().exclude(false).suppliers(listOf()))
            .warehousesConstraints(WarehousePromoConstraintDto().exclude(false).warehouses(listOf(11L)))
            .categoriesConstraints(CategoryPromoConstraintDto().categories(listOf()))
            .vendorsConstraints(VendorPromoConstraintDto().exclude(false).vendors(listOf()))
            .mskusConstraints(MskuPromoConstraintDto().exclude(false).mskus(listOf()))
        //endregion
        return promoUpdateResponse
    }

    private fun buildGetPromoClientResponse(): PromoResponseV2 {
        val promoGetResponse = PromoResponseV2()
        //region init promoGetResponse
        promoGetResponse
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                    .parentPromoId("cf_000000000")
                    .name("Promo old name")
                    .startAt(OffsetDateTime.parse("2018-12-03T09:15:30Z").toEpochSecond())
                    .endAt(OffsetDateTime.parse("2019-12-03T09:15:30Z").toEpochSecond())
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("rulesUrl")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("landingUrl")
                            .auto(false)
                    )
                    .active(false)
                    .hidden(true)
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(CheapestAsGift().count(2))
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind("CROSS_CATEGORY")
                            .tradeManager("ivan")
                            .departments(listOf("HEALTH"))
                            .markom("sveta")
                            .purpose("GMV_GENERATION")
                            .budgetOwner("PRODUCT")
                            .supplierType("1P")
                            .compensationSource("PART_MARKET_PART_PARTNER")
                            .author("petr")
                            .finalBudget(true)
                            .autoCompensation(false)
                            .mediaPlanS3Key(null)
                            .mediaPlanS3FileName(null)
                            .compensationReceiveMethods(listOf(CompensationReceiveMethod.WITHOUT_COMPENSATION.name))
                            .compensationTicket("https://st.yandex-team.ru/QUEUE-123")
                            .assortmentLoadMethod("TRACKER")
                            .promotions(
                                listOf(
                                    Promotion()
                                        .id(111L.toString())
                                        .catteam("catteam")
                                        .category("category")
                                        .channel("channel")
                                        .count(123L)
                                        .countUnit("недели")
                                        .budgetPlan(124L)
                                        .budgetFact(125L)
                                        .isCustomBudgetPlan(true)
                                        .comment("comment")
                                )
                            )
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto().exclude(false).suppliers(listOf()))
            .categoriesConstraints(CategoryPromoConstraintDto().categories(listOf()))
            .vendorsConstraints(VendorPromoConstraintDto().exclude(false).vendors(listOf()))
            .mskusConstraints(MskuPromoConstraintDto().exclude(false).mskus(listOf()))
            .warehousesConstraints(WarehousePromoConstraintDto().exclude(false).warehouses(listOf(11L)))
        //endregion
        return promoGetResponse
    }

    @Test
    fun getSskuTemplateSuccessTest() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/cheapest-as-gift/template")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val columns = PromoSskuMetaData.getColumns()
        val sskus = excelHelper.read(excelData.inputStream(), columns, EXCEL_HEADER_ROW, EXCEL_START_ROW)
        Assertions.assertThat(sskus).isEmpty()
    }

    @Test
    fun getSskuTemplateForNotSupportedMechanics() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/flash/template")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.importSskus.before.csv"],
        after = ["PromoApiTest.importSskus.after.csv"]
    )
    fun importPromoSskuSuccessTest() {
        cheapestAsGiftPromoExistsStub(PROMO_ID)

        uploadPromoSskuFile(PROMO_ID, "/xlsx-template/promo_sskus.xlsx")
            .andExpect(MockMvcResultMatchers.status().isCreated)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.importSskus.before.csv"],
        after = ["PromoApiTest.importSskus.before.csv"]
    )
    fun importPromoSskuWithPromoServiceError() {
        cheapestAsGiftPromoExistsStub(PROMO_ID)

        doThrow(ApiErrorException("Promo service is not available.")).`when`(promoApiClient)?.updatePromo(notNull())

        uploadPromoSskuFile(PROMO_ID, "/xlsx-template/promo_sskus.xlsx")
            .andExpect(MockMvcResultMatchers.status().is5xxServerError)
    }

    @Test
    fun importItemsForNotSupportedMechanics() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(MechanicsType.BLUE_FLASH)
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        uploadPromoSskuFile(PROMO_ID, "/xlsx-template/promo_sskus.xlsx")
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.reimportSskus.before.csv"],
        after = ["PromoApiTest.importSskus.after.csv"]
    )
    fun reimportPromoSskuSuccessTest() {
        cheapestAsGiftPromoExistsStub(PROMO_ID)

        uploadPromoSskuFile(PROMO_ID, "/xlsx-template/promo_sskus.xlsx")
            .andExpect(MockMvcResultMatchers.status().isCreated)
    }

    @Test
    fun importSskuPromoNotFound() {
        promoDoesNotExistStub(PROMO_ID)
        uploadPromoSskuFile(PROMO_ID, "/xlsx-template/promo_sskus.xlsx")
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.importSskuValidationFailed.before.csv"],
        after = ["PromoApiTest.importSskuValidationFailed.after.csv"]
    )
    fun importPromoSskuValidationFailed() {
        cheapestAsGiftPromoExistsStub(PROMO_ID)

        uploadPromoSskuFile(PROMO_ID, "/xlsx-template/promo_sskus.xlsx")
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoApiTest.exportSskus.before.csv"]
    )
    fun exportPromoSskuSuccessTest() {
        cheapestAsGiftPromoExistsStub(PROMO_ID)

        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID/items")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val columns = PromoSskuMetaData.getColumns()
        val actualSskus: List<PriceExcelDTO> =
            excelHelper.read(excelData.inputStream(), columns, EXCEL_HEADER_ROW, EXCEL_START_ROW)

        val expectedSskus = priceRepository.getAllByPromoId(PROMO_ID)

        Assertions.assertThat(actualSskus).isEqualTo(expectedSskus)
    }

    @Test
    fun exportItemsForNotSupportedMechanics() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(MechanicsType.BLUE_FLASH)
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID/items")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun exportSskuPromoNotFound() {
        promoDoesNotExistStub(PROMO_ID)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID/items")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun getMediaPlanSuccessTest() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)
        doReturn(true).`when`(s3Client).doesObjectExist(bucketName, "mediaPlanS3Key")

        val s3Object = mock(S3Object::class.java)
        val s3ObjectInputStream = S3ObjectInputStream(javaClass.getResourceAsStream(s3TestFile), null)
        doReturn(s3Object).`when`(s3Client).getObject(bucketName, "mediaPlanS3Key")
        doReturn(s3ObjectInputStream).`when`(s3Object).objectContent

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID/media-plan")
                .contentType("application/json")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().string(TestUtils.readResourceFile(s3TestFile))
            )
    }

    @Test
    fun getMediaPlanPromoNotFoundTest() {
        promoDoesNotExistStub(PROMO_ID)

        val error = ErrorResponse()
        error.errorCode = "PROMOBOSS_INTERNAL"
        error.message = "Promo not found"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID/media-plan")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun getMediaPlanKeyDoesNotExistInPromoNotFoundTest() {
        val promoResponse = PromoResponseV2().promoId(PROMO_ID)

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        val error = ErrorResponse()
        error.errorCode = "PROMO_MEDIAPLAN_LINK_NOT_FOUND"
        error.message = "Ссылка на медиаплан не найдена. PromoId=$PROMO_ID"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID/media-plan")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun getMediaPlanS3ObjectDoesNotExistNotFoundTest() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)
        doReturn(false).`when`(s3Client).doesObjectExist(bucketName, "mediaPlanS3Key")

        val error = ErrorResponse()
        error.errorCode = "PROMO_MEDIAPLAN_NOT_FOUND"
        error.message = "Медиаплан не найден в S3. PromoId=$PROMO_ID"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID/media-plan")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun getMediaPlanS3ExceptionInternalServerError() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)
        doThrow(AmazonServiceException("Amazon error")).`when`(s3Client).doesObjectExist(any(), any())

        val error = ErrorResponse()
        error.errorCode = "PROMO_MEDIAPLAN_DOWNLOAD"
        error.message = "Amazon error (Service: null; Status Code: 0; Error Code: null; Request ID: null; Proxy: null)"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID/media-plan")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun uploadMediaPlanSuccessTest() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )

        val key = "$PROMO_ID/test.txt"

        val updatePromoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key(key)
                            .mediaPlanS3FileName("test.txt")
                    )
            )

        `when`(promoApiClient.getPromo(PROMO_ID)).thenReturn(promoResponse, updatePromoResponse)
        doNothing().`when`(promoApiClient)?.updatePromo(notNull())

        val bytes = javaClass.getResourceAsStream(s3TestFile)?.readAllBytes()
        val file = MockMultipartFile("mediaPlan", "test.txt", "multipart/form-data", bytes)
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/promos/$PROMO_ID/media-plan")
                .file(file)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.content().json("{\"fileName\":\"test.txt\"}"))

        val inOrder = inOrder(s3Client, promoApiClient)

        inOrder.verify(promoApiClient, times(2))?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())
        inOrder.verify(s3Client).putObject(eq(bucketName), contains(key), any(), any())

        verifyNoMoreInteractions(promoApiClient)
        verifyNoMoreInteractions(s3Client)
    }

    @Test
    fun uploadMediaPlanPromoNotFoundTest() {
        promoDoesNotExistStub(PROMO_ID)

        val error = ErrorResponse()
        error.errorCode = "PROMOBOSS_INTERNAL"
        error.message = "Promo not found"

        val bytes = javaClass.getResourceAsStream(s3TestFile)?.readAllBytes()
        val file = MockMultipartFile("mediaPlan", "test.txt", "multipart/form-data", bytes)
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/promos/$PROMO_ID/media-plan")
                .file(file)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun uploadMediaPlanWithDeleteOldSuccessTest() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        val key = "$PROMO_ID/test.txt"

        val updatePromoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key(key)
                            .mediaPlanS3FileName("test.txt")
                    )
            )

        `when`(promoApiClient.getPromo(PROMO_ID)).thenReturn(promoResponse, updatePromoResponse)
        doNothing().`when`(promoApiClient)?.updatePromo(notNull())

        val bytes = javaClass.getResourceAsStream(s3TestFile)?.readAllBytes()
        val file = MockMultipartFile("mediaPlan", "test.txt", "multipart/form-data", bytes)
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/promos/$PROMO_ID/media-plan")
                .file(file)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.content().json("{\"fileName\":\"test.txt\"}"))

        val inOrder = inOrder(s3Client, promoApiClient)

        inOrder.verify(promoApiClient, times(2))?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())
        inOrder.verify(s3Client).putObject(eq(bucketName), contains(key), any(), any())
        inOrder.verify(s3Client).deleteObject(bucketName, "mediaPlanS3Key")

        verifyNoMoreInteractions(promoApiClient)
        verifyNoMoreInteractions(s3Client)
    }

    @Test
    fun uploadMediaPlanS3ExceptionInternalServerError() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        val key = "$PROMO_ID/test.txt"

        val updatePromoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key(key)
                            .mediaPlanS3FileName("test.txt")
                    )
            )

        doNothing().`when`(promoApiClient)?.updatePromo(notNull())
        `when`(promoApiClient.getPromo(PROMO_ID)).thenReturn(promoResponse, updatePromoResponse)

        doThrow(AmazonServiceException("Amazon error"))
            .`when`(s3Client).putObject(eq(bucketName), contains(key), any(), any())

        val bytes = javaClass.getResourceAsStream(s3TestFile)?.readAllBytes()
        val file = MockMultipartFile("mediaPlan", "test.txt", "multipart/form-data", bytes)

        val error = ErrorResponse()
        error.errorCode = "PROMO_MEDIAPLAN_UPLOAD"
        error.message = "Amazon error (Service: null; Status Code: 0; Error Code: null; Request ID: null; Proxy: null)"

        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/promos/$PROMO_ID/media-plan")
                .file(file)
        ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )

        val inOrder = inOrder(s3Client, promoApiClient)

        inOrder.verify(promoApiClient, times(2))?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())
        inOrder.verify(s3Client).putObject(eq(bucketName), contains(key), any(), any())
        inOrder.verify(promoApiClient)?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())

        verifyNoMoreInteractions(promoApiClient)
        verifyNoMoreInteractions(s3Client)
    }

    @Test
    fun uploadMediaPlanPromoApiErrorInternalServerError() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        doThrow(ApiErrorException("Api client error")).`when`(promoApiClient)?.updatePromo(notNull())

        val error = ErrorResponse()
        error.message = "Api client error"

        val bytes = javaClass.getResourceAsStream(s3TestFile)?.readAllBytes()
        val file = MockMultipartFile("mediaPlan", "test.txt", "multipart/form-data", bytes)
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/promos/$PROMO_ID/media-plan")
                .file(file)
        ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )

        val inOrder = inOrder(s3Client, promoApiClient)

        inOrder.verify(promoApiClient, times(2))?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())

        verifyNoMoreInteractions(promoApiClient)
        verifyNoMoreInteractions(s3Client)
    }

    @Test
    fun uploadMediaPlanTooBigFileBadRequestExceptionFile() {

        val bigFile = RandomAccessFile("name", "rw")
        bigFile.setLength(10_500_000L)

        val bytes = Channels.newInputStream(bigFile.channel)?.readAllBytes()
        val file = MockMultipartFile("mediaPlan", "test.txt", "multipart/form-data", bytes)

        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/promos/$PROMO_ID/media-plan")
                .file(file)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Max file size exceeded. File size have to be less then 10 Mb")
            )

        verifyZeroInteractions(promoApiClient)
        verifyZeroInteractions(s3Client)
    }

    @Test
    fun deleteMediaPlanSuccessTest() {
        val promoResponse = PromoResponseV2()
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        doNothing().`when`(promoApiClient)?.updatePromo(notNull())

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/promos/$PROMO_ID/media-plan")
        ).andExpect(MockMvcResultMatchers.status().isNoContent)

        val inOrder = inOrder(s3Client, promoApiClient)

        inOrder.verify(promoApiClient, times(2))?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())
        inOrder.verify(s3Client).deleteObject(bucketName, "mediaPlanS3Key")

        verifyNoMoreInteractions(promoApiClient)
        verifyNoMoreInteractions(s3Client)
    }

    @Test
    fun deleteMediaPlanPromoWithoutS3InfoSuccessTest() {
        val promoResponse = PromoResponseV2().promoId(PROMO_ID)

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/promos/$PROMO_ID/media-plan")
        ).andExpect(MockMvcResultMatchers.status().isNoContent)

        verify(promoApiClient)?.getPromo(PROMO_ID)

        verifyNoMoreInteractions(promoApiClient)
        verifyZeroInteractions(s3Client)
    }

    @Test
    fun deleteMediaPlanPromoNotFoundTest() {
        promoDoesNotExistStub(PROMO_ID)

        val error = ErrorResponse()
        error.errorCode = "PROMOBOSS_INTERNAL"
        error.message = "Promo not found"

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/promos/$PROMO_ID/media-plan")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )

        verify(promoApiClient)?.getPromo(PROMO_ID)
        verifyNoMoreInteractions(promoApiClient)

        verifyZeroInteractions(s3Client)
    }

    @Test
    fun deleteMediaPlanS3ExceptionInternalServerError() {
        val promoResponse = PromoResponseV2()
        promoResponse
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        doNothing().`when`(promoApiClient).updatePromo(notNull())
        `when`(s3Client.deleteObject(bucketName, "mediaPlanS3Key")).thenThrow(AmazonServiceException("Amazon error"))

        val error = ErrorResponse()
        error.errorCode = "PROMO_MEDIAPLAN_DELETE"
        error.message = "Amazon error (Service: null; Status Code: 0; Error Code: null; Request ID: null; Proxy: null)"

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/promos/$PROMO_ID/media-plan")
        ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )

        val inOrder = inOrder(s3Client, promoApiClient)

        inOrder.verify(promoApiClient, times(2))?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())
        inOrder.verify(s3Client).deleteObject(bucketName, "mediaPlanS3Key")
        inOrder.verify(promoApiClient)?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())

        verifyNoMoreInteractions(promoApiClient)
        verifyNoMoreInteractions(s3Client)
    }

    @Test
    fun deleteMediaPlanPromoApiErrorInternalServerError() {
        val promoResponse = PromoResponseV2()
        promoResponse
            .promoId(PROMO_ID)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(PromoMechanicsType.PROMO_CODE.clientValue)
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .mediaPlanS3Key("mediaPlanS3Key")
                            .mediaPlanS3FileName("mediaPlanS3FileName")
                    )
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        doThrow(ApiErrorException("Api client error")).`when`(promoApiClient)?.updatePromo(notNull())

        val error = ErrorResponse()
        error.message = "Api client error"

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/promos/$PROMO_ID/media-plan")
        ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )

        val inOrder = inOrder(s3Client, promoApiClient)

        inOrder.verify(promoApiClient, times(2))?.getPromo(PROMO_ID)
        inOrder.verify(promoApiClient)?.updatePromo(notNull())

        verifyNoMoreInteractions(promoApiClient)
        verifyZeroInteractions(s3Client)
    }

    @Test
    fun searchPromo() {
        val searchResultsFromService = ru.yandex.mj.generated.client.promoservice.model.PromoSearchResult()
            .promos(
                listOf<PromoSearchResultItem>(
                    //region init searchResultsFromService
                    PromoSearchResultItem().promoId("promoId")
                        .mechanicsType(PromoMechanicsType.DIRECT_DISCOUNT.clientValue)
                        .parentPromoId("parentPromoId")
                        .name("name")
                        .status(PromoStatus.NEW.code)
                        .startAt(startAt)
                        .endAt(endAt)
                        .productsCount1p(100)
                        .productsCount3p(40)
                        .srcCiface(
                            PromoSearchResultItemSrcCiface()
                                .promoKind(PromoKind.CATEGORY.name)
                                .author("author")
                                .tradeManager("tradeManager")
                                .departments(listOf("department", "department1"))
                                .supplierType(SupplierType.FIRST_PARTY.value)
                                .compensationSource(Compensation.MARKET.name)
                                .promotionBudgetFact(500L)
                                .finalBudget(false)
                        )
                    //endregion
                )
            )
            .totalCount(10)
        val searchRequestToService = buildBasePromoSearchRequestDto()
            //region init searchRequestToService
            .pageSize(10)
            .pageNumber(1)
            .parentPromoId(listOf("parentPromoId", "parentPromoId1"))
            .mechanicsType(listOf(MechanicsType.CHEAPEST_AS_GIFT, MechanicsType.GENERIC_BUNDLE))
            .startAtFrom(startAtFrom)
            .startAtTo(startAtTo)
            .endAtFrom(endAtFrom)
            .endAtTo(endAtTo)
            .status(
                listOf(
                    ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW,
                    ru.yandex.mj.generated.client.promoservice.model.PromoStatus.READY,
                )
            )
            .srcCiface(
                PromoSearchRequestDtoV2SrcCiface()
                    .author(listOf("author", "author1"))
                    .promoKind(listOf("NATIONAL", "CROSS_CATEGORY"))
                    .department(listOf("department", "department1"))
                    .tradeManager(listOf("tradeManager", "tradeManager1"))
                    .supplierType(listOf("1P"))
                    .finalBudget(false)
            )
        //endregion
        doReturn(searchResultsFromService).`when`(promoApiClient)?.searchPromo(notNull())

        val expectedResponseDto = DashboardPromoDtoResponse()
            .promos(
                listOf(
                    DashboardPromoItemDtoResponse()
                        //region init expectedResponseDto
                        .promoId("promoId")
                        .promoKind(PromoKind.CATEGORY.displayName)
                        .mechanicsType(PromoMechanicsType.DIRECT_DISCOUNT.displayName)
                        .parentPromoId("parentPromoId")
                        .name("name")
                        .startDate(startAt)
                        .endDate(endAt)
                        .author("author")
                        .tradeManager("tradeManager")
                        .department("department")
                        .departments(listOf("department", "department1"))
                        .supplierType(SupplierType.FIRST_PARTY.displayName)
                        .compensationSource(Compensation.MARKET.displayName)
                        .status(PromoStatus.NEW.displayName)
                        .displayStatus(PromoDisplayStatus.NEW.displayName)
                        .budgetTotal(500)
                        .ssku1pCount(100)
                        .ssku3pCount(40)
                        .finalBudget(false)
                )
            ).totalCount(10)
        //endregion

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/search").contentType("application/json")
                .param("limit", "10")
                .param("page", "1")
                .param("author", "author", "author1")
                .param("promoKind", "NATIONAL", "CROSS_CATEGORY")
                .param("department", "department", "department1")
                .param("mechanicsType", "CHEAPEST_AS_GIFT", "GENERIC_BUNDLE")
                .param("parentPromoId", "parentPromoId", "parentPromoId1")
                .param("startDateFrom", startAtFrom.toString())
                .param("startDateTo", startAtTo.toString())
                .param("endDateFrom", endAtFrom.toString())
                .param("endDateTo", endAtTo.toString())
                .param("tradeManager", "tradeManager", "tradeManager1")
                .param("supplierType", "1P")
                .param("status", "NEW", "READY")
                .param("finalBudget", "false")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        verify(promoApiClient)?.searchPromo(searchRequestToService)
    }

    @Test
    fun searchPromoWithEmptyFilter() {
        val searchResultsFromService = PromoSearchResult().promos(
            listOf(
                //region init searchResultsFromService
                PromoSearchResultItem().promoId("promoId")
                    .mechanicsType(PromoMechanicsType.DIRECT_DISCOUNT.clientValue)
                    .parentPromoId("parentPromoId")
                    .name("name")
                    .status(PromoStatus.NEW.code)
                    .startAt(startAt)
                    .endAt(endAt)
                    .productsCount1p(100)
                    .productsCount3p(40)
                    .srcCiface(
                        PromoSearchResultItemSrcCiface()
                            .promoKind(PromoKind.CATEGORY.name)
                            .author("author")
                            .tradeManager("tradeManager")
                            .departments(listOf("department", "department1"))
                            .supplierType(SupplierType.FIRST_PARTY.value)
                            .compensationSource(Compensation.MARKET.name)
                            .promotionBudgetFact(500L)
                            .finalBudget(null)
                    )
                //endregion
            )
        ).totalCount(10)
        val searchRequestToService = buildBasePromoSearchRequestDto()
        doReturn(searchResultsFromService).`when`(promoApiClient)?.searchPromo(searchRequestToService)

        val expectedResponseDto = DashboardPromoDtoResponse().promos(
            listOf(
                DashboardPromoItemDtoResponse()
                    //region init expectedResponseDto
                    .promoId("promoId")
                    .promoKind(PromoKind.CATEGORY.displayName)
                    .mechanicsType(PromoMechanicsType.DIRECT_DISCOUNT.displayName)
                    .parentPromoId("parentPromoId")
                    .name("name")
                    .startDate(startAt)
                    .endDate(endAt)
                    .author("author")
                    .tradeManager("tradeManager")
                    .department("department")
                    .departments(listOf("department", "department1"))
                    .supplierType(SupplierType.FIRST_PARTY.displayName)
                    .compensationSource(Compensation.MARKET.displayName)
                    .status(PromoStatus.NEW.displayName)
                    .displayStatus(PromoDisplayStatus.NEW.displayName)
                    .budgetTotal(500)
                    .ssku1pCount(100)
                    .ssku3pCount(40)
                    .finalBudget(null)
            )
        ).totalCount(10)
        //endregion

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/search").contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        verify(promoApiClient)?.searchPromo(searchRequestToService)
    }

    @Test
    fun searchPromoWithEmptyResult() {
        val searchRequestToService = buildBasePromoSearchRequestDto()
        doReturn(PromoSearchResult()).`when`(promoApiClient)?.searchPromo(searchRequestToService)

        val expectedResponseDto = DashboardPromoDtoResponse().totalCount(0).promos(Collections.emptyList())
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/search").contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        verify(promoApiClient)?.searchPromo(searchRequestToService)
    }

    @Test
    fun searchPromoWithWrongRequest() {
        val error = ErrorResponse()
            .errorCode("PROMO_KIND_UNKNOWN")
            .message("Вид промо не распознан")
            .errorFields(Collections.emptyList())

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/search").contentType("application/json")
                .param("promoKind", "NATIONAL", "WRONG")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun exportSearchPromo() {
        val searchResultsFromService = PromoSearchResult().promos(
            listOf(
                //region init searchResultsFromService
                PromoSearchResultItem().promoId("promoId")
                    .mechanicsType(PromoMechanicsType.DIRECT_DISCOUNT.clientValue)
                    .parentPromoId("parentPromoId")
                    .name("name")
                    .status(PromoStatus.NEW.code)
                    .startAt(startAt)
                    .endAt(endAt)
                    .productsCount1p(100)
                    .productsCount3p(40)
                    .srcCiface(
                        PromoSearchResultItemSrcCiface()
                            .promoKind(PromoKind.CATEGORY.name)
                            .author("author")
                            .tradeManager("tradeManager")
                            .departments(listOf("department", "department1"))
                            .supplierType(SupplierType.FIRST_PARTY.value)
                            .compensationSource(Compensation.MARKET.name)
                            .promotionBudgetFact(500L)
                            .finalBudget(true)
                            .assortmentLoadMethod("PI")
                    )
                //endregion
            )
        ).totalCount(10)
        val searchRequestToService = buildBasePromoSearchRequestDto()
            //region init searchRequestToService
            .parentPromoId(listOf("parentPromoId", "parentPromoId1"))
            .mechanicsType(listOf(MechanicsType.CHEAPEST_AS_GIFT, MechanicsType.GENERIC_BUNDLE))
            .startAtFrom(startAtFrom)
            .startAtTo(startAtTo)
            .endAtFrom(endAtFrom)
            .endAtTo(endAtTo)
            .status(
                listOf(
                    ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW,
                    ru.yandex.mj.generated.client.promoservice.model.PromoStatus.READY
                )
            )
            .srcCiface(
                PromoSearchRequestDtoV2SrcCiface()
                    .author(listOf("author", "author1"))
                    .promoKind(listOf("NATIONAL", "CROSS_CATEGORY"))
                    .department(listOf("department", "department1"))
                    .tradeManager(listOf("tradeManager", "tradeManager1"))
                    .supplierType(listOf("1P"))
                    .finalBudget(true)
            )
        //endregion
        doReturn(searchResultsFromService).`when`(promoApiClient)?.searchPromo(notNull())

        val expectedResponseDto = listOf(
            PromoSearchResultDTO(
                //region init expectedResponseDto
                promoId = "promoId",
                promoKind = PromoKind.CATEGORY.displayName,
                mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT.displayName,
                parentPromoId = "parentPromoId",
                name = "name",
                startDate = startAtExcel,
                endDate = endAtExcel,
                tradeManager = "tradeManager",
                departments = "department, department1",
                supplierType = SupplierType.FIRST_PARTY.displayName,
                compensationSource = Compensation.MARKET.displayName,
                displayStatus = PromoDisplayStatus.NEW.displayName,
                budgetTotal = 500L,
                ssku1pCount = 100,
                ssku3pCount = 40,
                finalBudget = "да",
                assortmentLoadMethod = "ПИ"
            )
            //endregion
        )

        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/search/export").contentType("application/json")
                .param("author", "author", "author1")
                .param("promoKind", "NATIONAL", "CROSS_CATEGORY")
                .param("department", "department", "department1")
                .param("mechanicsType", "CHEAPEST_AS_GIFT", "GENERIC_BUNDLE")
                .param("parentPromoId", "parentPromoId", "parentPromoId1")
                .param("startDateFrom", startAtFrom.toString())
                .param("startDateTo", startAtTo.toString())
                .param("endDateFrom", endAtFrom.toString())
                .param("endDateTo", endAtTo.toString())
                .param("tradeManager", "tradeManager", "tradeManager1")
                .param("supplierType", "1P")
                .param("status", "NEW", "READY")
                .param("finalBudget", "true")
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsByteArray

        verify(promoApiClient)?.searchPromo(searchRequestToService)

        val actualResponseDto =
            promoSearchExcelHelper.read(excelData.inputStream(), PromoSearchResultMetaData.getColumns())
        Assertions.assertThat(actualResponseDto).isEqualTo(expectedResponseDto)
    }

    @Test
    fun exportSearchPromoWithEmptyFilter() {
        val searchResultsFromService = PromoSearchResult().promos(
            listOf(
                //region init searchResultsFromService
                PromoSearchResultItem().promoId("promoId")
                    .mechanicsType(PromoMechanicsType.DIRECT_DISCOUNT.clientValue)
                    .parentPromoId("parentPromoId")
                    .name("name")
                    .status(PromoStatus.NEW.code)
                    .startAt(startAt)
                    .endAt(endAt)
                    .productsCount1p(100)
                    .productsCount3p(40)
                    .srcCiface(
                        PromoSearchResultItemSrcCiface()
                            .promoKind(PromoKind.CATEGORY.name)
                            .author("author")
                            .tradeManager("tradeManager")
                            .departments(listOf("department", "department1"))
                            .supplierType(SupplierType.FIRST_PARTY.value)
                            .compensationSource(Compensation.MARKET.name)
                            .promotionBudgetFact(500L)
                            .finalBudget(null)
                            .assortmentLoadMethod("TRACKER")
                    )
                //endregion
            )
        ).totalCount(10)
        val searchRequestToService = buildBasePromoSearchRequestDto()
        doReturn(searchResultsFromService).`when`(promoApiClient)?.searchPromo(searchRequestToService)

        val expectedResponseDto = listOf(
            PromoSearchResultDTO(
                //region init expectedResponseDto
                promoId = "promoId",
                promoKind = PromoKind.CATEGORY.displayName,
                mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT.displayName,
                parentPromoId = "parentPromoId",
                name = "name",
                startDate = startAtExcel,
                endDate = endAtExcel,
                tradeManager = "tradeManager",
                departments = "department, department1",
                supplierType = SupplierType.FIRST_PARTY.displayName,
                compensationSource = Compensation.MARKET.displayName,
                displayStatus = PromoDisplayStatus.NEW.displayName,
                budgetTotal = 500L,
                ssku1pCount = 100,
                ssku3pCount = 40,
                finalBudget = null,
                assortmentLoadMethod = "Трекер"
            )
            //endregion
        )

        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/search/export").contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsByteArray

        verify(promoApiClient)?.searchPromo(searchRequestToService)

        val actualResponseDto =
            promoSearchExcelHelper.read(excelData.inputStream(), PromoSearchResultMetaData.getColumns())
        Assertions.assertThat(actualResponseDto).isEqualTo(expectedResponseDto)
    }

    @Test
    fun exportSearchPromoWithEmptyResult() {
        val searchRequestToService = buildBasePromoSearchRequestDto()
        doReturn(PromoSearchResult()).`when`(promoApiClient)?.searchPromo(searchRequestToService)

        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/search/export").contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsByteArray

        verify(promoApiClient)?.searchPromo(searchRequestToService)

        val actualResponseDto =
            promoSearchExcelHelper.read(excelData.inputStream(), PromoSearchResultMetaData.getColumns())
        Assertions.assertThat(actualResponseDto.size).isEqualTo(0)
    }

    private fun buildBasePromoSearchRequestDto() =
        PromoSearchRequestDtoV2()
            .srcCiface(PromoSearchRequestDtoV2SrcCiface())
            .sourceType(listOf(SourceType.CATEGORYIFACE))
            .sort(
                listOf(
                    PromoSearchRequestDtoV2Sort()
                        .field(PromoSearchRequestDtoV2Sort.FieldEnum.PROMOID)
                        .direction(PromoSearchRequestDtoV2Sort.DirectionEnum.DESC)
                )
            )

    @Test
    fun exportSearchPromoWithWrongRequest() {
        val error = ErrorResponse()
            .errorCode("PROMO_KIND_UNKNOWN")
            .message("Вид промо не распознан")
            .errorFields(Collections.emptyList())

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/search/export").contentType("application/json")
                .param("promoKind", "NATIONAL", "WRONG")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest).andExpect(
            MockMvcResultMatchers.content()
                .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
        )
    }

    private fun getDownloadCommonRestrictionTemplateArguments(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestrictionType.MSKU.type,
                PromoMechanicsType.CHEAPEST_AS_GIFT.name,
                AssortmentLoadMethod.PI.name,
                RestrictionMskuMetaData.getColumns()
            ),
            Arguments.of(
                RestrictionType.VENDOR.type,
                PromoMechanicsType.COIN.name,
                AssortmentLoadMethod.TRACKER.name,
                RestrictionVendorMetaData.getColumns()
            ),
            Arguments.of(
                RestrictionType.PARTNER.type,
                PromoMechanicsType.SPREAD_DISCOUNT_COUNT.name,
                AssortmentLoadMethod.LOYALTY.name,
                RestrictionPartnerMetaData.getColumns()
            ),
            Arguments.of(
                RestrictionType.CATEGORY.type,
                PromoMechanicsType.CHEAPEST_AS_GIFT.name,
                null,
                RestrictionCategoryMetaData.getColumns()
            )
        )
    }

    @ParameterizedTest
    @MethodSource("getDownloadCommonRestrictionTemplateArguments")
    fun downloadCommonRestrictionTemplate(
        type: String,
        mechanics: String,
        assortmentLoadMethod: String?,
        headers: List<ColumnMetaData<Restriction>>
    ) {
        val template = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/restrictions/template")
                .param("type", type)
                .param("mechanics", mechanics)
                .param("assortmentLoadMethod", assortmentLoadMethod)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val values = restrictionExcelHelper.read(template.inputStream(), headers)
        Assertions.assertThat(values).isEmpty()
    }

    private fun getCategoryDiscountRestrictionArguments(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                PromoMechanicsType.DIRECT_DISCOUNT.name,
                null
            ),
            Arguments.of(
                PromoMechanicsType.BLUE_FLASH.name,
                AssortmentLoadMethod.PI.name,
            )
        )
    }

    @ParameterizedTest
    @MethodSource("getCategoryDiscountRestrictionArguments")
    fun downloadCategoryDiscountRestrictionTemplate(
        mechanics: String,
        assortmentLoadMethod: String?
    ) {
        val template = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/restrictions/template")
                .param("type", "category")
                .param("mechanics", mechanics)
                .param("assortmentLoadMethod", assortmentLoadMethod)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray


        val values = categoryExcelHelper.read(template.inputStream(), RestrictionCategoryDiscountMetaData.getColumns())
        Assertions.assertThat(values).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("getRestrictionsWithWrongParamArguments")
    @DbUnitDataSet(before = ["PromoApiTest.uploadMskuRestrictions.csv"])
    fun downloadRestrictionTemplateWrongType(
        restrictionType: String,
        promoMechanicsType: String,
        assortmentLoadMethod: String?,
        expectedErrorMessage: String,
        expectedErrorCode: String
    ) {
        val error = ErrorResponse()
            .errorCode(expectedErrorCode)
            .message(expectedErrorMessage)
            .errorFields(emptyList())

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/restrictions/template")
                .param("type", restrictionType)
                .param("mechanics", promoMechanicsType)
                .param("assortmentLoadMethod", assortmentLoadMethod)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    fun downloadRestrictionTemplateNoTypeParam() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/restrictions/template")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(before = ["PromoApiTest.uploadMskuRestrictionsWrongData.after.csv"])
    fun downloadRestrictionImportingErrors() {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/restrictions/validate/errors/excel")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = ExcelImportingErrorMetaData.getColumnMetaData()
        val actual = excelErrorsHelper.read(excelData.inputStream(), headers)
        val expected = listOf(
            ExcelImportingError(rowNumber = 6, message = "Неверный формат значения: wrongValue. Ожидается целое число"),
            ExcelImportingError(message = "MSKU не найдены: 11111111,454091016")
        )
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    private fun getRestrictionsWithWrongParamArguments(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                "wrongRestrictionType",
                PromoMechanicsType.PROMO_CODE.name,
                AssortmentLoadMethod.PI.name,
                "Тип ограничения wrongRestrictionType не распознан",
                "PROMO_RESTRICTION_TYPE_NOT_FOUND"
            ),
            Arguments.of(
                RestrictionType.VENDOR.type,
                "wrongPromoMechanicsType",
                AssortmentLoadMethod.PI.name,
                "Тип механики не распознан",
                "PROMO_MECHANICS_TYPE_UNKNOWN"
            ),
            Arguments.of(
                RestrictionType.VENDOR.type,
                PromoMechanicsType.PROMO_CODE.name,
                "wrongAssortmentLoadMethod",
                "Способ загрузки ассортимента не распознан",
                "PROMO_ASSORTMENT_LOAD_METHOD_UNKNOWN"
            )
        )
    }

    @ParameterizedTest
    @MethodSource("getRestrictionsWithWrongParamArguments")
    @DbUnitDataSet(before = ["PromoApiTest.uploadMskuRestrictions.csv"])
    fun uploadRestrictionsWithWrongParam(
        restrictionType: String,
        promoMechanicsType: String,
        assortmentLoadMethod: String?,
        expectedErrorMessage: String,
        expectedErrorCode: String
    ) {
        val error = ErrorResponse()
            .errorCode(expectedErrorCode)
            .message(expectedErrorMessage)
            .errorFields(emptyList())

        uploadRestrictionFile(
            filename = "/xlsx-template/restrictions/msku_restrictions.xlsx",
            type = restrictionType,
            mechanics = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    private fun getUploadCommonRestrictionsSuccessArguments(): Stream<Arguments> {
        return Stream.concat(
            getUploadCategoryRestrictionsSuccessArguments(),
            getUploadCategoryDiscountRestrictionsSuccessArguments()
        )
    }

    private fun getUploadCategoryRestrictionsSuccessArguments(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(PromoMechanicsType.SPREAD_DISCOUNT_COUNT.name, AssortmentLoadMethod.TRACKER.name),
            Arguments.of(PromoMechanicsType.CHEAPEST_AS_GIFT.name, null)
        )
    }

    private fun getUploadCategoryDiscountRestrictionsSuccessArguments(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(PromoMechanicsType.DIRECT_DISCOUNT.name, AssortmentLoadMethod.LOYALTY.name),
            Arguments.of(PromoMechanicsType.BLUE_FLASH.name, AssortmentLoadMethod.PI.name)
        )
    }

    private fun uploadRestrictionsSuccess(
        type: String,
        filename: String,
        promoMechanicsType: String,
        assortmentLoadMethod: String?,
        expectedResponse: List<PromoRestrictionItemResponseDto>
    ) {
        uploadRestrictionFile(
            filename = filename,
            type = type,
            mechanics = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponse))
            )
    }

    @ParameterizedTest
    @MethodSource("getUploadCommonRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadMskuRestrictions.csv"],
        after = ["PromoApiTest.uploadMskuRestrictions.csv"]
    )
    fun uploadMskuRestrictionsSuccess(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        val expectedResponse = listOf(
            PromoRestrictionItemResponseDto()
                .id(434077179)
                .name("Вилы GARDENA ErgoLine 17013-20 (117 см)"),
            PromoRestrictionItemResponseDto()
                .id(434081377)
                .name("Вилы GARDENA NatureLine 17002-20"),
            PromoRestrictionItemResponseDto()
                .id(454091016)
                .name("Вилы FISKARS 1019603 (113 см)")
        )

        uploadRestrictionsSuccess(
            filename = "/xlsx-template/restrictions/msku_restrictions.xlsx",
            type = "msku",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod,
            expectedResponse = expectedResponse
        )
    }

    @ParameterizedTest
    @MethodSource("getUploadCommonRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadVendorRestrictions.csv"],
        after = ["PromoApiTest.uploadVendorRestrictions.csv"]
    )
    fun uploadVendorRestrictionsSuccess(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        val expectedResponse = listOf(
            PromoRestrictionItemResponseDto()
                .id(1)
                .name("ВАЗ"),
            PromoRestrictionItemResponseDto()
                .id(2)
                .name("АЗЛК"),
            PromoRestrictionItemResponseDto()
                .id(3)
                .name("УАЗ")
        )

        uploadRestrictionsSuccess(
            filename = "/xlsx-template/restrictions/vendor_restrictions.xlsx",
            type = "vendor",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod,
            expectedResponse = expectedResponse
        )
    }

    @ParameterizedTest
    @MethodSource("getUploadCommonRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadPartnerRestrictions.csv"],
        after = ["PromoApiTest.uploadPartnerRestrictions.csv"]
    )
    fun uploadPartnerRestrictionsSuccess(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        val expectedResponse = listOf(
            PromoRestrictionItemResponseDto()
                .id(1)
                .name("ООО Рога"),
            PromoRestrictionItemResponseDto()
                .id(2)
                .name("ООО Копыта"),
            PromoRestrictionItemResponseDto()
                .id(3)
                .name("ООО Рога и копыта")
        )

        uploadRestrictionsSuccess(
            filename = "/xlsx-template/restrictions/partner_restrictions.xlsx",
            type = "partner",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod,
            expectedResponse = expectedResponse
        )
    }

    @ParameterizedTest
    @MethodSource("getUploadCategoryRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadCategoryRestrictions.csv"],
        after = ["PromoApiTest.uploadCategoryRestrictions.csv"]
    )
    fun uploadCategoryRestrictionsSuccess(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        val expectedResponse = listOf(
            PromoRestrictionItemResponseDto()
                .id(1)
                .name("Авто"),
            PromoRestrictionItemResponseDto()
                .id(2)
                .name("Мото"),
            PromoRestrictionItemResponseDto()
                .id(3)
                .name("Тракторы")
        )

        uploadRestrictionsSuccess(
            filename = "/xlsx-template/restrictions/category_restrictions.xlsx",
            type = "category",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod,
            expectedResponse = expectedResponse
        )
    }

    @ParameterizedTest
    @MethodSource("getUploadCategoryDiscountRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadCategoryDiscountRestrictions.csv"],
        after = ["PromoApiTest.uploadCategoryDiscountRestrictions.csv"]
    )
    fun uploadCategoryDiscountRestrictionsSuccess(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        val expectedResponse = listOf(
            PromoRestrictionItemResponseDto()
                .id(1)
                .percent(10)
                .name("Авто"),
            PromoRestrictionItemResponseDto()
                .id(2)
                .percent(20)
                .name("Мото"),
            PromoRestrictionItemResponseDto()
                .id(3)
                .percent(30)
                .name("Тракторы")
        )

        uploadRestrictionsSuccess(
            filename = "/xlsx-template/restrictions/category_discount_restrictions.xlsx",
            type = "category",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod,
            expectedResponse = expectedResponse
        )
    }

    private fun uploadRestrictionsWrongData(
        type: String,
        filename: String,
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        uploadRestrictionFile(
            filename = filename,
            type = type,
            mechanics = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("{\"message\":\"В загружаемом листе с ограничениями содержатся ошибки\",\"excelLink\":\"/api/v1/promos/restrictions/validate/errors/excel\"}")
            )
    }

    @ParameterizedTest
    @MethodSource("getUploadCommonRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadMskuRestrictionsWrongData.before.csv"],
        after = ["PromoApiTest.uploadMskuRestrictionsWrongData.after.csv"]
    )
    fun uploadMskuRestrictionsWrongData(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        uploadRestrictionsWrongData(
            filename = "/xlsx-template/restrictions/msku_restrictions_with_wrong.xlsx",
            type = "msku",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod
        )
    }

    @ParameterizedTest
    @MethodSource("getUploadCommonRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadVendorRestrictionsWrongData.before.csv"],
        after = ["PromoApiTest.uploadVendorRestrictionsWrongData.after.csv"]
    )
    fun uploadVendorRestrictionsWrongData(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        uploadRestrictionsWrongData(
            filename = "/xlsx-template/restrictions/vendor_restrictions_with_wrong.xlsx",
            type = "vendor",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod
        )
    }

    @ParameterizedTest
    @MethodSource("getUploadCommonRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadPartnerRestrictionsWrongData.before.csv"],
        after = ["PromoApiTest.uploadPartnerRestrictionsWrongData.after.csv"]
    )
    fun uploadPartnerRestrictionsWrongData(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        uploadRestrictionsWrongData(
            filename = "/xlsx-template/restrictions/partner_restrictions_with_wrong.xlsx",
            type = "partner",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod
        )
    }

    @ParameterizedTest
    @MethodSource("getUploadCategoryRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadCategoryRestrictionsWrongData.before.csv"],
        after = ["PromoApiTest.uploadCategoryRestrictionsWrongData.after.csv"]
    )
    fun uploadCategoryRestrictionsWrongData(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        uploadRestrictionsWrongData(
            filename = "/xlsx-template/restrictions/category_restrictions_with_wrong.xlsx",
            type = "category",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod
        )
    }

    @ParameterizedTest
    @MethodSource("getUploadCategoryDiscountRestrictionsSuccessArguments")
    @DbUnitDataSet(
        before = ["PromoApiTest.uploadCategoryDiscountRestrictionsWrongData.before.csv"],
        after = ["PromoApiTest.uploadCategoryDiscountRestrictionsWrongData.after.csv"]
    )
    fun uploadCategoryDiscountRestrictionsWrongData(
        promoMechanicsType: String,
        assortmentLoadMethod: String?
    ) {
        uploadRestrictionsWrongData(
            filename = "/xlsx-template/restrictions/category_discount_restrictions_with_wrong.xlsx",
            type = "category",
            promoMechanicsType = promoMechanicsType,
            assortmentLoadMethod = assortmentLoadMethod
        )
    }

    private fun getDownloadCommonRestrictionsNotEmptySuccessArguments(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestrictionType.MSKU.type,
                PromoMechanicsType.CASHBACK.name,
                AssortmentLoadMethod.PI.name,
                RestrictionMskuMetaData.getColumns(),
                listOf(
                    Restriction(id = "41", name = "msku_41"),
                    Restriction(id = "42", name = "msku_42"),
                    Restriction(id = "43", name = null)
                ),
                listOf(
                    PromoRestrictionItemRequestDto().id(41),
                    PromoRestrictionItemRequestDto().id(42),
                    PromoRestrictionItemRequestDto().id(43)
                )
            ),
            Arguments.of(
                RestrictionType.VENDOR.type,
                PromoMechanicsType.BLUE_SET.name,
                null,
                RestrictionVendorMetaData.getColumns(),
                listOf(
                    Restriction(id = "31", name = "vendor_31"),
                    Restriction(id = "32", name = "vendor_32"),
                    Restriction(id = "33", name = null)
                ),
                listOf(
                    PromoRestrictionItemRequestDto().id(31),
                    PromoRestrictionItemRequestDto().id(32),
                    PromoRestrictionItemRequestDto().id(33)
                )
            ),
            Arguments.of(
                RestrictionType.PARTNER.type,
                PromoMechanicsType.PROMO_CODE.name,
                AssortmentLoadMethod.LOYALTY.name,
                RestrictionPartnerMetaData.getColumns(),
                listOf(
                    Restriction(id = "1", name = "supplier_3p_1"),
                    Restriction(id = "2", name = "supplier_3p_2"),
                    Restriction(id = "3", name = null)
                ),
                listOf(
                    PromoRestrictionItemRequestDto().id(1),
                    PromoRestrictionItemRequestDto().id(2),
                    PromoRestrictionItemRequestDto().id(3)
                )
            ),
            Arguments.of(
                RestrictionType.CATEGORY.type,
                PromoMechanicsType.COIN.name,
                AssortmentLoadMethod.TRACKER.name,
                RestrictionCategoryMetaData.getColumns(),
                listOf(
                    Restriction(id = "21", name = "category_21"),
                    Restriction(id = "22", name = "category_22"),
                    Restriction(id = "23", name = null)
                ),
                listOf(
                    PromoRestrictionItemRequestDto().id(21),
                    PromoRestrictionItemRequestDto().id(22),
                    PromoRestrictionItemRequestDto().id(23)
                )
            ),
            Arguments.of(
                RestrictionType.WAREHOUSE.type,
                PromoMechanicsType.CHEAPEST_AS_GIFT.name,
                null,
                RestrictionWarehouseMetaData.getColumns(),
                listOf(
                    Restriction(id = "21", name = "warehouse_21"),
                    Restriction(id = "22", name = "warehouse_22"),
                    Restriction(id = "23", name = null)
                ),
                listOf(
                    PromoRestrictionItemRequestDto().id(21),
                    PromoRestrictionItemRequestDto().id(22),
                    PromoRestrictionItemRequestDto().id(23)
                )
            )
        )
    }

    @ParameterizedTest
    @MethodSource("getDownloadCommonRestrictionsNotEmptySuccessArguments")
    @DbUnitDataSet(before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"])
    fun downloadCommonRestrictionsNotEmptySuccess(
        type: String,
        mechanics: String,
        assortmentLoadMethod: String?,
        headers: List<ColumnMetaData<Restriction>>,
        expected: List<Restriction>,
        requestDto: List<PromoRestrictionItemRequestDto>
    ) {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/promos/restrictions/excel")
                .contentType("application/json")
                .param("type", type)
                .param("mechanics", mechanics)
                .param("assortmentLoadMethod", assortmentLoadMethod)
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val actual = restrictionExcelHelper.read(excelData.inputStream(), headers)
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("getCategoryDiscountRestrictionArguments")
    @DbUnitDataSet(before = ["PromoApiTest.createOrUpdateWithRestrictionsBefore.csv"])
    fun downloadCategoryDiscountRestrictionsNotEmptySuccess(
        mechanics: String,
        assortmentLoadMethod: String?
    ) {
        val requestDto = listOf(
            PromoRestrictionItemRequestDto().id(21).percent(21),
            PromoRestrictionItemRequestDto().id(22).percent(22),
            PromoRestrictionItemRequestDto().id(23).percent(23)
        )

        val expected = listOf(
            CategoryDiscountRestriction(id = "21", name = "category_21", percent = "21"),
            CategoryDiscountRestriction(id = "22", name = "category_22", percent = "22"),
            CategoryDiscountRestriction(id = "23", name = null, percent = "23")
        )

        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/promos/restrictions/excel")
                .contentType("application/json")
                .param("type", "category")
                .param("mechanics", mechanics)
                .param("assortmentLoadMethod", assortmentLoadMethod)
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val actual = categoryExcelHelper.read(excelData.inputStream(), RestrictionCategoryDiscountMetaData.getColumns())
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("getDownloadCommonRestrictionsNotEmptySuccessArguments")
    fun downloadRestrictionsEmptySuccess(
        type: String,
        mechanics: String,
        assortmentLoadMethod: String?,
        headers: List<ColumnMetaData<Restriction>>
    ) {
        val requestDto = emptyList<PromoRestrictionItemRequestDto>()

        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/promos/restrictions/excel")
                .contentType("application/json")
                .param("type", type)
                .param("mechanics", mechanics)
                .param("assortmentLoadMethod", assortmentLoadMethod)
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val actual = restrictionExcelHelper.read(excelData.inputStream(), headers)
        val expected = emptyList<Restriction>()
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("getRestrictionsWithWrongParamArguments")
    @DbUnitDataSet(before = ["PromoApiTest.uploadMskuRestrictions.csv"])
    fun downloadRestrictionsNotEmptyWrongParam(
        restrictionType: String,
        promoMechanicsType: String,
        assortmentLoadMethod: String?,
        expectedErrorMessage: String,
        expectedErrorCode: String
    ) {
        val requestDto = listOf(
            PromoRestrictionItemRequestDto().id(1),
            PromoRestrictionItemRequestDto().id(2),
            PromoRestrictionItemRequestDto().id(3)
        )

        val error = ErrorResponse()
            .errorCode(expectedErrorCode)
            .message(expectedErrorMessage)
            .errorFields(emptyList())

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/promos/restrictions/excel")
                .contentType("application/json")
                .param("type", restrictionType)
                .param("mechanics", promoMechanicsType)
                .param("assortmentLoadMethod", assortmentLoadMethod)
                .content(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(requestDto))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    private fun uploadRestrictionFile(
        filename: String,
        type: String,
        mechanics: String,
        assortmentLoadMethod: String?
    ): ResultActions {
        val bytes = javaClass.getResourceAsStream(filename)?.readAllBytes()
        val file = MockMultipartFile("excelFile", filename, "application/vnd.ms-excel", bytes)
        return mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/promos/restrictions/validate")
                .file(file)
                .param("type", type)
                .param("mechanics", mechanics)
                .param("assortmentLoadMethod", assortmentLoadMethod)
        )
    }

    private fun uploadPromoSskuFile(promoId: String, filename: String): ResultActions {
        val bytes = javaClass.getResourceAsStream(filename)?.readAllBytes()
        val file = MockMultipartFile("excelFile", filename, "application/vnd.ms-excel", bytes)
        return mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/promos/$promoId/items").file(file))
    }

    private fun cheapestAsGiftPromoExistsStub(promoId: String) {
        val promoResponse = PromoResponseV2()
            .promoId(promoId)
            .main(
                PromoMainResponseParams()
                    .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(promoId)
    }

    private fun promoDoesNotExistStub(promoId: String) {
        doThrow(
            NotFoundException(
                code = ExceptionCode.PROMOBOSS_INTERNAL,
                message = "Promo not found"
            )
        ).`when`(promoApiClient)?.getPromo(promoId)
    }

    class ValidateMskuArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(
                    123L, MskuValidateDtoResponse()
                        .result(false)
                        .errorMessage("MSKU 123 not found")
                ),
                Arguments.of(
                    1L, MskuValidateDtoResponse()
                        .result(true)
                        .msku(
                            MskuValidateDtoResponseMsku()
                                .id(1)
                                .name("title01")
                        )
                )
            )
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = ValidateMskuArgumentsProvider::class)
    @DbUnitDataSet(
        before = ["PromoApiTest.validateMsku.before.csv"]
    )
    fun validateMsku(msku: Long?, response: MskuValidateDtoResponse) {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/msku/$msku/validate")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(response))
            )
    }

    private fun buildSecurityTestPerform(): ResultActions {
        val promoResponse = PromoResponseV2()
            .main(
                PromoMainResponseParams()
                    .active(true)
            )

        doReturn(promoResponse).`when`(promoApiClient)?.getPromo(PROMO_ID)

        return mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/$PROMO_ID")
        )
    }

    private fun securityTest_throw() {
        val error = ErrorResponse()
        error.message = "Нет доступа к интерфейсу управления ценами"
        error.rolesMissing = listOf(
            IdmRoleDto()
                .name("ROLE_PRICING_MGMT_ACCESS")
                .description("Доступ к интерфейсу управления ценами"),
            IdmRoleDto()
                .name("ROLE_PROMO_USER")
                .description("Промо - пользователь")
        )

        buildSecurityTestPerform()
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(error))
            )
    }

    @Test
    @WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = ["PRICING_MGMT_ACCESS", "PROMO_USER"])
    fun securityTest_ok() {
        buildSecurityTestPerform()
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @WithAnonymousUser
    @DbUnitDataSet(before = ["PromoApiTest.roles.csv"])
    fun securityTest_no_user_throw() {
        buildSecurityTestPerform()
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("")
            )
    }

    @Test
    @WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = ["PRICING_MGMT_ACCESS"])
    @DbUnitDataSet(before = ["PromoApiTest.roles.csv"])
    fun securityTest_no_PROMO_USER_throw() {
        securityTest_throw()
    }

    @Test
    @WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = ["PROMO_USER"])
    @DbUnitDataSet(before = ["PromoApiTest.roles.csv"])
    fun securityTest_no_PRICING_MGMT_ACCESS_throw() {
        securityTest_throw()
    }

    @Test
    @WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = [])
    @DbUnitDataSet(before = ["PromoApiTest.roles.csv"])
    fun securityTest_no_both_throw() {
        securityTest_throw()
    }

    @Test
    @WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = [])
    fun securityTest_rolesNotFound_throw() {
        val nestedServletException = assertThrows<NestedServletException> { buildSecurityTestPerform() }
        val expected = "Roles not found in database: ROLE_PRICING_MGMT_ACCESS, ROLE_PROMO_USER"
        org.junit.jupiter.api.Assertions.assertEquals(expected, nestedServletException.cause?.message)
    }
}
