package ru.yandex.market.pricingmgmt.util.promo

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.CompensationReceiveMethod
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoBudgetOwner
import ru.yandex.market.pricingmgmt.model.promo.PromoDisplayStatus
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoPurpose
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CheapestAsGift
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CompleteSetKind
import ru.yandex.market.pricingmgmt.model.promo.mechanics.Promocode
import ru.yandex.market.pricingmgmt.model.promo.mechanics.PromocodeType
import ru.yandex.market.pricingmgmt.model.promo.restrictions.PromoCategoryRestrictionItem
import ru.yandex.market.pricingmgmt.service.promo.converters.PromoConverters
import ru.yandex.mj.generated.server.model.CheapestAsGiftDto
import ru.yandex.mj.generated.server.model.PromoCategoriesRestrictionItemDto
import ru.yandex.mj.generated.server.model.PromoCategoriesRestrictionItemShortDto
import ru.yandex.mj.generated.server.model.PromoDtoRequest
import ru.yandex.mj.generated.server.model.PromoDtoResponse
import ru.yandex.mj.generated.server.model.PromoPromotionDto
import ru.yandex.mj.generated.server.model.PromoRestrictionItemDto
import ru.yandex.mj.generated.server.model.PromocodeDto
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal class PromoClientDtoConverterTest : AbstractFunctionalTest() {

    private val now: OffsetDateTime = OffsetDateTime.now().withNano(0).withOffsetSameInstant(ZoneOffset.UTC)

    @Autowired
    lateinit var promoConverters: PromoConverters

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun activeNewPromoToDtoStatusTest() {
        val promo = buildBasePromo()
        val dto = PromoClientDtoConverter.toDto(promo, promoConverters)

        assertEquals(PromoStatus.NEW.name, dto.status)
        assertEquals(PromoDisplayStatus.NEW.name, dto.displayStatus)
    }

    @Test
    fun inactiveNewPromoToDtoStatusTest() {
        val promo = buildBasePromo()
        promo.active = false
        val dto = PromoClientDtoConverter.toDto(promo, promoConverters)

        assertEquals(PromoStatus.NEW.name, dto.status)
        assertEquals(PromoDisplayStatus.DISABLED.name, dto.displayStatus)
    }

    @Test
    fun cancelledPromoToDtoStatusTest() {
        val promo = buildBasePromo()
        promo.status = PromoStatus.CANCELED
        val dto = PromoClientDtoConverter.toDto(promo, promoConverters)

        assertEquals(PromoStatus.CANCELED.name, dto.status)
        assertEquals(PromoDisplayStatus.CANCELED.name, dto.displayStatus)
    }

    @Test
    fun runningPromoToDtoStatusTest() {
        val promo = buildBasePromo()
        promo.status = PromoStatus.READY
        promo.startDate = OffsetDateTime.now().minusDays(10).toEpochSecond()
        promo.endDate = OffsetDateTime.now().plusDays(10).toEpochSecond()
        val dto = PromoClientDtoConverter.toDto(promo, promoConverters)

        assertEquals(PromoStatus.READY.name, dto.status)
        assertEquals(PromoDisplayStatus.RUNNING.name, dto.displayStatus)
    }

    @Test
    fun finishedPromoToDtoStatusTest() {
        val promo = buildBasePromo()
        promo.status = PromoStatus.READY
        promo.startDate = OffsetDateTime.now().minusDays(10).toEpochSecond()
        promo.endDate = OffsetDateTime.now().minusDays(5).toEpochSecond()
        val dto = PromoClientDtoConverter.toDto(promo, promoConverters)

        assertEquals(PromoStatus.READY.name, dto.status)
        assertEquals(PromoDisplayStatus.FINISHED.name, dto.displayStatus)
    }

    @Test
    fun readyPromoToDtoStatusTest() {
        val promo = buildBasePromo()
        promo.status = PromoStatus.READY
        promo.startDate = OffsetDateTime.now().plusDays(10).toEpochSecond()
        promo.endDate = OffsetDateTime.now().plusDays(15).toEpochSecond()
        val dto = PromoClientDtoConverter.toDto(promo, promoConverters)

        assertEquals(PromoStatus.READY.name, dto.status)
        assertEquals(PromoDisplayStatus.READY.name, dto.displayStatus)
    }

    private fun buildBasePromo(): Promo {
        return Promo(
            name = "Test promo",
            promoKind = PromoKind.CROSS_CATEGORY,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            parentPromoId = "SP0001",
            startDate = now.minusDays(50).toEpochSecond(),
            endDate = now.minusDays(20).toEpochSecond(),
            tradeManager = "tmanager01",
            departments = listOf("dept01", "dept02"),
            streams = listOf("stream01", "stream02"),
            markom = "catmen01",
            purpose = PromoPurpose.GMV_GENERATION,
            budgetOwner = PromoBudgetOwner.PRODUCT,
            supplierType = SupplierType.FIRST_PARTY,
            compensationSource = Compensation.PARTNER,
            status = PromoStatus.NEW,
            active = true,
            hidden = false,
            cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_2),
            landingUrlAutogenerated = false,
            landingUrl = "landing/Page/Url",
            rulesUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            assortmentLoadMethod = AssortmentLoadMethod.TRACKER,
            piPublishDate = now.minusDays(60).toEpochSecond(),
        )
    }

    private fun buildBasePromoDtoRequest(): PromoDtoRequest {
        return PromoDtoRequest()
            .name("Test promo")
            .promoKind(PromoKind.CROSS_CATEGORY.name)
            .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
            .parentPromoId("SP0001")
            .startDate(now.minusDays(50).toEpochSecond())
            .endDate(now.minusDays(20).toEpochSecond())
            .tradeManager("tmanager01")
            .streams(listOf("stream01", "stream02"))
            .departments(listOf("dept01", "dept02"))
            .markom("catmen01")
            .purpose(PromoPurpose.GMV_GENERATION.name)
            .budgetOwner(PromoBudgetOwner.PRODUCT.name)
            .supplierType(SupplierType.FIRST_PARTY.value)
            .compensationSource(Compensation.PARTNER.name)
            .status(PromoStatus.NEW.name)
            .disabled(false)
            .hidden(false)
            .cheapestAsGift(
                CheapestAsGiftDto()
                    .count(2)
            )
            .landingPageUrl("landing/Page/Url")
            .rulesPageUrl("rules/Page/Url")
            .assortmentLoadMethod("TRACKER")
            .piPublishedDate(now.minusDays(60).toEpochSecond())
    }

    private fun buildBasePromoDtoResponse(): PromoDtoResponse {
        return PromoDtoResponse()
            .name("Test promo")
            .promoKind(PromoKind.CROSS_CATEGORY.name)
            .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
            .parentPromoId("SP0001")
            .startDate(now.minusDays(50).toEpochSecond())
            .endDate(now.minusDays(20).toEpochSecond())
            .tradeManager("tmanager01")
            .streams(listOf("stream01", "stream02"))
            .departments(listOf("dept01", "dept02"))
            .department("dept01")
            .markom("catmen01")
            .purpose(PromoPurpose.GMV_GENERATION.name)
            .budgetOwner(PromoBudgetOwner.PRODUCT.name)
            .supplierType(SupplierType.FIRST_PARTY.value)
            .compensationSource(Compensation.PARTNER.name)
            .compensationReceiveMethods(emptyList())
            .status(PromoStatus.NEW.name)
            .disabled(false)
            .hidden(false)
            .cheapestAsGift(
                CheapestAsGiftDto()
                    .count(2)
            )
            .landingPageUrlAutogenerated(false)
            .landingPageUrl("landing/Page/Url")
            .rulesPageUrlAutogenerated(false)
            .rulesPageUrl("rules/Page/Url")
            .promotion(PromoPromotionDto())
            .displayStatus("NEW")
            .assortmentLoadMethod("TRACKER")
            .piPublishedDate(now.minusDays(60).toEpochSecond())
    }

    @Test
    fun promoFromDtoTest() {
        val promoDtoRequest = buildBasePromoDtoRequest()
        val expected = buildBasePromo()

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethod_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationReceiveMethods(null)
        val expected = buildBasePromo()
        expected.compensationReceiveMethods = emptyList()

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethod_Empty() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationReceiveMethods(emptyList())
        val expected = buildBasePromo()
        expected.compensationReceiveMethods = emptyList()

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethod_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
        val expected = buildBasePromo()
        expected.compensationReceiveMethods =
            listOf(CompensationReceiveMethod.WITHOUT_COMPENSATION, CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE)

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethods_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationReceiveMethods(null)
        val expected = buildBasePromo()
        expected.compensationReceiveMethods = emptyList()

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethods_Empty() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationReceiveMethods(emptyList())
        val expected = buildBasePromo()
        expected.compensationReceiveMethods = emptyList()

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethods_NotEmpty() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
        val expected = buildBasePromo()
        expected.compensationReceiveMethods =
            listOf(CompensationReceiveMethod.WITHOUT_COMPENSATION, CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE)

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_promoKind_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .promoKind(null)
        val expected = buildBasePromo()
        expected.promoKind = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_promoKind_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .promoKind(PromoKind.CROSS_CATEGORY.name)
        val expected = buildBasePromo()
        expected.promoKind = PromoKind.CROSS_CATEGORY

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_promoKind_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .promoKind("promoKindUnknown")
        val expected = buildBasePromo()
        expected.promoKind = PromoKind.UNKNOWN

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_mechanicsType_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .mechanicsType(null)
        val expected = buildBasePromo()
        expected.mechanicsType = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_mechanicsType_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.name)
        val expected = buildBasePromo()
        expected.mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_mechanicsType_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .mechanicsType("promoKindUnknown")
        val expected = buildBasePromo()
        expected.mechanicsType = PromoMechanicsType.UNKNOWN

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_purpose_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .purpose(null)
        val expected = buildBasePromo()
        expected.purpose = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_purpose_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .purpose(PromoPurpose.CLIENT_ACQUISITION.name)
        val expected = buildBasePromo()
        expected.purpose = PromoPurpose.CLIENT_ACQUISITION

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_purpose_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .purpose("purposeUnknown")
        val expected = buildBasePromo()
        expected.purpose = PromoPurpose.UNKNOWN

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_budgetOwner_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .budgetOwner(null)
        val expected = buildBasePromo()
        expected.budgetOwner = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_budgetOwner_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .budgetOwner(PromoBudgetOwner.PRODUCT.name)
        val expected = buildBasePromo()
        expected.budgetOwner = PromoBudgetOwner.PRODUCT

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_budgetOwner_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .budgetOwner("budgetOwnerUnknown")
        val expected = buildBasePromo()
        expected.budgetOwner = PromoBudgetOwner.UNKNOWN

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_supplierType_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .supplierType(null)
        val expected = buildBasePromo()
        expected.supplierType = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_supplierType_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .supplierType(SupplierType.FIRST_PARTY.displayName)
        val expected = buildBasePromo()
        expected.supplierType = SupplierType.FIRST_PARTY

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_supplierType_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .supplierType("supplierTypeUnknown")
        val expected = buildBasePromo()
        expected.supplierType = SupplierType.UNKNOWN

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationSource_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationSource(null)
        val expected = buildBasePromo()
        expected.compensationSource = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationSource_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationSource(Compensation.MARKET.name)
        val expected = buildBasePromo()
        expected.compensationSource = Compensation.MARKET

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_compensationSource_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .compensationSource("compensationSourceUnknown")
        val expected = buildBasePromo()
        expected.compensationSource = Compensation.UNKNOWN

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_status_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .status(null)
        val expected = buildBasePromo()
        expected.status = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_status_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .status(PromoStatus.NEW.name)
        val expected = buildBasePromo()
        expected.status = PromoStatus.NEW

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_status_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .status("statusUnknown")
        val expected = buildBasePromo()
        expected.status = PromoStatus.UNKNOWN

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_completeSetKind_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .cheapestAsGift(
                CheapestAsGiftDto()
                    .count(null)
            )

        val expected = buildBasePromo()
        expected.cheapestAsGift = CheapestAsGift(
            completeSetKind = null
        )

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_completeSetKind_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .cheapestAsGift(
                CheapestAsGiftDto()
                    .count(2)
            )

        val expected = buildBasePromo()
        expected.cheapestAsGift = CheapestAsGift(
            completeSetKind = CompleteSetKind.SET_OF_2
        )

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_completeSetKind_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .cheapestAsGift(
                CheapestAsGiftDto()
                    .count(123456789)
            )

        val expected = buildBasePromo()
        expected.cheapestAsGift = CheapestAsGift(
            completeSetKind = CompleteSetKind.UNKNOWN
        )

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_assortmentLoadMethod_Null() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .assortmentLoadMethod(null)
        val expected = buildBasePromo()
        expected.assortmentLoadMethod = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_assortmentLoadMethod_NotNull() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .assortmentLoadMethod(AssortmentLoadMethod.PI.name)
        val expected = buildBasePromo()
        expected.assortmentLoadMethod = AssortmentLoadMethod.PI

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_assortmentLoadMethod_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .assortmentLoadMethod("assortmentLoadMethodUnknown")
        val expected = buildBasePromo()
        expected.assortmentLoadMethod = AssortmentLoadMethod.UNKNOWN

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_promocode() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .cheapestAsGift(null)
            .promocode(
                PromocodeDto()
                    .codeType(PromocodeType.FIXED_DISCOUNT.name)
                    .value(1)
                    .code("code")
                    .minCartPrice(2)
                    .maxCartPrice(3)
                    .applyMultipleTimes(true)
                    .additionalConditions("additionalConditions")
            )
        val expected = buildBasePromo()
        expected.cheapestAsGift = null
        expected.promocode = Promocode(
            codeType = PromocodeType.FIXED_DISCOUNT,
            value = 1,
            code = "code",
            minCartPrice = 2,
            maxCartPrice = 3,
            applyMultipleTimes = true,
            additionalConditions = "additionalConditions"
        )

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_autogeneratedNullToTrue_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .landingPageUrlAutogenerated(null)
            .landingPageUrl("")
            .rulesPageUrlAutogenerated(null)
            .rulesPageUrl("")

        val promo = PromoClientDtoConverter.fromDto(promoDtoRequest)

        assertNotNull(promo.landingUrlAutogenerated)
        assertTrue(promo.landingUrlAutogenerated!!)
        assertNotNull(promo.rulesUrlAutogenerated)
        assertTrue(promo.rulesUrlAutogenerated!!)
    }

    @Test
    fun promoFromDtoTest_autogeneratedNullToFalse_Unknown() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .landingPageUrlAutogenerated(null)
            .landingPageUrl("landingPageUrl")
            .rulesPageUrlAutogenerated(null)
            .rulesPageUrl("rulesPageUrl")

        val promo = PromoClientDtoConverter.fromDto(promoDtoRequest)

        assertNotNull(promo.landingUrlAutogenerated)
        assertFalse(promo.landingUrlAutogenerated!!)
        assertNotNull(promo.rulesUrlAutogenerated)
        assertFalse(promo.rulesUrlAutogenerated!!)
    }

    @Test
    fun promoFromDtoTest_emptyLists_success() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .department(null)
            .departments(null)
            .compensationReceiveMethod(null)
            .compensationReceiveMethods(null)
            .streams(null)

        var promo = Promo()

        assertDoesNotThrow {
            promo = PromoClientDtoConverter.fromDto(promoDtoRequest)
        }

        assertTrue(promo.departments.isEmpty())
        assertTrue(promo.compensationReceiveMethods.isEmpty())
        assertTrue(promo.streams.isEmpty())
    }

    @Test
    fun promoFromDtoTest_constraints_distinct_success() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemShortDto()
                        .id(1)
                        .percent(11),
                    PromoCategoriesRestrictionItemShortDto()
                        .id(1)
                        .percent(12),
                    PromoCategoriesRestrictionItemShortDto()
                        .id(2)
                        .percent(22),
                    PromoCategoriesRestrictionItemShortDto()
                        .id(2)
                        .percent(22),
                    PromoCategoriesRestrictionItemShortDto()
                        .id(3)
                        .percent(null),
                    PromoCategoriesRestrictionItemShortDto()
                        .id(3)
                        .percent(null),
                    PromoCategoriesRestrictionItemShortDto()
                        .id(4)
                        .percent(44),
                    PromoCategoriesRestrictionItemShortDto()
                        .id(5)
                        .percent(null)
                )
            )
            .partnersRestriction(
                listOf(1, 1, 2)
            )
            .vendorsRestriction(listOf(1, 1, 2))
            .mskusRestriction(listOf(1, 1, 2))
            .warehouseRestriction(listOf(1, 1, 2))

        val expected = buildBasePromo()
        expected.categoriesRestriction = listOf(
            PromoCategoryRestrictionItem(id = 1, percent = 11),
            PromoCategoryRestrictionItem(id = 1, percent = 12),
            PromoCategoryRestrictionItem(id = 2, percent = 22),
            PromoCategoryRestrictionItem(id = 3, percent = null),
            PromoCategoryRestrictionItem(id = 4, percent = 44),
            PromoCategoryRestrictionItem(id = 5, percent = null),
        )
        expected.partnersRestriction = listOf(1,2)
        expected.vendorsRestriction = listOf(1,2)
        expected.mskusRestriction = listOf(1,2)
        expected.warehousesRestriction = listOf(1,2)

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoFromDtoTest_constraints_null_success() {
        val promoDtoRequest = buildBasePromoDtoRequest()
            .categoriesRestriction(null)
            .partnersRestriction(null)
            .vendorsRestriction(null)
            .mskusRestriction(null)
            .warehouseRestriction(null)

        val expected = buildBasePromo()
        expected.categoriesRestriction = null
        expected.partnersRestriction = null
        expected.vendorsRestriction = null
        expected.mskusRestriction = null
        expected.warehousesRestriction = null

        assertEquals(expected, PromoClientDtoConverter.fromDto(promoDtoRequest))
    }

    @Test
    fun promoToDtoTest() {
        val promo = buildBasePromo()
        val promoDtoResponse = buildBasePromoDtoResponse()

        assertEquals(promoDtoResponse, PromoClientDtoConverter.toDto(promo, promoConverters))
    }

    @Test
    fun promoToDtoTest_compensationReceiveMethod_empty() {
        val promo = buildBasePromo()
        promo.compensationReceiveMethods = emptyList()
        val promoDtoResponse = buildBasePromoDtoResponse()
            .compensationReceiveMethod(null)

        assertEquals(promoDtoResponse, PromoClientDtoConverter.toDto(promo, promoConverters))
    }

    @Test
    fun promoToDtoTest_compensationReceiveMethod_one() {
        val promo = buildBasePromo()
        promo.compensationReceiveMethods = listOf(CompensationReceiveMethod.WITHOUT_COMPENSATION)
        val promoDtoResponse = buildBasePromoDtoResponse()
            .compensationReceiveMethods(listOf(CompensationReceiveMethod.WITHOUT_COMPENSATION.name))
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)

        assertEquals(promoDtoResponse, PromoClientDtoConverter.toDto(promo, promoConverters))
    }

    @Test
    fun promoToDtoTest_compensationReceiveMethod_several() {
        val promo = buildBasePromo()
        promo.compensationReceiveMethods =
            listOf(CompensationReceiveMethod.WITHOUT_COMPENSATION, CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE)
        val promoDtoResponse = buildBasePromoDtoResponse()
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )
            .compensationReceiveMethod(CompensationReceiveMethod.WITHOUT_COMPENSATION.name)

        assertEquals(promoDtoResponse, PromoClientDtoConverter.toDto(promo, promoConverters))
    }

    @Test
    fun promoToDtoTest_promocode() {
        val promo = buildBasePromo()
        promo.cheapestAsGift = null
        promo.promocode = Promocode(
            codeType = PromocodeType.FIXED_DISCOUNT,
            value = 1,
            code = "code",
            minCartPrice = 2,
            maxCartPrice = 3,
            applyMultipleTimes = true,
            additionalConditions = "additionalConditions"
        )

        val promoDtoResponse = buildBasePromoDtoResponse()
            .cheapestAsGift(null)
            .promocode(
                PromocodeDto()
                    .codeType(PromocodeType.FIXED_DISCOUNT.name)
                    .value(1)
                    .code("code")
                    .minCartPrice(2)
                    .maxCartPrice(3)
                    .applyMultipleTimes(true)
                    .additionalConditions("additionalConditions")
            )

        assertEquals(promoDtoResponse, PromoClientDtoConverter.toDto(promo, promoConverters))
    }

    @Test
    @DbUnitDataSet(before = ["PromoClientDtoConverterTest.csv"])
    fun promoToDtoTest_restrictions() {
        val promo = buildBasePromo()
        promo.partnersRestriction = listOf(1L, 2L)
        promo.categoriesRestriction = listOf(
            PromoCategoryRestrictionItem(id = 21L, percent = 21),
            PromoCategoryRestrictionItem(id = 22L, percent = 22)
        )
        promo.vendorsRestriction = listOf(31L, 32L)
        promo.mskusRestriction = listOf(41L, 42L)
        promo.warehousesRestriction = listOf(21L, 22L)

        val promoDtoResponse = buildBasePromoDtoResponse()
            .partnersRestriction(
                listOf(
                    PromoRestrictionItemDto().id(1L).name("partner01").outdated(false),
                    PromoRestrictionItemDto().id(2L).name("partner02").outdated(true)
                )
            )
            .categoriesRestriction(
                listOf(
                    PromoCategoriesRestrictionItemDto().id(21L).name("category21").outdated(false).percent(21),
                    PromoCategoriesRestrictionItemDto().id(22L).name("category22").outdated(true).percent(22),
                )
            )
            .vendorsRestriction(
                listOf(
                    PromoRestrictionItemDto().id(31L).name("vendor31").outdated(false),
                    PromoRestrictionItemDto().id(32L).name("vendor32").outdated(true)
                )
            )
            .mskusRestriction(
                listOf(
                    PromoRestrictionItemDto().id(41L).name("msku41").outdated(false),
                    PromoRestrictionItemDto().id(42L).name("msku42").outdated(true)
                )
            )
            .warehouseRestriction(
                listOf(21L, 22L)
            )
            .warehouseRestrictionV2(
                listOf(
                    PromoRestrictionItemDto().id(21L).name("warehouse21").outdated(false),
                    PromoRestrictionItemDto().id(22L).name("warehouse22").outdated(true)
                )
            )

        assertEquals(promoDtoResponse, PromoClientDtoConverter.toDto(promo, promoConverters))
    }
}
