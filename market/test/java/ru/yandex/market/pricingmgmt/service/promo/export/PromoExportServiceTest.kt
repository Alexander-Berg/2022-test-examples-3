package ru.yandex.market.pricingmgmt.service.promo.export

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.TestUtils.any
import ru.yandex.market.pricingmgmt.client.promo.api.PromoApiClient
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoPromotion
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import ru.yandex.market.pricingmgmt.model.promo.restrictions.PromoCategoryRestrictionItem
import ru.yandex.market.pricingmgmt.service.EnvironmentService
import ru.yandex.market.pricingmgmt.service.promo.PromoService
import ru.yandex.market.pricingmgmt.service.promo.export.dto.CategoryRestrictionDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.ErrorResponse
import ru.yandex.market.pricingmgmt.service.promo.export.dto.MskuRestrictionDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.OriginalBrandRestrictionDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.OriginalCategoryRestrictionDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PiPromoMechanicDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoAdditionalInfoDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoBrandDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoCategoryDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoChannelsDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoConstraintsDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoDescriptionRequestDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoResponsibleDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoStatusDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.Response
import ru.yandex.market.pricingmgmt.service.promo.export.dto.SupplierRestrictionDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.WarehouseRestrictionDto
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil.createOffsetDateTime
import ru.yandex.mj.generated.client.promoservice.model.*
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

class PromoExportServiceTest : AbstractFunctionalTest() {
    @MockBean
    private var promoApiClient: PromoApiClient? = null

    @MockBean
    private var promoService: PromoService? = null

    @MockBean
    private var promoB2BClient: PromoB2BClient? = null

    @Autowired
    private var environmentService: EnvironmentService? = null

    @Autowired
    private var promoExportService: PromoExportService? = null

    companion object {
        private const val PROMO_ID = "promoId"
        private val SEARCH_RESULT = ru.yandex.mj.generated.client.promoservice.model.PromoSearchResult().promos(
            listOf(
                //region init searchResultsFromService
                ru.yandex.mj.generated.client.promoservice.model.PromoSearchResultItem()
                    .promoId(PROMO_ID)
                    .mechanicsType(PromoMechanicsType.DIRECT_DISCOUNT.clientValue)
                    .parentPromoId("parentPromoId")
                    .name("name")
                    .status(PromoStatus.NEW.code)
                    .startAt(createOffsetDateTime(2017, 12, 3, 9, 15, 30).toEpochSecond())
                    .endAt(createOffsetDateTime(2018, 12, 3, 9, 15, 30).toEpochSecond())
                    .updatedAt(createOffsetDateTime(2017, 11, 3, 9, 15, 30).toEpochSecond())
                    .productsCount1p(100)
                    .productsCount3p(40)
                    .active(true)
                    .srcCiface(
                        PromoSearchResultItemSrcCiface()
                            .promoKind(PromoKind.CATEGORY.name)
                            .author("author")
                            .tradeManager("tradeManager")
                            .supplierType(SupplierType.FIRST_PARTY.value)
                            .compensationSource(Compensation.MARKET.name)
                            .promotionBudgetFact(500L)
                    )
                //endregion
            )
        ).totalCount(10)

        private val PROMO = Promo(
            promoId = PROMO_ID,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            assortmentLoadMethod = AssortmentLoadMethod.PI
        )
    }

    @AfterEach
    fun tearDown() {
        clearInvocations(promoService)
        clearInvocations(promoB2BClient)
        clearInvocations(promoApiClient)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.before.csv"],
        after = ["PromoExportServiceTest.successCreate.after.csv"]
    )
    fun successCreate() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(Response(httpStatus = 200)).`when`(promoB2BClient)
            ?.createPromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        verify(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        after = ["PromoExportServiceTest.successCreate.after.csv"]
    )
    fun successFirstExport() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(Response(httpStatus = 200)).`when`(promoB2BClient)
            ?.createPromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        val expectedSearchRequest = PromoSearchRequestDtoV2()
            //region expectedSearchRequest init
            .mechanicsType(
                listOf(
                    MechanicsType.PROMO_CODE,
                    MechanicsType.CHEAPEST_AS_GIFT,
                    MechanicsType.BLUE_FLASH,
                    MechanicsType.DIRECT_DISCOUNT
                )
            )
            .sourceType(listOf(SourceType.CATEGORYIFACE))
            .updatedAtFrom(0)
            .sort(
                listOf(
                    PromoSearchRequestDtoV2Sort().field(PromoSearchRequestDtoV2Sort.FieldEnum.UPDATEDAT)
                        .direction(PromoSearchRequestDtoV2Sort.DirectionEnum.ASC)
                )
            ).srcCiface(
                PromoSearchRequestDtoV2SrcCiface()
                .assortmentLoadMethod(listOf(AssortmentLoadMethod.PI.name))
            )
        //endregion
        verify(promoApiClient)?.searchPromo(expectedSearchRequest)

