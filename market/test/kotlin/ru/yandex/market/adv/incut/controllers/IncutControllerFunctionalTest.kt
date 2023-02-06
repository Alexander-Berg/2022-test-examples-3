package ru.yandex.market.adv.incut.controllers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import net.javacrumbs.jsonunit.JsonAssert
import net.javacrumbs.jsonunit.core.Option
import org.dbunit.database.DatabaseConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.util.UriComponentsBuilder
import ru.yandex.market.adv.incut.AbstractFunctionalTest
import ru.yandex.market.adv.incut.controllers.mapper.IncutViewMapper
import ru.yandex.market.adv.incut.integration.saas.mapper.IncutSaasDocumentMapper
import ru.yandex.market.adv.incut.integration.saas.mapper.IncutSaasMessageMapper
import ru.yandex.market.adv.incut.integration.saas.service.IncutSaasLogbrokerEvent
import ru.yandex.market.adv.incut.service.advertiser.model.Advertiser
import ru.yandex.market.adv.incut.service.advertiser.model.AdvertiserType
import ru.yandex.market.adv.incut.service.file.model.FileMeta
import ru.yandex.market.adv.incut.service.incut.model.Incut
import ru.yandex.market.adv.incut.service.incut.model.IncutBody
import ru.yandex.market.adv.incut.service.incut.model.IncutState
import ru.yandex.market.adv.incut.service.incut.model.IncutTargetPlace
import ru.yandex.market.adv.incut.service.incut.model.IncutType
import ru.yandex.market.adv.incut.service.incut.model.body.autobanner.BannerClickUrlType
import ru.yandex.market.adv.incut.service.incut.model.body.autobanner.BannerColor
import ru.yandex.market.adv.incut.service.incut.model.body.autobanner.BannerColorTheme
import ru.yandex.market.adv.incut.utils.FunctionalTestHelper
import ru.yandex.market.adv.incut.utils.convertToBase64
import ru.yandex.market.adv.incut.utils.time.toInstantAtUtc3
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import ru.yandex.market.media.adv.SaasIncuts
import ru.yandex.mj.generated.server.model.IncutViewTransition
import ru.yandex.search.saas.RTYServer
import ru.yandex.search.saas.SearchZone
import java.net.URL
import java.time.Clock
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID
import java.util.concurrent.CompletableFuture

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")
)
class IncutControllerFunctionalTest(
    @Autowired
    private val vendorPartnerMock: WireMockServer,
    @Autowired
    private val mediaAdvIncutSearchMock: WireMockServer,
    @Autowired
    private val incutSaasLogbrokerEventPublisher: LogbrokerEventPublisher<IncutSaasLogbrokerEvent>,
    @Autowired
    private val clock: Clock,
    @Autowired
    private val incutSaasDocumentMapper: IncutSaasDocumentMapper,
) : AbstractFunctionalTest() {

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutsRecommendationsForModels/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutsRecommendationsForModels/after.csv"]
    )
    fun `test incuts recommendations for models`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testIncutsRecommendationsForModels/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testIncutsRecommendationsForModels/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/recommendations")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("incutId", 1)
            .build()
            .toUriString()

        val body: String = getStringResource("/testIncutsRecommendationsForModels/requestBody.json")

        val actual: ResponseEntity<String> = FunctionalTestHelper.postForEntity(query, body)

        val expected: String = getStringResource("/testIncutsRecommendationsForModels/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutsRecommendationsForAutobanner/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutsRecommendationsForAutobanner/after.csv"]
    )
    fun `test incuts recommendations for autobanner`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testIncutsRecommendationsForAutobanner/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testIncutsRecommendationsForAutobanner/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/recommendations")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("incutId", 1)
            .build()
            .toUriString()

        val body: String = getStringResource("/testIncutsRecommendationsForAutobanner/requestBody.json")

        val actual: ResponseEntity<String> = FunctionalTestHelper.postForEntity(query, body)

        val expected: String = getStringResource("/testIncutsRecommendationsForAutobanner/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetCategoriesTree/before.csv"])
    fun `test correct categories tree body`() {

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/categories/trees")
            .queryParam("hid", 90401)
            .build()
            .toUriString()

        val actual: String = FunctionalTestHelper.get(query)

        val expected: String = getStringResource("testGetCategoriesTree/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actual,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testEmptyCategoriesTreeBody/before.csv"])
    fun `test empty categories tree body`() {

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/categories/trees")
            .queryParam("hid", 123)
            .build()
            .toUriString()

        val exception: HttpClientErrorException.NotFound =
            assertThrows(HttpClientErrorException.NotFound::class.java) { FunctionalTestHelper.get(query) }

        val expected = getStringResource("/testEmptyCategoriesTreeBody/expected.json")

        JsonAssert.assertJsonEquals(
            HttpStatus.NOT_FOUND,
            exception.statusCode,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Disabled
    @Test
    fun `test invalid body with correct type`() {

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .build()
            .toUriString()

        val body = getStringResource("testInvalidIncutBody/incut_body.json")

        val exception: HttpClientErrorException = assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.post(query, body)
        }

        val expected = getStringResource("testInvalidIncutBody/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Disabled
    @Test
    fun `test empty categories`() {

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .build()
            .toUriString()

        val body = getStringResource("testEmptyCategories/incut_body.json")

        val exception: HttpClientErrorException = assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.post(query, body)
        }

        val expected = getStringResource("testEmptyCategories/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncutSList/before.csv"])
    fun `test list filter by name`() {

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncutSList/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/list")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("page", 1)
            .queryParam("pageSize", 2)
            .queryParam("incutName", "Нкат")
            .build()
            .toUriString()

        val actually = FunctionalTestHelper.get(query)

        val expected = getStringResource("testListFilterByName/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncutSList/before.csv"])
    fun `test list filter by priority incuts`() {

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncutSList/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/list")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("page", 1)
            .queryParam("pageSize", 2)
            .queryParam("priorityIncut", "5,9")
            .build()
            .toUriString()

        val actually = FunctionalTestHelper.get(query)

        val expected = getStringResource("testListFilterByPriorityIncuts/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncutListByName/before.csv"])
    fun `test get incut list by name`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/list")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("page", 1)
            .queryParam("pageSize", 2)
            .queryParam("incutName", "Нкат")
            .build()
            .toUriString()

        val actually = FunctionalTestHelper.get(query)

        val expected = getStringResource("testGetIncutListByName/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncutSList/before.csv"])
    fun `test incuts list filter by priority incuts`() {

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncutSList/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/list")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("page", 1)
            .queryParam("pageSize", 2)
            .queryParam("priorityIncut", "6,2")
            .build()
            .toUriString()

        val actually = FunctionalTestHelper.get(query)

        val expected = getStringResource("testIncutsListFilterByPriorityIncuts/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncutSList/before.csv"])
    fun `test list filter by bid`() {

        vendorPartnerMock.stubFor(
            WireMock.get(UrlPattern.ANY)
                .willReturn(WireMock.okJson(getStringResource("testGetIncutSList/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/list")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("page", 2)
            .queryParam("pageSize", 4)
            .queryParam("bidFrom", 15)
            .queryParam("bidTo", 50)
            .build()
            .toUriString()

        val actually = FunctionalTestHelper.get(query)
        val expected = getStringResource("testListFilterByBid/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testListFilterByState/before.csv"])
    fun `test list filter by state`() {
        vendorPartnerMock.stubFor(
            WireMock.get(UrlPattern.ANY)
                .willReturn(WireMock.okJson(getStringResource("testListFilterByState/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/list")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("page", 1)
            .queryParam("pageSize", 2)
            .queryParam("incutState", "MODERATION")
            .build()
            .toUriString()

        val actually = FunctionalTestHelper.get(query)
        val expected = getStringResource("testListFilterByState/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    fun `text auto banner additional data`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/autoBanner/additional")
            .build()
            .toUriString()

        val actually: String = FunctionalTestHelper.get(query)
        val expected: String = getStringResource("testAutoBannerAdditionalData/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncutAdvertiserDoesNotExists/before.csv"]
    )
    fun `test get incut advertiser does not exists`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/1")
            .queryParam("vendorId", 1)
            .queryParam("datasourceId", 1)
            .queryParam("uid", 1186962236)
            .build()
            .toUriString()

        val exception = assertThrows(HttpClientErrorException.NotFound::class.java) {
            FunctionalTestHelper.get(query)
        }
        val expected: String = getStringResource("/testGetIncutAdvertiserDoesNotExists/expected.json")

        assertEquals(exception.statusCode, HttpStatus.NOT_FOUND)
        JsonAssert.assertJsonEquals(
            exception.responseBodyAsString,
            expected,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncutDoesNotExistsForAdvertiser/before.csv"]
    )
    fun `test get incut does not exists for advertiser`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .build()
            .toUriString()

        val exception = assertThrows(HttpClientErrorException.NotFound::class.java) {
            FunctionalTestHelper.get(query)
        }
        val expected: String = getStringResource("/testGetIncutDoesNotExistsForAdvertiser/expected.json")

        assertEquals(exception.statusCode, HttpStatus.NOT_FOUND)
        JsonAssert.assertJsonEquals(
            exception.responseBodyAsString,
            expected,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncut/before.csv"]
    )
    fun `test get incut`() {
        vendorPartnerMock.stubFor(
            WireMock.post(
                "/vendors/19708/models/report"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncut/models.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncut/categories.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/brands?brandIds=101&brandIds=102"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncut/brands.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .build()
            .toUriString()

        val response: String = FunctionalTestHelper.get(query)
        val expected: String = getStringResource("testGetIncut/expected.json")


        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncuts/before.csv"]
    )
    fun `test get incuts`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .build()
            .toUriString()

        val response: String = FunctionalTestHelper.get(query)
        val expected: String = getStringResource("testGetIncuts/expected.json")


        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testGetIncutWithModeration/before.csv"]
    )
    fun `test get incut with moderation`() {
        vendorPartnerMock.stubFor(
            WireMock.post(
                "/vendors/19708/models/report"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncutWithModeration/models.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncutWithModeration/categories.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/brands?brandIds=101&brandIds=102"
            ).willReturn(WireMock.okJson(getStringResource("testGetIncutWithModeration/brands.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .build()
            .toUriString()

        val response: String = FunctionalTestHelper.get(query)
        val expected: String = getStringResource("testGetIncutWithModeration/expected.json")


        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    //LIFECYCLE FUNCTIONAL TEST

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testModelsIncutTransitFromNewToDraftSuccess/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testModelsIncutTransitFromNewToDraftSuccess/after.csv"]
    )
    fun `test models incut transit from new to draft success`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2022, Month.MARCH,
                    17, 15, 8, 0
                ).toInstantAtUtc3()
            )

        vendorPartnerMock.stubFor(
            WireMock.post(
                "/vendors/19708/models/report"
            ).willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitFromNewToDraftSuccess/models.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            )
                .willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitFromNewToDraftSuccess/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.NEW_TO_DRAFT.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testModelsIncutTransitFromNewToDraftSuccess/requestBody.json")

        val actual: ResponseEntity<String> = FunctionalTestHelper.postForEntity(query, body)

        val expected: String = getStringResource("/testModelsIncutTransitFromNewToDraftSuccess/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testCreateIncutWithTransit/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testCreateIncutWithTransit/after.csv"]
    )
    fun `test create incut with transit`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2022, Month.MARCH,
                    17, 15, 8, 0
                ).toInstantAtUtc3()
            )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            )
                .willReturn(WireMock.okJson(getStringResource("/testCreateIncutWithTransit/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.NEW_TO_DRAFT.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testCreateIncutWithTransit/requestBody.json")

        val actual: ResponseEntity<String> = FunctionalTestHelper.postForEntity(query, body)

        val expected: String = getStringResource("/testCreateIncutWithTransit/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testModelsIncutTransitToDraftSelfSuccess/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testModelsIncutTransitToDraftSelfSuccess/after.csv"]
    )
    fun `test models incut transit to draft self success`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)
                    .toInstantAtUtc3()
            )

        vendorPartnerMock.stubFor(
            WireMock.post(
                "/vendors/19708/models/report"
            ).willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitToDraftSelfSuccess/models.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            )
                .willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitToDraftSelfSuccess/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DRAFT_SELF.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testModelsIncutTransitToDraftSelfSuccess/requestBody.json")

        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)

        val expected: String = getStringResource("/testModelsIncutTransitToDraftSelfSuccess/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testPutIncut/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testPutIncut/after.csv"]
    )
    fun `test put incuts`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)
                    .toInstantAtUtc3()
            )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DRAFT_SELF.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testPutIncut/requestBody.json")

        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)

        val expected: String = getStringResource("/testPutIncut/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            actual.body,
            expected,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testPutIncutsTransit/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testPutIncutsTransit/after.csv"]
    )
    fun `test put incuts transit`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)
                    .toInstantAtUtc3()
            )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1/transit")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DRAFT_SELF.name)
            .build()
            .toUriString()

        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query)

        val expected: String = getStringResource("/testPutIncutsTransit/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            actual.body,
            expected,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testModelsIncutTransitFromNewToDraftAllValidationsFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testModelsIncutTransitFromNewToDraftAllValidationsFailed/after.csv"]
    )
    fun `test models incut transit from new to draft all validations failed`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2022, Month.MARCH,
                    17, 15, 8, 0
                ).toInstantAtUtc3()
            )

        vendorPartnerMock.stubFor(
            WireMock.post(
                "/vendors/19708/models"
            )
                .willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitFromNewToDraftAllValidationsFailed/models.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            )
                .willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitFromNewToDraftAllValidationsFailed/categories.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.NEW_TO_DRAFT.name)
            .build()
            .toUriString()

        val body: String =
            getStringResource("/testModelsIncutTransitFromNewToDraftAllValidationsFailed/requestBody.json")

        val exception = assertThrows(HttpClientErrorException.BadRequest::class.java) {
            FunctionalTestHelper.postForEntity(query, body)

        }

        val expected: String =
            getStringResource("/testModelsIncutTransitFromNewToDraftAllValidationsFailed/expected.json")

        assertEquals(exception.statusCode, HttpStatus.BAD_REQUEST)

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testModelsIncutTransitFromActivationReadyToActiveSuccess/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testModelsIncutTransitFromActivationReadyToActiveSuccess/after.csv"]
    )
    fun `test models incut transit from activation ready to active success`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)
                    .toInstantAtUtc3()
            )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491,10498025"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitFromActivationReadyToActiveSuccess/recommendations.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.post(
                "/vendors/19708/models/report"
            )
                .willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitFromActivationReadyToActiveSuccess/models.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            )
                .willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitFromActivationReadyToActiveSuccess/categories.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/vendors/list?vendorId=19708&page=1&size=1"
            )
                .willReturn(WireMock.okJson(getStringResource("/testModelsIncutTransitFromActivationReadyToActiveSuccess/vendorsList.json")))
        )

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(null))

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incut/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.ACTIVATION_READY_TO_ACTIVATING.value)
            .build()
            .toUriString()

        val body: String =
            getStringResource("/testModelsIncutTransitFromActivationReadyToActiveSuccess/requestBody.json")

        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)

        val expected: String =
            getStringResource("/testModelsIncutTransitFromActivationReadyToActiveSuccess/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testActivatingToActiveTransitForModelsFromSearch/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testActivatingToActiveTransitForModelsFromSearch/after.csv"]
    )
    fun `test activating to active transit for models from search`() {
        val localDateTime = LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)

        Mockito.`when`(clock.instant())
            .thenReturn(
                localDateTime.toInstantAtUtc3()
            )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testActivatingToActiveTransitForModelsFromSearch/recommendations.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testActivatingToActiveTransitForModelsFromSearch/vendorList.json")))
        )

        val incut = Incut(
            1,
            "New incut test",
            IncutType.MODELS,
            25,
            1000,
            IncutState.ACTIVE,
            listOf(91491),
            listOf(91491),
            listOf(10498025),
            listOf(),
            listOf(1, 2, 3),
            listOf(),
            Advertiser(1, 19708, 28195, AdvertiserType.VENDOR),
            1,
            IncutBody(
                listOf(1429703292, 1448810179, 1448810180),
                null,
                null,
                null,
                null
            ),
            localDateTime,
            localDateTime,
            IncutTargetPlace.SEARCH,
            IncutViewMapper.CURRENT_DESIGN_VERSION
        )

        val event = getModifyDocumentLogbrokerEvent(incut, "0", "91491", true)

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(event))
            .thenReturn(CompletableFuture.completedFuture(null))

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.ACTIVATING_TO_ACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testActivatingToActiveTransitForModelsFromSearch/requestBody.json")
        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)
        val expected: String =
            getStringResource("/testActivatingToActiveTransitForModelsFromSearch/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

        Mockito.verify(incutSaasLogbrokerEventPublisher, Mockito.times(1)).publishEventAsync(event)
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testActivatingToActiveTransitForModelsFromModelCard/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testActivatingToActiveTransitForModelsFromModelCard/after.csv"]
    )
    fun `test activating to active transit for models from model card`() {
        val localDateTime = LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)

        Mockito.`when`(clock.instant())
            .thenReturn(
                localDateTime.toInstantAtUtc3()
            )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("10498025"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testActivatingToActiveTransitForModelsFromModelCard/recommendations.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testActivatingToActiveTransitForModelsFromModelCard/vendorList.json")))
        )

        val incut = Incut(
            1,
            "New incut test",
            IncutType.MODELS,
            25,
            1000,
            IncutState.ACTIVE,
            listOf(91491),
            listOf(),
            listOf(10498025),
            listOf(),
            listOf(1, 2, 3),
            listOf(),
            Advertiser(1, 19708, 28195, AdvertiserType.VENDOR),
            1,
            IncutBody(
                listOf(1429703292, 1448810179, 1448810180),
                null,
                null,
                null,
                null
            ),
            localDateTime,
            localDateTime,
            IncutTargetPlace.MODEL_CARD,
            IncutViewMapper.CURRENT_DESIGN_VERSION
        )

        val event = getModifyDocumentLogbrokerEvent(incut, "1",  "10498025", false)

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(event))
            .thenReturn(CompletableFuture.completedFuture(null))

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.ACTIVATING_TO_ACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testActivatingToActiveTransitForModelsFromModelCard/requestBody.json")
        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)
        val expected: String =
            getStringResource("/testActivatingToActiveTransitForModelsFromModelCard/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

        Mockito.verify(incutSaasLogbrokerEventPublisher, Mockito.times(1)).publishEventAsync(event)
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testActivatingToActiveTransitForAutobannerFromSearch/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testActivatingToActiveTransitForAutobannerFromSearch/after.csv"]
    )
    fun `test activating to active transit for autobanner from search`() {
        val localDateTime = LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)
        Mockito.`when`(clock.instant())
            .thenReturn(
                localDateTime.toInstantAtUtc3()
            )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testActivatingToActiveTransitForAutobannerFromSearch/recommendations.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testActivatingToActiveTransitForAutobannerFromSearch/vendorList.json")))
        )

        val incut = Incut(
            1,
            "New incut test",
            IncutType.AUTOBANNER_WITH_MODELS,
            25,
            1000,
            IncutState.ACTIVE,
            listOf(91491),
            listOf(91491),
            listOf(10498025),
            listOf(),
            listOf(1, 2, 3),
            listOf(),
            Advertiser(1, 19708, 28195, AdvertiserType.VENDOR),
            1,
            IncutBody(
                listOf(1429703292, 1448810179, 1448810180),
                FileMeta(
                    UUID.fromString("46c43a33-2b27-47d9-880c-afb5a61bda0e"),
                    "testImage.jpg",
                    localDateTime,
                    8441,
                    67282295,
                    URL("https://incut-public.s3.mdst.yandex.net/autobanner/06e1f927-d837-46a3-b380-74a3648bb00a.png"),
                    "autobanner"
                ),
                BannerClickUrlType.BRANDZONE,
                "Custom motto test",
                BannerColor(
                    1,
                    "RED_PASTEL",
                    "#FFF2E5",
                    BannerColorTheme.LIGHT
                )
            ),
            localDateTime,
            localDateTime,
            IncutTargetPlace.SEARCH,
            IncutViewMapper.CURRENT_DESIGN_VERSION
        )

        val event = getModifyDocumentLogbrokerEvent(incut, "0",  "91491", true)

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(event))
            .thenReturn(CompletableFuture.completedFuture(null))

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.ACTIVATING_TO_ACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testActivatingToActiveTransitForAutobannerFromSearch/requestBody.json")
        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)
        val expected: String =
            getStringResource("/testActivatingToActiveTransitForAutobannerFromSearch/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

        Mockito.verify(incutSaasLogbrokerEventPublisher, Mockito.times(1)).publishEventAsync(event)
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testDeactToInactiveTransitForAutobannerFromSearch/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testDeactToInactiveTransitForAutobannerFromSearch/after.csv"]
    )
    fun `test deact to inactive transit for autobanner from search`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)
                    .toInstantAtUtc3()
            )

        val event = getDeleteDocumentLogbrokerEvent()

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(event))
            .thenReturn(CompletableFuture.completedFuture(null))

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testDeactToInactiveTransitForAutobannerFromSearch/recommendations.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testDeactToInactiveTransitForAutobannerFromSearch/vendorList.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testDeactToInactiveTransitForAutobannerFromSearch/requestBody.json")
        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)
        val expected: String =
            getStringResource("/testDeactToInactiveTransitForAutobannerFromSearch/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

        Mockito.verify(incutSaasLogbrokerEventPublisher, Mockito.times(1)).publishEventAsync(event)
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testDeactToInactiveTransitForModelsFromModelCard/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testDeactToInactiveTransitForModelsFromModelCard/after.csv"]
    )
    fun `test deact to inactive transit for models from model card`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)
                    .toInstantAtUtc3()
            )

        val event = getDeleteDocumentLogbrokerEvent()

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(event))
            .thenReturn(CompletableFuture.completedFuture(null))

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("10498025"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testDeactToInactiveTransitForModelsFromModelCard/recommendations.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testDeactToInactiveTransitForModelsFromModelCard/vendorList.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testDeactToInactiveTransitForModelsFromModelCard/requestBody.json")
        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)
        val expected: String =
            getStringResource("/testDeactToInactiveTransitForModelsFromModelCard/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

        Mockito.verify(incutSaasLogbrokerEventPublisher, Mockito.atLeastOnce()).publishEventAsync(event)
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testDeactToInactiveTransitForModelsUsingDesignVersion/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testDeactToInactiveTransitForModelsUsingDesignVersion/after.csv"]
    )
    fun `test deact to inactive transit for models using design version`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(2022, Month.MARCH, 17, 15, 8, 0)
                    .toInstantAtUtc3()
            )

        val event = getDeleteDocumentLogbrokerEvent()

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(event))
            .thenReturn(CompletableFuture.completedFuture(null))

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .willReturn(WireMock.okJson(getStringResource("/testDeactToInactiveTransitForModelsUsingDesignVersion/recommendations.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testDeactToInactiveTransitForModelsUsingDesignVersion/vendorList.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testDeactToInactiveTransitForModelsUsingDesignVersion/requestBody.json")
        val actual: ResponseEntity<String> = FunctionalTestHelper.putForEntity(query, body)
        val expected: String =
            getStringResource("/testDeactToInactiveTransitForModelsUsingDesignVersion/expected.json")

        assertEquals(actual.statusCode, HttpStatus.OK)

        JsonAssert.assertJsonEquals(
            expected,
            actual.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

        Mockito.verify(incutSaasLogbrokerEventPublisher, Mockito.atLeastOnce()).publishEventAsync(event)
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testNameIsNotBlankValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testNameIsNotBlankValidationFailed/after.csv"]
    )
    fun `test name is not blank validation failed`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testNameIsNotBlankValidationFailed/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("10498025"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testNameIsNotBlankValidationFailed/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testNameIsNotBlankValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testNameIsNotBlankValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testNameIsNotTooLargeValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testNameIsNotTooLargeValidationFailed/after.csv"]
    )
    fun `test name is not too large validation failed`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testNameIsNotTooLargeValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testNameIsNotTooLargeValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testMainCategoriesIsNotEmptyValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testMainCategoriesIsNotEmptyValidationFailed/after.csv"]
    )
    fun `test main categories is not empty validation failed`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testMainCategoriesIsNotEmptyValidationFailed/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("10498025"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testMainCategoriesIsNotEmptyValidationFailed/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.ACTIVATION_READY_TO_ACTIVATING.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testMainCategoriesIsNotEmptyValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testMainCategoriesIsNotEmptyValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testBidIsNotGreaterThenExpectedValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testBidIsNotGreaterThenExpectedValidationFailed/after.csv"]
    )
    fun `test bid is not greater then expected validation failed`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testBidIsNotGreaterThenExpectedValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testBidIsNotGreaterThenExpectedValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutTypeIsNotChangedValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutTypeIsNotChangedValidationFailed/after.csv"]
    )
    fun `test incut type is not changed validation failed`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testIncutTypeIsNotChangedValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testIncutTypeIsNotChangedValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetingCategoriesForSearchAreNotSetValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetingCategoriesForSearchAreNotSetValidationFailed/after.csv"]
    )
    fun `test targeting categories for search are not set validation failed`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.ACTIVATION_READY_TO_ACTIVATING.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testTargetingCategoriesForSearchAreNotSetValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testTargetingCategoriesForSearchAreNotSetValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetingCategoriesForModelCardAreNotSetValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetingCategoriesForModelCardAreNotSetValidationFailed/after.csv"]
    )
    fun `test targeting categories for model card are not set validation failed`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.ACTIVATION_READY_TO_ACTIVATING.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testTargetingCategoriesForModelCardAreNotSetValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testTargetingCategoriesForModelCardAreNotSetValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testBidIsLessThenRecommendedMinBidValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testBidIsLessThenRecommendedMinBidValidationFailed/after.csv"]
    )
    fun `test bid is less then recommended min bid validation failed`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testBidIsLessThenRecommendedMinBidValidationFailed/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .willReturn(WireMock.okJson(getStringResource("/testBidIsLessThenRecommendedMinBidValidationFailed/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.ACTIVATION_READY_TO_ACTIVATING.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testBidIsLessThenRecommendedMinBidValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(query, body)
            }

        val expected: String = getStringResource("/testBidIsLessThenRecommendedMinBidValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testEnoughModelsForModelCardForModelsValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testEnoughModelsForModelCardForModelsValidationFailed/after.csv"]
    )
    fun `test enough models for model card for models validation failed`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testEnoughModelsForModelCardForModelsValidationFailed/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("10498025"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testEnoughModelsForModelCardForModelsValidationFailed/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testEnoughModelsForModelCardForModelsValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testEnoughModelsForModelCardForModelsValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testEnoughModelsForSearchForModelsValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testEnoughModelsForSearchForModelsValidationFailed/after.csv"]
    )
    fun `test enough models for search for models validation failed`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testEnoughModelsForSearchForModelsValidationFailed/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testEnoughModelsForSearchForModelsValidationFailed/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testEnoughModelsForSearchForModelsValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String = getStringResource("/testEnoughModelsForSearchForModelsValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetCategoriesForModelsForSearchValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetCategoriesForModelsForSearchValidationFailed/after.csv"]
    )
    fun `test target categories for models for search validation failed`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testTargetCategoriesForModelsForSearchValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String =
            getStringResource("/testTargetCategoriesForModelsForSearchValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetCategoriesForModelsForModelCardValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetCategoriesForModelsForModelCardValidationFailed/after.csv"]
    )
    fun `test target categories for models for model card validation failed`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testTargetCategoriesForModelsForModelCardValidationFailed/vendorsList.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String =
            getStringResource("/testTargetCategoriesForModelsForModelCardValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String =
            getStringResource("/testTargetCategoriesForModelsForModelCardValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testEnoughModelsForSearchForAutobannerValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testEnoughModelsForSearchForAutobannerValidationFailed/after.csv"]
    )
    fun `test enough models for search for autobanner validation failed`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testEnoughModelsForSearchForAutobannerValidationFailed/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testEnoughModelsForSearchForAutobannerValidationFailed/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String = getStringResource("/testEnoughModelsForSearchForAutobannerValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String =
            getStringResource("/testEnoughModelsForSearchForAutobannerValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetCategoriesForAutobannerForSearchValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testTargetCategoriesForAutobannerForSearchValidationFailed/after.csv"]
    )
    fun `test target categories for autobanner for search validation failed`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String =
            getStringResource("/testTargetCategoriesForAutobannerForSearchValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String =
            getStringResource("/testTargetCategoriesForAutobannerForSearchValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutBodyIsNotSetForAutobannerForSearchValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutBodyIsNotSetForAutobannerForSearchValidationFailed/after.csv"]
    )
    fun `test incut body is not set for autobanner for search validation failed`() {
        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String =
            getStringResource("/testIncutBodyIsNotSetForAutobannerForSearchValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String =
            getStringResource("/testIncutBodyIsNotSetForAutobannerForSearchValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutBodyIsChangedForAutobannerForSearchValidationFailed/before.csv"],
        after = ["/ru/yandex/market/adv/incut/controllers/IncutControllerFunctionalTest/testIncutBodyIsChangedForAutobannerForSearchValidationFailed/after.csv"]
    )
    fun `test incut body is changed for autobanner for search validation failed`() {
        vendorPartnerMock.stubFor(
            WireMock.get("/vendors/list?vendorId=19708&page=1&size=1")
                .willReturn(WireMock.okJson(getStringResource("/testIncutBodyIsChangedForAutobannerForSearchValidationFailed/vendorsList.json")))
        )

        mediaAdvIncutSearchMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/recommends"))
                .withQueryParam("target_hids", WireMock.equalTo("91491"))
                .withQueryParam("format", WireMock.equalTo("json"))
                .willReturn(WireMock.okJson(getStringResource("/testIncutBodyIsChangedForAutobannerForSearchValidationFailed/recommendations.json")))
        )

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/incuts/1")
            .queryParam("vendorId", 19708)
            .queryParam("datasourceId", 28195)
            .queryParam("uid", 1186962236)
            .queryParam("transition", IncutViewTransition.DEACTIVATING_TO_INACTIVE.name)
            .build()
            .toUriString()

        val body: String =
            getStringResource("/testIncutBodyIsChangedForAutobannerForSearchValidationFailed/requestBody.json")

        val exception: HttpClientErrorException.BadRequest =
            assertThrows(HttpClientErrorException.BadRequest::class.java) {
                FunctionalTestHelper.putForEntity(
                    query,
                    body
                )
            }

        val expected: String =
            getStringResource("/testIncutBodyIsChangedForAutobannerForSearchValidationFailed/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    private fun getModifyDocumentLogbrokerEvent(
        incut: Incut,
        pageValue: String,
        categoryIdKey: String,
        hasForeignCategory: Boolean
    ): IncutSaasLogbrokerEvent {
        val incutSaasDocument: SaasIncuts.TIncut = incutSaasDocumentMapper.mapToDocument(incut)

        val documentBuilder = RTYServer.TMessage.TDocument.newBuilder()
            .setUrl("adv_incut_1")
            .setMimeType(IncutSaasMessageMapper.DOCUMENT_MIME_TYPE)
            .setModificationTimestamp(1647518880)
            .setBody(incutSaasDocument.convertToBase64())
            .setKeyPrefix(1000)

        documentBuilder
            .addSearchAttributes(getIntegerTAttribute(IncutSaasMessageMapper.INCUT_ID_KEY, "1"))
            .addGroupAttributes(getIntegerTAttribute(IncutSaasMessageMapper.INCUT_ID_KEY, "1"))
            .addDocumentProperties(getTProperty(IncutSaasMessageMapper.INCUT_ID_KEY, "1"))

            .addSearchAttributes(getIntegerTAttribute(IncutSaasMessageMapper.BRAND_ID_KEY, "0"))
            .addGroupAttributes(getIntegerTAttribute(IncutSaasMessageMapper.BRAND_ID_KEY, "0"))
            .addDocumentProperties(getTProperty(IncutSaasMessageMapper.BRAND_ID_KEY, "0"))

            .addSearchAttributes(getIntegerTAttribute(IncutSaasMessageMapper.CATEGORY_ID_KEY, categoryIdKey))
            .addGroupAttributes(getIntegerTAttribute(IncutSaasMessageMapper.CATEGORY_ID_KEY, categoryIdKey))
            .addDocumentProperties(getTProperty(IncutSaasMessageMapper.CATEGORY_ID_KEY, categoryIdKey))

        if (hasForeignCategory) {
            documentBuilder
                .addSearchAttributes(getIntegerTAttribute(IncutSaasMessageMapper.FOREIGN_CATEGORY_ID_KEY, "10498025"))
                .addGroupAttributes(getIntegerTAttribute(IncutSaasMessageMapper.FOREIGN_CATEGORY_ID_KEY, "10498025"))
                .addDocumentProperties(getTProperty(IncutSaasMessageMapper.FOREIGN_CATEGORY_ID_KEY, "10498025"))
        }

        documentBuilder
            .addSearchAttributes(getIntegerTAttribute(IncutSaasMessageMapper.TARGET_PAGE_KEY, pageValue))
            .addGroupAttributes(getIntegerTAttribute(IncutSaasMessageMapper.TARGET_PAGE_KEY, pageValue))
            .addDocumentProperties(getTProperty(IncutSaasMessageMapper.TARGET_PAGE_KEY, pageValue))

        return IncutSaasLogbrokerEvent(
            RTYServer.TMessage.newBuilder()
                .setMessageType(RTYServer.TMessage.TMessageType.MODIFY_DOCUMENT)
                .setDocument(documentBuilder)
                .build()
        )
    }

    private fun getDeleteDocumentLogbrokerEvent(): IncutSaasLogbrokerEvent {
        return IncutSaasLogbrokerEvent(
            RTYServer.TMessage.newBuilder()
                .setMessageType(RTYServer.TMessage.TMessageType.DELETE_DOCUMENT)
                .setDocument(
                    RTYServer.TMessage.TDocument.newBuilder()
                        .setUrl("adv_incut_1")
                        .setModificationTimestamp(1647518880)
                        .setKeyPrefix(1000)
                        .build()
                )
                .build()
        )
    }

    private fun getIntegerTAttribute(
        key: String,
        value: String
    ): SearchZone.TAttribute {
        return SearchZone.TAttribute.newBuilder()
            .setName(key)
            .setValue(value)
            .setType(SearchZone.TAttribute.TAttributeType.INTEGER_ATTRIBUTE)
            .build()
    }

    private fun getTProperty(
        key: String,
        value: String
    ): RTYServer.TMessage.TDocument.TProperty {
        return RTYServer.TMessage.TDocument.TProperty.newBuilder()
            .setName(key)
            .setValue(value)
            .build()
    }
}
