package ru.yandex.market.pricingmgmt.util.promo

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationFilter
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.CompensationReceiveMethod
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoBudgetOwner
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoPurpose
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CheapestAsGift
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CompleteSetKind
import ru.yandex.market.pricingmgmt.model.promo.mechanics.Promocode
import ru.yandex.market.pricingmgmt.model.promo.mechanics.PromocodeType
import ru.yandex.market.pricingmgmt.util.exception.ConvertException
import ru.yandex.mj.generated.client.promoservice.model.CategoryPromoConstraintDto
import ru.yandex.mj.generated.client.promoservice.model.GenerateableUrlDto
import ru.yandex.mj.generated.client.promoservice.model.MskuPromoConstraintDto
import ru.yandex.mj.generated.client.promoservice.model.PromoMainRequestParams
import ru.yandex.mj.generated.client.promoservice.model.PromoMainResponseParams
import ru.yandex.mj.generated.client.promoservice.model.PromoMechanicsParams
import ru.yandex.mj.generated.client.promoservice.model.PromoRequestV2
import ru.yandex.mj.generated.client.promoservice.model.PromoResponseV2
import ru.yandex.mj.generated.client.promoservice.model.PromoSrcParams
import ru.yandex.mj.generated.client.promoservice.model.SourceType
import ru.yandex.mj.generated.client.promoservice.model.SrcCifaceDtoV2
import ru.yandex.mj.generated.client.promoservice.model.SupplierPromoConstraintsDto
import ru.yandex.mj.generated.client.promoservice.model.VendorPromoConstraintDto
import ru.yandex.mj.generated.client.promoservice.model.WarehousePromoConstraintDto
import java.time.OffsetDateTime
import java.time.ZoneOffset

@WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = ["PRICING_MGMT_ACCESS"])
internal class PromoServerDtoConverterTest : AbstractFunctionalTest() {
    private val now: OffsetDateTime = OffsetDateTime.now()