        verify(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.successUpdate.before.csv"],
        after = ["PromoExportServiceTest.successUpdate.after.csv"]
    )
    fun successUpdate() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(Response(httpStatus = 200)).`when`(promoB2BClient)
            ?.updatePromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        verify(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.successUpdate.before.csv"]
    )
    fun successUpdate_searchRequest() {
        val searchRequest = PromoSearchRequestDtoV2()
            .mechanicsType(
                listOf(
                    MechanicsType.PROMO_CODE,
                    MechanicsType.CHEAPEST_AS_GIFT,
                    MechanicsType.BLUE_FLASH,
                    MechanicsType.DIRECT_DISCOUNT
                )
            )
            .sourceType(listOf(SourceType.CATEGORYIFACE))
            .updatedAtFrom(
                OffsetDateTime.of(LocalDateTime.of(2022, 11, 3, 9, 15, 30), OffsetDateTime.now().offset)
                    .withOffsetSameInstant(UTC).toEpochSecond()
            )
            .sort(
                listOf(
                    PromoSearchRequestDtoV2Sort()
                        .field(PromoSearchRequestDtoV2Sort.FieldEnum.UPDATEDAT)
                        .direction(PromoSearchRequestDtoV2Sort.DirectionEnum.ASC)
                )
            )
            .srcCiface(
                PromoSearchRequestDtoV2SrcCiface()
                    .assortmentLoadMethod(listOf(AssortmentLoadMethod.PI.name))
            )

        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(searchRequest)

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(
            Response(httpStatus = 200)
        ).`when`(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        verify(promoApiClient)?.searchPromo(searchRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.successUpdate.before.csv"]
    )
    fun noPromosToExport() {
        doReturn(PromoSearchResult().promos(null).totalCount(0)).`when`(promoApiClient)
            ?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        promoExportService?.run()

        verify(promoService, never())?.getPromo(PROMO_ID)
        verify(promoB2BClient, never())?.updatePromo(any(PromoDescriptionRequestDto::class.java))
        //verifyNoMoreInteractions(promoB2BClient)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.before.csv"],
        after = ["PromoExportServiceTest.internalValidationError.after.csv"]
    )
    fun internalPromoValidationError() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        val promo = Promo(
            promoId = PROMO_ID,
            updatedAt = OffsetDateTime.now().toEpochSecond(),
            assortmentLoadMethod = AssortmentLoadMethod.PI
        )

        doReturn(promo).`when`(promoService)?.getPromo(PROMO_ID)
        promoExportService?.run()
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.before.csv"],
        after = ["PromoExportServiceTest.b2bValidationError.after.csv"]
    )
    fun b2bPromoValidationError() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo(PROMO_ID)

        doReturn(
            Response(httpStatus = 400, ErrorResponse(message = "Smth wrong"))
        ).`when`(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.before.csv"],
        after = ["PromoExportServiceTest.b2bCantSendPromoToStorageError.after.csv"]
    )
    fun b2bCantSendPromoToStorageError() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo(PROMO_ID)

        doReturn(Response(httpStatus = 507, ErrorResponse(message = "FAILED_SENDING_TO_PROMO_STORAGE.")))
            .`when`(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))

        val e = assertThrows<RuntimeException> { promoExportService?.run() }
        Assertions.assertEquals(
            "Error exporting promo to B2B: the service couldn't save promo [promoId=promoId] to storage. Reason: FAILED_SENDING_TO_PROMO_STORAGE.",
            e.message
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.before.csv"],
        after = ["PromoExportServiceTest.b2bInternalError.after.csv"]
    )
    fun b2bInternalError() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo(PROMO_ID)

