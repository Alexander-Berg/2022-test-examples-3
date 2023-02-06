package ru.yandex.market.pricingmgmt.service.promo

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.client.promo.api.PromoApiClient
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoSearchRequest
import ru.yandex.market.pricingmgmt.model.promo.PromoSearchResultItem
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.PromosSearchResult
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import ru.yandex.mj.generated.client.promoservice.model.MechanicsType
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2Sort
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchRequestDtoV2SrcCiface
import ru.yandex.mj.generated.client.promoservice.model.PromoSearchResultItemSrcCiface
import ru.yandex.mj.generated.client.promoservice.model.SourceType
import java.time.OffsetDateTime

class PromoSearchServiceTest : AbstractFunctionalTest() {
    @MockBean
    private val promoApiClient: PromoApiClient? = null

    @Autowired
    private var promoSearchService: PromoSearchService? = null


    @Test
    fun successfulSearchPromo() {
        val searchResultsFromService = ru.yandex.mj.generated.client.promoservice.model.PromoSearchResult().promos(
            listOf(
                //region init searchResultsFromService
                ru.yandex.mj.generated.client.promoservice.model.PromoSearchResultItem()
                    .promoId("promoId")
                    .mechanicsType(PromoMechanicsType.DIRECT_DISCOUNT.clientValue)
                    .parentPromoId("parentPromoId")
                    .name("name")
                    .status(PromoStatus.NEW.code)
                    .startAt(OffsetDateTime.parse("2017-12-03T09:15:30Z").toEpochSecond())
                    .endAt(OffsetDateTime.parse("2018-12-03T09:15:30Z").toEpochSecond())
                    .productsCount1p(100)
                    .productsCount3p(40)
                    .active(true).srcCiface(
                        PromoSearchResultItemSrcCiface()
                            .promoKind(PromoKind.CATEGORY.name)
                            .author("author")
                            .tradeManager("tradeManager")
                            .departments(listOf("department", "department1"))
                            .supplierType(SupplierType.FIRST_PARTY.value)
                            .compensationSource(Compensation.MARKET.name)
                            .promotionBudgetFact(500L)
                            .assortmentLoadMethod("PI")
                    )
                //endregion
            )
        ).totalCount(10)
        val searchRequestToService = PromoSearchRequestDtoV2()
            //region init searchRequestToService
            .pageSize(10).pageNumber(1)
            .parentPromoId(listOf("parentPromoId", "parentPromoId1"))
            .mechanicsType(listOf(MechanicsType.CHEAPEST_AS_GIFT, MechanicsType.GENERIC_BUNDLE))
            .startAtFrom(OffsetDateTime.parse("2017-12-02T09:15:30Z").toEpochSecond())
            .startAtTo(OffsetDateTime.parse("2017-12-04T09:15:30Z").toEpochSecond())
            .endAtFrom(OffsetDateTime.parse("2018-12-02T09:15:30Z").toEpochSecond())
            .endAtTo(OffsetDateTime.parse("2018-12-04T09:15:30Z").toEpochSecond())            .updatedAtFrom(OffsetDateTime.parse("2016-12-03T09:15:30Z").toEpochSecond())
            .updatedAtTo(OffsetDateTime.parse("2018-12-03T09:15:30Z").toEpochSecond())
            .srcCiface(
                PromoSearchRequestDtoV2SrcCiface()
                    .author(listOf("author", "author1"))
                    .promoKind(listOf("NATIONAL", "CROSS_CATEGORY"))
                    .department(listOf("department", "department1"))
                    .tradeManager(listOf("tradeManager", "tradeManager1"))
                    .supplierType(listOf("1P"))
                    .assortmentLoadMethod(listOf("PI", "TRACKER"))
            )
            .status(
                listOf(
                    ru.yandex.mj.generated.client.promoservice.model.PromoStatus.NEW,
                    ru.yandex.mj.generated.client.promoservice.model.PromoStatus.READY,
                )
            )
            .sourceType(listOf(SourceType.CATEGORYIFACE))
            .sort(
                listOf(
                    PromoSearchRequestDtoV2Sort()
                        .field(PromoSearchRequestDtoV2Sort.FieldEnum.PROMOID)
                        .direction(PromoSearchRequestDtoV2Sort.DirectionEnum.DESC)
                )
            )
        //endregion
        Mockito.doReturn(searchResultsFromService).`when`(promoApiClient)?.searchPromo(notNull())

        val actualResult = promoSearchService?.findPromos(buildPromoSearchRequest())
        val expectedResult = PromosSearchResult(
            promos = listOf(
                //region init expectedResult
                PromoSearchResultItem(
                    promoId = "promoId",
                    name = "name",
                    promoKind = PromoKind.CATEGORY,
                    mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT,
                    parentPromoId = "parentPromoId",
                    startDate = OffsetDateTime.parse("2017-12-03T09:15:30Z").toEpochSecond(),
                    endDate = OffsetDateTime.parse("2018-12-03T09:15:30Z").toEpochSecond(),
                    author = "author",
                    tradeManager = "tradeManager",
                    departments = listOf("department", "department1"),
                    supplierType = SupplierType.FIRST_PARTY,
                    compensationSource = Compensation.MARKET,
                    status = PromoStatus.NEW,
                    active = true,
                    ssku1pCount = 100,
                    ssku3pCount = 40,
                    totalBudget = 500L,
                    assortmentLoadMethod = AssortmentLoadMethod.PI
                )
                //endregion
            ), totalCount = 10
        )
        Mockito.verify(promoApiClient)?.searchPromo(searchRequestToService)
        Assertions.assertEquals(expectedResult, actualResult)
    }