    private fun buildBasePromoRequest(): PromoRequestV2 {
        return PromoRequestV2()
            .promoId("promoId")
            .modifiedBy("localDeveloper")
            .main(
                PromoMainRequestParams()
                    .sourceType(SourceType.CATEGORYIFACE)
                    .name("Test promo")
                    .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.clientValue)
                    .parentPromoId("SP0001")
                    .startAt(now.minusDays(50).toEpochSecond())
                    .endAt(now.minusDays(20).toEpochSecond())
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
                    .active(true)
                    .hidden(false)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("rules/Page/Url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("landing/Page/Url")
                            .auto(false)
                    )
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(
                        ru.yandex.mj.generated.client.promoservice.model.CheapestAsGift()
                            .count(CompleteSetKind.SET_OF_2.productsCount)
                    )
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind(PromoKind.CROSS_CATEGORY.toString())
                            .tradeManager("tmanager01")
                            .departments(listOf("dept01", "dept02"))
                            .streams(listOf("stream01", "stream02"))
                            .markom("catmen01")
                            .purpose(PromoPurpose.GMV_GENERATION.name)
                            .budgetOwner(PromoBudgetOwner.PRODUCT.name)
                            .supplierType(SupplierType.FIRST_PARTY.value)
                            .compensationSource(Compensation.PARTNER.name)
                            .compensationReceiveMethods(emptyList())
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto())
            .warehousesConstraints(WarehousePromoConstraintDto())
            .categoriesConstraints(CategoryPromoConstraintDto())
            .vendorsConstraints(VendorPromoConstraintDto())
            .mskusConstraints(MskuPromoConstraintDto())
    }

    private fun buildBasePromoResponse(): PromoResponseV2 {
        return PromoResponseV2()
            .promoId("promoId")
            .main(
                PromoMainResponseParams()
                    .name("Test promo")
                    .mechanicsType(PromoMechanicsType.CHEAPEST_AS_GIFT.clientValue)
                    .parentPromoId("SP0001")
                    .startAt(now.minusDays(50).toEpochSecond())
                    .endAt(now.minusDays(20).toEpochSecond())
                    .status(ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW)
                    .active(true)
                    .hidden(false)
                    .rulesUrl(
                        GenerateableUrlDto()
                            .url("rules/Page/Url")
                            .auto(false)
                    )
                    .landingUrl(
                        GenerateableUrlDto()
                            .url("landing/Page/Url")
                            .auto(false)
                    )
            )
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(
                        ru.yandex.mj.generated.client.promoservice.model.CheapestAsGift()
                            .count(CompleteSetKind.SET_OF_2.productsCount)
                    )
            )
            .src(
                PromoSrcParams()
                    .ciface(
                        SrcCifaceDtoV2()
                            .promoKind(PromoKind.CROSS_CATEGORY.toString())
                            .tradeManager("tmanager01")
                            .departments(listOf("dept01", "dept02"))
                            .streams(listOf("stream01", "stream02"))
                            .markom("catmen01")
                            .purpose(PromoPurpose.GMV_GENERATION.name)
                            .budgetOwner(PromoBudgetOwner.PRODUCT.name)
                            .supplierType(SupplierType.FIRST_PARTY.value)
                            .compensationSource(Compensation.PARTNER.name)
                    )
            )
            .suppliersConstraints(SupplierPromoConstraintsDto())
            .warehousesConstraints(WarehousePromoConstraintDto())
    }

    @Test
    fun promoToDtoTest_compensationReceiveMethod_null_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false
        )

        val expected = buildBasePromoRequest()
        expected
            .src!!
            .ciface!!
            .compensationReceiveMethods(emptyList())

        Assertions.assertEquals(expected, PromoServerDtoConverter.toDto(promo))
    }

    @Test
    fun promoToDtoTest_compensationReceiveMethod_notEmpty_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = listOf(
                CompensationReceiveMethod.WITHOUT_COMPENSATION,
                CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE
            )
        )

        val expected = buildBasePromoRequest()
        expected
            .src!!
            .ciface!!
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )

        Assertions.assertEquals(expected, PromoServerDtoConverter.toDto(promo))
    }

    @Test
    fun promoToDtoTest_ok() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = emptyList(),
            piPublishDate = OffsetDateTime.of(2022, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toEpochSecond()
        )

        val promoRequest = buildBasePromoRequest()
        promoRequest
            .src!!
            .ciface!!
            .piPublishedAt(OffsetDateTime.of(2022, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toEpochSecond())

        Assertions.assertEquals(promoRequest, PromoServerDtoConverter.toDto(promo, null))
    }

    @Test
    fun promoToDtoTest_supplier() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = emptyList(),
            partnersRestriction = listOf(123L)
        )

        val promoRequest = buildBasePromoRequest()
        promoRequest
            .suppliersConstraints(
                SupplierPromoConstraintsDto()
                    .suppliers(listOf(123L))
                    .exclude(false)
            )

        Assertions.assertEquals(promoRequest, PromoServerDtoConverter.toDto(promo, null))
    }

    @Test
    fun promoToDtoTest_warehouse() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = emptyList(),
            warehousesRestriction = listOf(223L)
        )

        val promoRequest = buildBasePromoRequest()
        promoRequest
            .warehousesConstraints(
                WarehousePromoConstraintDto()
                    .warehouses(listOf(223L))
                    .exclude(false)
            )

        Assertions.assertEquals(promoRequest, PromoServerDtoConverter.toDto(promo, null))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethod_known_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = listOf(
                CompensationReceiveMethod.WITHOUT_COMPENSATION,
                CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE
            )
        )

        val promoResponse = buildBasePromoResponse()
        promoResponse
            .src!!
            .ciface!!
            .compensationReceiveMethods(
                listOf(
                    CompensationReceiveMethod.WITHOUT_COMPENSATION.name,
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )

        Assertions.assertEquals(promo, PromoServerDtoConverter.fromDto(promoResponse))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethod_unknown_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = listOf(
                CompensationReceiveMethod.UNKNOWN,
                CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE
            )
        )

        val promoResponse = buildBasePromoResponse()
        promoResponse
            .src!!
            .ciface!!
            .compensationReceiveMethods(
                listOf(
                    "unknown compensation receive method",
                    CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE.name
                )
            )

        Assertions.assertEquals(promo, PromoServerDtoConverter.fromDto(promoResponse))
    }

    @Test
    fun promoFromDtoTest_compensationReceiveMethod_empty_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = emptyList()
        )

        val promoResponse = buildBasePromoResponse()
        promoResponse
            .src!!
            .ciface!!
            .compensationReceiveMethods(emptyList())

        Assertions.assertEquals(promo, PromoServerDtoConverter.fromDto(promoResponse))
    }

    @Test
    fun promoFromDtoTest_department_one_success() {
        val promo = Promo(
            promoId = "promoId",
            name = "Test promo",
            promoKind = PromoKind.CROSS_CATEGORY,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            parentPromoId = "SP0001",
            startDate = now.minusDays(50).toEpochSecond(),
            endDate = now.minusDays(20).toEpochSecond(),
            tradeManager = "tmanager01",
            departments = listOf("dept01"),
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = emptyList()
        )

        val promoResponse = buildBasePromoResponse()
        promoResponse
            .src!!
            .ciface!!
            .departments(listOf("dept01"))

        Assertions.assertEquals(promo, PromoServerDtoConverter.fromDto(promoResponse))
    }

    @Test
    fun promoFromDtoTest_department_empty_success() {
        val promo = Promo(
            promoId = "promoId",
            name = "Test promo",
            promoKind = PromoKind.CROSS_CATEGORY,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            parentPromoId = "SP0001",
            startDate = now.minusDays(50).toEpochSecond(),
            endDate = now.minusDays(20).toEpochSecond(),
            tradeManager = "tmanager01",
            departments = emptyList(),
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            compensationReceiveMethods = emptyList()
        )

        val promoResponse = buildBasePromoResponse()
        promoResponse
            .src!!
            .ciface!!
            .departments(emptyList())

        Assertions.assertEquals(promo, PromoServerDtoConverter.fromDto(promoResponse))
    }

    @Test
    fun promoFromDtoTest_supplier_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            partnersRestriction = listOf(123L)
        )

        val promoResponse = buildBasePromoResponse()
            .suppliersConstraints(
                SupplierPromoConstraintsDto()
                    .suppliers(listOf(123L))
                    .exclude(false)
            )

        Assertions.assertEquals(promo, PromoServerDtoConverter.fromDto(promoResponse))
    }

    @Test
    fun promoFromDtoTest_supplier_withoutExclude_throwsException() {
        val promoResponse = buildBasePromoResponse()
            .suppliersConstraints(
                SupplierPromoConstraintsDto()
                    .suppliers(listOf(123L))
            )

        val exception = Assertions.assertThrows(ConvertException::class.java) {
            PromoServerDtoConverter.fromDto(promoResponse)
        }
        Assertions.assertEquals("Constraint suppliers exclude = null", exception.message)
    }

    @Test
    fun promoFromDtoTest_supplier_withExcludeTrue_throwsException() {
        val promoResponse = buildBasePromoResponse()
            .suppliersConstraints(
                SupplierPromoConstraintsDto()
                    .suppliers(listOf(123L))
                    .exclude(true)
            )

        val exception = Assertions.assertThrows(ConvertException::class.java) {
            PromoServerDtoConverter.fromDto(promoResponse)
        }
        Assertions.assertEquals("Constraint suppliers exclude = true", exception.message)
    }

    @Test
    fun promoFromDtoTest_warehouse_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            warehousesRestriction = listOf(123L)
        )

        val promoResponse = buildBasePromoResponse()
            .warehousesConstraints(
                WarehousePromoConstraintDto()
                    .warehouses(listOf(123L))
                    .exclude(false)
            )

        Assertions.assertEquals(promo, PromoServerDtoConverter.fromDto(promoResponse))
    }

    @Test
    fun promoFromDtoTest_warehouse_withoutExclude_throwsException() {
        val promoResponse = buildBasePromoResponse()
            .warehousesConstraints(
                WarehousePromoConstraintDto()
                    .warehouses(listOf(123L))
            )

        val exception = Assertions.assertThrows(ConvertException::class.java) {
            PromoServerDtoConverter.fromDto(promoResponse)
        }
        Assertions.assertEquals("Constraint warehouses exclude = null", exception.message)
    }

    @Test
    fun promoFromDtoTest_warehouse_withExcludeTrue_throwsException() {
        val promoResponse = buildBasePromoResponse()
            .warehousesConstraints(
                WarehousePromoConstraintDto()
                    .warehouses(listOf(123L))
                    .exclude(true)
            )

        val exception = Assertions.assertThrows(ConvertException::class.java) {
            PromoServerDtoConverter.fromDto(promoResponse)
        }
        Assertions.assertEquals("Constraint warehouses exclude = true", exception.message)
    }

    @Test
    fun promoFromDtoTest_promocode_success() {
        val promo = Promo(
            promoId = "promoId",
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
            promocode = Promocode(
                codeType = PromocodeType.FIXED_DISCOUNT,
                value = 1,
                code = "code",
                minCartPrice = 2,
                maxCartPrice = 3,
                applyMultipleTimes = true,
                additionalConditions = "additionalConditions"
            ),
            landingUrlAutogenerated = false,
            landingUrl = "landing/Page/Url",
            rulesUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url"
        )

        val promoResponse = buildBasePromoResponse()
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(null)
                    .promocode(
                        ru.yandex.mj.generated.client.promoservice.model.Promocode()
                            .codeType(ru.yandex.mj.generated.client.promoservice.model.Promocode.CodeTypeEnum.FIXED_DISCOUNT)
                            .value(1)
                            .code("code")
                            .minCartPrice(2)
                            .maxCartPrice(3)
                            .applyMultipleTimes(true)
                            .additionalConditions("additionalConditions")
                    )
            )

        Assertions.assertEquals(promo, PromoServerDtoConverter.fromDto(promoResponse))
    }

    @Test
    fun promoToDtoTest_assortmentLoadMethod_null_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            assortmentLoadMethod = null
        )

        val expected = buildBasePromoRequest()
        expected
            .src!!
            .ciface!!
            .assortmentLoadMethod(null)

        Assertions.assertEquals(expected, PromoServerDtoConverter.toDto(promo))
    }

    @Test
    fun promoToDtoTest_assortmentLoadMethod_notNull_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            assortmentLoadMethod = AssortmentLoadMethod.TRACKER
        )

        val expected = buildBasePromoRequest()
        expected
            .src!!
            .ciface!!
            .assortmentLoadMethod("TRACKER")

        Assertions.assertEquals(expected, PromoServerDtoConverter.toDto(promo))
    }

    @Test
    fun promoToDtoTest_promocode_success() {
        val promo = Promo(
            promoId = "promoId",
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
            landingUrl = "landing/Page/Url",
            landingUrlAutogenerated = false,
            rulesUrl = "rules/Page/Url",
            rulesUrlAutogenerated = false,
            promocode = Promocode(
                codeType = PromocodeType.FIXED_DISCOUNT,
                value = 1,
                code = "code",
                minCartPrice = 2,
                maxCartPrice = 3,
                applyMultipleTimes = true,
                additionalConditions = "additionalConditions"
            )
        )

        val expected = buildBasePromoRequest()
        expected
            .mechanics(
                PromoMechanicsParams()
                    .cheapestAsGift(null)
                    .promocode(
                        ru.yandex.mj.generated.client.promoservice.model.Promocode()
                            .codeType(ru.yandex.mj.generated.client.promoservice.model.Promocode.CodeTypeEnum.FIXED_DISCOUNT)
                            .value(1)
                            .code("code")
                            .minCartPrice(2)
                            .maxCartPrice(3)
                            .applyMultipleTimes(true)
                            .additionalConditions("additionalConditions")
                    )
            )

        Assertions.assertEquals(expected, PromoServerDtoConverter.toDto(promo))
    }
}
