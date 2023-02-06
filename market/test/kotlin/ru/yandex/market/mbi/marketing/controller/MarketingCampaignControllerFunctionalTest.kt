package ru.yandex.market.mbi.marketing.controller

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import net.javacrumbs.jsonunit.JsonAssert
import net.javacrumbs.jsonunit.core.Option
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.util.UriComponentsBuilder.fromUriString
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.marketing.FunctionalTest
import ru.yandex.market.mbi.marketing.FunctionalTestHelper
import ru.yandex.market.mbi.marketing.toInstantAtUtc3
import ru.yandex.market.mbi.marketing.toMillisAtUtc0
import ru.yandex.market.mbi.marketing.toMillisAtUtc3
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class MarketingCampaignControllerFunctionalTest(
    @Autowired val clock: Clock,
    @Autowired val mbiMock: WireMockServer
) : FunctionalTest() {

    companion object {
        const val MANAGER_UID: Int = 1
        const val ADMIN_UID: Int = 2

        const val PARTNER_ID: Int = 100500
        const val CAMPAIGN_ID: Int = 1
    }

    @BeforeEach
    internal fun setUp() {
        mbiMock.stubFor(
            get(anyUrl())
                .willReturn(okXml(getStringResource("/mbiApiResponse.xml")))
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/create_marketing_campaign_successfully_by_manager/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/create_marketing_campaign_successfully_by_manager/after.csv"]
    )
    @Test
    fun `create marketing campaign successfully by manager`() {
        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 26, 14, 23, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request = getStringResource("/create_marketing_campaign_successfully_by_manager/request.json")
        val response = FunctionalTestHelper.postForEntity(query, request)
        val expected = getStringResource("/create_marketing_campaign_successfully_by_manager/expected.json")

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            response.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/create_marketing_campaign_failed_dateFrom_is_after_dateTo/after.csv"]
    )
    @Test
    fun `create marketing campaign failed dateFrom is after dateTo`() {
        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val request = getStringResource("/create_marketing_campaign_failed_dateFrom_is_after_dateTo/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/create_marketing_campaign_failed_dateFrom_is_after_dateTo/expected.json")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/create_marketing_campaign_failed_campaign_name_is_too_long/after.csv"]
    )
    @Test
    fun `create marketing campaign failed campaign name is too long`() {
        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val request = getStringResource("/create_marketing_campaign_failed_campaign_name_is_too_long/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/create_marketing_campaign_failed_campaign_name_is_too_long/expected.json")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/create_marketing_campaign_failed_campaign_name_is_blank/after.csv"]
    )
    @Test
    fun `create marketing campaign failed campaign name is blank`() {

        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request = getStringResource("/create_marketing_campaign_failed_campaign_name_is_blank/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/create_marketing_campaign_failed_campaign_name_is_blank/expected.json")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/create_marketing_campaign_failed_sum_less_than_zero/after.csv"]
    )
    @Test
    fun `create marketing campaign failed sum less than zero`() {

        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request = getStringResource("/create_marketing_campaign_failed_sum_less_than_zero/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/create_marketing_campaign_failed_sum_less_than_zero/expected.json")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/create_marketing_campaign_failed_dateTo_is_in_past/after.csv"]
    )
    @Test
    fun `create marketing campaign failed dateTo is in past`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDate.of(2021, Month.MAY, 28).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request = getStringResource("/create_marketing_campaign_failed_dateTo_is_in_past/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/create_marketing_campaign_failed_dateTo_is_in_past/expected.json")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/create_marketing_campaign_failed_dates_not_in_single_month/after.csv"]
    )
    @Test
    fun `create marketing campaign failed dates not in single month`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDate.of(2020, Month.JANUARY, 1).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request = getStringResource("/create_marketing_campaign_failed_dates_not_in_single_month/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/create_marketing_campaign_failed_dates_not_in_single_month/expected.json")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_get_all_partner_marketing_campaigns_list/before.csv"]
    )
    fun `test get all partner marketing campaigns list`() {

        val query = fromUriString("${baseUrl()}/api/v1/partners/1/marketing/campaigns/list")
            .queryParam("uid", MANAGER_UID)
            .queryParam("page", 1)
            .queryParam("pageSize", 2)
            .queryParam("marketingServiceType", "PROMO_BANNERS")
            .queryParam("campaignStatus", "AWAITING_APPROVE,STALE,FINISHED,ACTIVE")
            .build()
            .toUriString()

        val expected = getStringResource("/test_get_all_partner_marketing_campaigns_list/expected.json")
        val response = FunctionalTestHelper.get(query)

        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_get_all_partner_marketing_campaigns_list/before.csv"]
    )
    fun `test get all partner marketing campaigns list for the period`() {

        val dateEndsAfter = LocalDate.of(2021,2,15).toMillisAtUtc0()
        val dateStartsBefore = LocalDate.of(2021,4,15).toMillisAtUtc0()

        val query = fromUriString("${baseUrl()}/api/v1/partners/1/marketing/campaigns/list")
            .queryParam("uid", MANAGER_UID)
            .queryParam("page", 1)
            .queryParam("pageSize", 20)
            .queryParam("campaignStatus", "AWAITING_APPROVE,STALE,FINISHED,ACTIVE")
            .queryParam("campaignStartsBefore", dateStartsBefore)
            .queryParam("campaignEndsAfter",dateEndsAfter)
            .build()
            .toUriString()

        val expected = getStringResource("/test_get_all_partner_marketing_campaigns_list/expected_for_period.json")
        val response = FunctionalTestHelper.get(query)

        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_get_all_partner_marketing_campaigns_list/before.csv"]
    )
    fun `test get partner marketing campaigns list by one state`() {
        val queryOneStatus = fromUriString("${baseUrl()}/api/v1/partners/1/marketing/campaigns/list")
            .queryParam("uid", MANAGER_UID)
            .queryParam("page", 1)
            .queryParam("marketingServiceType", "PROMO_BANNERS")
            .queryParam("campaignStatus", "STALE")
            .build()
            .toUriString()
        val expectedOneStatus =
            getStringResource("/test_get_all_partner_marketing_campaigns_list/expected_one_status.json")
        val responseOneStatus = FunctionalTestHelper.get(queryOneStatus)

        JsonAssert.assertJsonEquals(
            expectedOneStatus,
            responseOneStatus,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_get_all_partner_marketing_campaigns_list/before.csv"]
    )
    fun `test get active campaigns`() {

        val query = fromUriString("${baseUrl()}/api/v1/partners/1/marketing/campaigns/list")
            .queryParam("uid", MANAGER_UID)
            .queryParam("campaignStatus", "ACTIVE")
            .build()
            .toUriString()
        val response = FunctionalTestHelper.get(query)

        val expected = getStringResource("/test_get_active_campaigns/expected.json")
        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_get_all_partner_marketing_campaigns_list/before.csv"]
    )
    fun `test find campaign by name`() {

        val query = fromUriString("${baseUrl()}/api/v1/partners/1/marketing/campaigns/list")
            .queryParam("uid", MANAGER_UID)
            .queryParam("campaignName", "Лучшие")
            .build()
            .toUriString()
        val response = FunctionalTestHelper.get(query)

        val expected = getStringResource("/test_find_campaign_by_name/expected.json")
        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_get_all_partner_marketing_campaigns_list/before.csv"]
    )
    fun `test dates filter`() {

        val from = LocalDate.of(2021, 2, 28).toMillisAtUtc3()
        val to = LocalDate.of(2021, 4, 2).toMillisAtUtc3()
        val query = fromUriString("${baseUrl()}/api/v1/partners/1/marketing/campaigns/list")
            .queryParam("uid", MANAGER_UID)
            .queryParam("campaignStartDate", from)
            .queryParam("campaignEndDate", to)
            .build()
            .toUriString()
        val response = FunctionalTestHelper.get(query)

        val expected = getStringResource("/test_dates_filter/expected.json")
        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_get_all_partner_marketing_campaigns_list/before.csv"]
    )
    fun `test find campaign by id`() {

        val query = fromUriString("${baseUrl()}/api/v1/partners/1/marketing/campaigns/1")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val response = FunctionalTestHelper.get(query)
        val expected = getStringResource("/test_find_campaign_by_id/expected.json")
        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_get_all_partner_marketing_campaigns_list/before.csv"]
    )
    fun `test entity not found exception`() {

        val query = fromUriString("${baseUrl()}/api/v1/partners/1/marketing/campaigns/100")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.get(query)
        }

        val expected = getStringResource("/test_entity_not_found_exception/expected.json")

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_successfully_by_manager_with_reapprove/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_successfully_by_manager_with_reapprove/after.csv"]
    )
    @Test
    fun `change marketing campaign successfully by manager with reapprove`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 23, 14, 55, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns/$CAMPAIGN_ID")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request =
            getStringResource("/change_marketing_campaign_successfully_by_manager_with_reapprove/request.json")
        val response = FunctionalTestHelper.putForEntity(query, request)
        val expected =
            getStringResource("/change_marketing_campaign_successfully_by_manager_with_reapprove/expected.json")

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            response.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_successfully_by_manager_without_reapprove/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_successfully_by_manager_without_reapprove/after.csv"]
    )
    @Test
    fun `change marketing campaign successfully by manager without reapprove`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 23, 14, 55, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns/$CAMPAIGN_ID")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request =
            getStringResource("/change_marketing_campaign_successfully_by_manager_without_reapprove/request.json")
        val response = FunctionalTestHelper.putForEntity(query, request)
        val expected =
            getStringResource("/change_marketing_campaign_successfully_by_manager_without_reapprove/expected.json")

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            response.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_failed_by_manager_not_exists/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_failed_by_manager_not_exists/after.csv"]
    )
    @Test
    fun `change marketing campaign failed by manager not exists`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 24, 14, 55, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns/100")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request = getStringResource("/change_marketing_campaign_failed_by_manager_not_exists/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.putForEntity(query, request)
        }
        val expected = getStringResource("/change_marketing_campaign_failed_by_manager_not_exists/expected.json")

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_failed_by_manager_wrong_state/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_failed_by_manager_wrong_state/after.csv"]
    )
    @Test
    fun `change marketing campaign failed by manager wrong state`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 24, 14, 55, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns/100")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()
        val request = getStringResource("/change_marketing_campaign_failed_by_manager_wrong_state/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.putForEntity(query, request)
        }
        val expected = getStringResource("/change_marketing_campaign_failed_by_manager_wrong_state/expected.json")

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/activate_marketing_campaign_successfully_by_partner/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/activate_marketing_campaign_successfully_by_partner/after.csv"]
    )
    @Test
    fun `activate marketing campaign successfully by partner`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 26, 14, 55, 3).toInstantAtUtc3())

        val query =
            fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns/$CAMPAIGN_ID/approval")
                .queryParam("uid", ADMIN_UID)
                .build()
                .toUriString()
        val response = FunctionalTestHelper.postForEntity(query)
        val expected = getStringResource("/activate_marketing_campaign_successfully_by_partner/expected.json")

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        JsonAssert.assertJsonEquals(expected, response.body, JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER))
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/schedule_marketing_campaign_successfully_by_partner/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/schedule_marketing_campaign_successfully_by_partner/after.csv"]
    )
    @Test
    fun `schedule marketing campaign successfully by partner`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 23, 15, 55, 3).toInstantAtUtc3())

        val query =
            fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns/$CAMPAIGN_ID/approval")
                .queryParam("uid", ADMIN_UID)
                .build()
                .toUriString()
        val response = FunctionalTestHelper.postForEntity(query)
        val expected = getStringResource("/schedule_marketing_campaign_successfully_by_partner/expected.json")

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        JsonAssert.assertJsonEquals(expected, response.body, JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER))
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/activate_marketing_campaign_failed_not_in_correct_state/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/activate_marketing_campaign_failed_not_in_correct_state/after.csv"]
    )
    @Test
    fun `activate marketing campaign failed not in correct state`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 24, 14, 55, 3).toInstantAtUtc3())

        val query =
            fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns/$CAMPAIGN_ID/approval")
                .queryParam("uid", ADMIN_UID)
                .build()
                .toUriString()

        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query)
        }
        val expected = getStringResource("/activate_marketing_campaign_failed_not_in_correct_state/expected.json")

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/activate_marketing_campaign_failed_overdue/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/activate_marketing_campaign_failed_overdue/after.csv"]
    )
    @Test
    fun `activate marketing campaign failed overdue`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.JUNE, 24, 14, 55, 3).toInstantAtUtc3())

        val query =
            fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns/$CAMPAIGN_ID/approval")
                .queryParam("uid", ADMIN_UID)
                .build()
                .toUriString()

        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query)
        }
        val expected = getStringResource("/activate_marketing_campaign_failed_overdue/expected.json")

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/activate_marketing_campaign_failed_no_manage_approve/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/activate_marketing_campaign_failed_no_manage_approve/after.csv"]
    )
    @Test
    fun `activate marketing campaign failed no manager approve`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 24, 14, 55, 3).toInstantAtUtc3())

        val query =
            fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns/$CAMPAIGN_ID/approval")
                .queryParam("uid", ADMIN_UID)
                .build()
                .toUriString()

        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query)
        }
        val expected = getStringResource("/activate_marketing_campaign_failed_no_manage_approve/expected.json")

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/delete_existing_campaign/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/delete_existing_campaign/after.csv"]
    )
    fun `delete existing campaign`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 26, 14, 0, 0).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns/2")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val response = FunctionalTestHelper.deleteForEntity(query)

        val expected = getStringResource("/delete_existing_campaign/expected.json")

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            response.body,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/delete_not_existing_campaign/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/delete_not_existing_campaign/after.csv"]
    )
    fun `delete not existing campaign`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 26, 14, 0, 0).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns/100")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.deleteForEntity(query)
        }

        val expected = getStringResource("/delete_not_existing_campaign/expected.json")

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/delete_campaign_with_wrong_state/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/delete_campaign_with_wrong_state/after.csv"]
    )
    fun `delete campaign with wrong state`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 26, 14, 0, 0).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns/1")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.deleteForEntity(query)
        }

        val expected = getStringResource("/delete_campaign_with_wrong_state/expected.json")

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_create_campaign_with_exists_anaplanId/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_create_campaign_with_exists_anaplanId/after.csv"]
    )
    fun `test create campaign with exists anaplanId`() {

        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(2020, Month.MAY, 26, 14, 0, 0)
                    .toInstantAtUtc3()
            )

        val query = fromUriString("${baseUrl()}/api/v1/partners/${PARTNER_ID}/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val request = getStringResource("/test_create_campaign_with_exists_anaplanId/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/test_create_campaign_with_exists_anaplanId/expected.json")

        print(exception.responseBodyAsString)
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_failed_by_existing_anaplan_id/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/change_marketing_campaign_failed_by_existing_anaplan_id/after.csv"]
    )
    @Test
    fun `change marketing campaign failed by existing anaplanId`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 24, 14, 55, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns/1")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val request = getStringResource("/change_marketing_campaign_failed_by_existing_anaplan_id/request.json")
        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.putForEntity(query, request)
        }
        val expected = getStringResource("/change_marketing_campaign_failed_by_existing_anaplan_id/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_anaplan_id_is_null/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_anaplan_id_is_null/after.csv"]
    )
    @Test
    fun `test anaplan id is null`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 24, 14, 55, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val request = getStringResource("/test_anaplan_id_is_null/request.json")

        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/test_anaplan_id_is_null/expected.json")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_cashback_campaign_two_months/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/controller/MarketingCampaignControllerFunctionalTest/test_cashback_campaign_two_months/after.csv"]
    )
    @Test
    fun `test cashback campaign two months`() {

        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 24, 14, 55, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val request = getStringResource("/test_cashback_campaign_two_months/request.json")
        FunctionalTestHelper.postForEntity(query, request)
    }

    @Test
    fun `test create fixed campaign with anaplan id`() {
        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 24, 14, 55, 3).toInstantAtUtc3())

        val query = fromUriString("${baseUrl()}/api/v1/partners/$PARTNER_ID/marketing/campaigns")
            .queryParam("uid", MANAGER_UID)
            .build()
            .toUriString()

        val request = getStringResource("/test_create_fixed_campaign_with_anaplan_id/request.json")

        val exception: HttpClientErrorException = Assertions.assertThrows(HttpClientErrorException::class.java) {
            FunctionalTestHelper.postForEntity(query, request)
        }
        val expected = getStringResource("/test_create_fixed_campaign_with_anaplan_id/expected.json")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        JsonAssert.assertJsonEquals(
            expected,
            exception.responseBodyAsString,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }
}