    @Test
    fun wrongRequestForPromoSearch_promoKind() {
        val promoSearchRequest = PromoSearchRequest(
            promoKind = listOf(PromoKind.UNKNOWN)
        )
        executeRequestAndCheckMessage(promoSearchRequest, "Вид промо не распознан")
    }


    @Test
    fun wrongRequestForPromoSearch_mechanicsType() {

        val promoSearchRequest = PromoSearchRequest(
            mechanicsType = listOf(PromoMechanicsType.UNKNOWN)
        )
        executeRequestAndCheckMessage(promoSearchRequest, "Тип механики не распознан")
    }

    @Test
    fun wrongRequestForPromoSearch_supplierType() {

        val promoSearchRequest = PromoSearchRequest(
            supplierType = listOf(SupplierType.UNKNOWN),
        )
        executeRequestAndCheckMessage(promoSearchRequest, "Тип поставщика не распознан")
    }

    @Test
    fun wrongRequestForPromoSearch_status() {

        val promoSearchRequest = PromoSearchRequest(
            status = listOf(PromoStatus.UNKNOWN),
        )
        executeRequestAndCheckMessage(promoSearchRequest, "Статус не распознан")
    }

    private fun executeRequestAndCheckMessage(promoSearchRequest: PromoSearchRequest, expectedMessage: String) {
        var errorMessage: String? = null
        try {
            promoSearchService?.findPromos(promoSearchRequest)
        } catch (e: ValidationException) {
            errorMessage = e.message
        }
        Assertions.assertEquals(expectedMessage, errorMessage)
    }

    private fun buildPromoSearchRequest(): PromoSearchRequest {
        return PromoSearchRequest(
            limit = 10,
            page = 1,
            author = listOf("author", "author1"),
            promoKind = listOf(PromoKind.NATIONAL, PromoKind.CROSS_CATEGORY),
            department = listOf("department", "department1"),
            parentPromoId = listOf("parentPromoId", "parentPromoId1"),
            mechanicsType = listOf(PromoMechanicsType.CHEAPEST_AS_GIFT, PromoMechanicsType.GENERIC_BUNDLE),
            tradeManager = listOf("tradeManager", "tradeManager1"),
            supplierType = listOf(SupplierType.FIRST_PARTY),
            startDateFrom = OffsetDateTime.parse("2017-12-02T09:15:30Z").toEpochSecond(),
            startDateTo = OffsetDateTime.parse("2017-12-04T09:15:30Z").toEpochSecond(),
            endDateFrom = OffsetDateTime.parse("2018-12-02T09:15:30Z").toEpochSecond(),
            endDateTo = OffsetDateTime.parse("2018-12-04T09:15:30Z").toEpochSecond(),
            status = listOf(PromoStatus.NEW, PromoStatus.READY),
            updatedFrom = OffsetDateTime.parse("2016-12-03T09:15:30Z").toEpochSecond(),
            updatedTo = OffsetDateTime.parse("2018-12-03T09:15:30Z").toEpochSecond(),
            assortmentLoadMethod = listOf(AssortmentLoadMethod.PI, AssortmentLoadMethod.TRACKER)
        )
    }
}