        doReturn(Response(httpStatus = 500, ErrorResponse(message = "INTERNAL_SERVER_ERROR.")))
            .`when`(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))

        val e = assertThrows<RuntimeException> { promoExportService?.run() }
        Assertions.assertEquals(
            "Error exporting promo to B2B: B2B internal service error. [promoId=promoId]. Reason: INTERNAL_SERVER_ERROR.",
            e.message
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.before.csv"],
        after = ["PromoExportServiceTest.exceptionOnConnectingToB2B.after.csv"]
    )
    fun exceptionOnConnectingToB2B() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo(PROMO_ID)

        doReturn(
            Response(
                exception = Exception("Can't connect to promo b2b")
            )
        ).`when`(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))

        val e = assertThrows<RuntimeException> { promoExportService?.run() }
        Assertions.assertEquals("Can't connect to promo b2b", e.message)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.successCreateFullTest.before.csv"],
        after = ["PromoExportServiceTest.successCreateFullTest.after.csv"]
    )
    fun successCreateFullTest() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        val promo = Promo(
            //region init promo
            promoId = "promoId",
            parentPromoId = "parentPromoId",
            mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT,
            tradeManager = "trade",
            name = "best flash",
            landingUrl = "landingUrl",
            rulesUrl = "rulesUrl",
            piPublishDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
            createdAt = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond(),
            status = PromoStatus.READY,
            active = true,
            updatedAt = createOffsetDateTime(2022, 12, 24, 8, 0, 0).toEpochSecond(),
            assortmentLoadMethod = AssortmentLoadMethod.PI,
            promotions = listOf(
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Mobile Performance"
                ),
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Web Performance"
                ),
                PromoPromotion(
                    catteamName = "ЭиБТ",
                    categoryName = "Контентное присутствие",
                    channelName = "Landing page"
                ),
            ),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 1L, percent = 30),
                PromoCategoryRestrictionItem(id = 2L, percent = 25),
                PromoCategoryRestrictionItem(id = 4L, percent = 60)
            ),
            vendorsRestriction = listOf(1L, 2L, 3L),
            mskusRestriction = listOf(4L, 5L, 6L),
            warehousesRestriction = listOf(7L, 8L, 9L),
            partnersRestriction = listOf(10L, 11L, 12L),
            //endregion
        )

        doReturn(promo).`when`(promoService)?.getPromo("promoId")

        doReturn(
            Response(httpStatus = 200)
        ).`when`(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        val expectedDto = PromoDescriptionRequestDto(
            //region init dto
            promoId = "promoId",
            parentPromoId = "parentPromoId",
            promoMechanic = PiPromoMechanicDto.DIRECT_DISCOUNT,
            promoResponsibles = PromoResponsibleDto("trade"),
            additionalInfo = PromoAdditionalInfoDto(
                promoName = "best flash",
                status = PromoStatusDto.RUNNING,
                landingUrl = "landingUrl",
                rulesUrl = "rulesUrl",
                publishPiDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
                createdAtDate = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond()
            ),
            promoMechanicsData = null,
            channelsDto = PromoChannelsDto(listOf(55, 54, 61)),
            constraints = PromoConstraintsDto(
                startDateTime = null,
                endDateTime = null,
                enabled = true,
                categoryRestrictions = CategoryRestrictionDto(
                    listOf(
                        PromoCategoryDto(1L, 30),
                        PromoCategoryDto(3L, 25)
                    )
                ),
                originalCategoryRestrictions = OriginalCategoryRestrictionDto(
                    listOf(
                        PromoCategoryDto(1L, 30),
                        PromoCategoryDto(2L, 25),
                        PromoCategoryDto(4L, 60)
                    )
                ),
                originalBrandRestrictions = OriginalBrandRestrictionDto(
                    listOf(
                        PromoBrandDto(1L),
                        PromoBrandDto(2L),
                        PromoBrandDto(3L)
                    )
                ),
                mskuRestrictions = MskuRestrictionDto(listOf(4L, 5L, 6L)),
                warehouseRestrictions = WarehouseRestrictionDto(listOf(7L, 8L, 9L)),
                supplierRestrictions = SupplierRestrictionDto(listOf(10L, 11L, 12L))
            )
            //endregion
        )

        verify(promoB2BClient)?.createPromo(expectedDto)
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.recoveryAfterFailedExport.before.csv"],
        after = ["PromoExportServiceTest.recoveryAfterFailedExport.after.csv"]
    )
    fun recoveryAfterFailedExport() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(
            Response(httpStatus = 507, errorResponse = ErrorResponse(message = "FAILED_SENDING_TO_PROMO_STORAGE"))
        ).`when`(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))

        //Первый запуск
        assertThrows<RuntimeException> { promoExportService?.run() }

        //Проверяем, что метка последней обработанной акции не сохранилась
        val lastUpdateTime = environmentService?.getDateTime("promo_b2b_last_updated_promo")
        val expectedUpdateTime =
            OffsetDateTime.of(LocalDateTime.of(2022, 11, 3, 9, 15, 30), OffsetDateTime.now().offset)
                .withOffsetSameInstant(UTC)
        Assertions.assertEquals(expectedUpdateTime, lastUpdateTime)

        doReturn(
            Response(httpStatus = 200)
        ).`when`(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))

        //Второй запуск
        promoExportService?.run()

        verify(promoB2BClient, times(2))?.updatePromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.storageConflictOnPromoCreate.before.csv"],
        after = ["PromoExportServiceTest.storageConflictOnPromoCreate.after.csv"]
    )
    fun storageConflictOnPromoCreate() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(Response(httpStatus = 409, errorResponse = ErrorResponse(message = "Promo is in a storage already.")))
            .`when`(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))
        doReturn(Response(httpStatus = 200))
            .`when`(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        verify(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))
        verify(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.storageConflictOnPromoUpdate.before.csv"],
        after = ["PromoExportServiceTest.storageConflictOnPromoUpdate.after.csv"]
    )
    fun storageConflictOnPromoUpdate() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(Response(httpStatus = 409, errorResponse = ErrorResponse(message = "Promo is in a storage already.")))
            .`when`(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))
        doReturn(Response(httpStatus = 200))
            .`when`(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        verify(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))
        verify(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.saveSeveralPromos.before.csv"],
        after = ["PromoExportServiceTest.saveSeveralPromos.after.csv"]
    )
    fun saveSeveralPromos() {
        doReturn(
            PromoSearchResult().promos(
                listOf(
                    PromoSearchResultItem()
                        .promoId("cf_1000")
                        .updatedAt(createOffsetDateTime(2022, 12, 24, 8, 0, 0).toEpochSecond()),
                    PromoSearchResultItem()
                        .promoId("cf_1001")
                        .updatedAt(createOffsetDateTime(2022, 12, 24, 8, 1, 0).toEpochSecond()),
                    PromoSearchResultItem()
                        .promoId("cf_1002")
                        .updatedAt(createOffsetDateTime(2022, 12, 24, 8, 2, 0).toEpochSecond())
                )
            ).totalCount(3)
        ).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        val promos = listOf(
            //region promos init
            Promo(
                promoId = "cf_1000",
                mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
                assortmentLoadMethod = AssortmentLoadMethod.PI
            ), Promo(
                promoId = "cf_1001",
                mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
                assortmentLoadMethod = AssortmentLoadMethod.PI
            ), Promo(
                promoId = "cf_1002",
                mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
                assortmentLoadMethod = AssortmentLoadMethod.PI
            )
            //endregion
        )

        doReturn(promos[0]).`when`(promoService)?.getPromo("cf_1000")
        doReturn(promos[1]).`when`(promoService)?.getPromo("cf_1001")
        doReturn(promos[2]).`when`(promoService)?.getPromo("cf_1002")

        doReturn(Response(httpStatus = 200)).`when`(promoB2BClient)
            ?.createPromo(any(PromoDescriptionRequestDto::class.java))
        doReturn(Response(httpStatus = 200)).`when`(promoB2BClient)
            ?.updatePromo(any(PromoDescriptionRequestDto::class.java))
        doReturn(Response(httpStatus = 200)).`when`(promoB2BClient)
            ?.createPromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        verify(promoB2BClient, times(2))?.createPromo(any(PromoDescriptionRequestDto::class.java))
        verify(promoB2BClient, times(1))?.updatePromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.skipSentPromo.before.csv"],
        after = ["PromoExportServiceTest.skipSentPromo.after.csv"]
    )
    fun skipSentPromo() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        promoExportService?.run()

        verify(promoB2BClient, never())?.updatePromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.skipAlreadyCheckedInvalidPromo.csv"],
        after = ["PromoExportServiceTest.skipAlreadyCheckedInvalidPromo.csv"]
    )
    fun skipAlreadyCheckedInvalidPromo() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        promoExportService?.run()

        verify(promoB2BClient, never())?.updatePromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.successUpdateAfterValidationError.before.csv"],
        after = ["PromoExportServiceTest.successUpdateAfterValidationError.after.csv"]
    )
    fun successUpdateAfterValidationError() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(Response(httpStatus = 200)).`when`(promoB2BClient)
            ?.updatePromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        verify(promoB2BClient)?.updatePromo(any(PromoDescriptionRequestDto::class.java))
    }

    @Test
    @DbUnitDataSet(
        before = ["PromoExportServiceTest.successCreateAfterValidationError.before.csv"],
        after = ["PromoExportServiceTest.successCreateAfterValidationError.after.csv"]
    )
    fun successCreateAfterValidationError() {
        doReturn(SEARCH_RESULT).`when`(promoApiClient)?.searchPromo(any(PromoSearchRequestDtoV2::class.java))

        doReturn(PROMO).`when`(promoService)?.getPromo("promoId")

        doReturn(Response(httpStatus = 200)).`when`(promoB2BClient)
            ?.createPromo(any(PromoDescriptionRequestDto::class.java))

        promoExportService?.run()

        verify(promoB2BClient)?.createPromo(any(PromoDescriptionRequestDto::class.java))
    }
}
