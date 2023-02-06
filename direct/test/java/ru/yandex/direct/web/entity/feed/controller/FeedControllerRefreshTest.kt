package ru.yandex.direct.web.entity.feed.controller

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.direct.core.entity.feed.model.Source
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.testing.MockMvcCreator
import ru.yandex.direct.core.testing.data.TestFeeds
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.common.getValue
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.feed.service.FeedWebService
import java.time.LocalDateTime

private const val METHOD_PATH = "/feed/refresh_feed"

@DirectWebTest
@RunWith(SpringRunner::class)
class FeedControllerRefreshTest {
    @Autowired
    private lateinit var feedWebService: FeedWebService

    @Autowired
    private lateinit var mockMvcCreator: MockMvcCreator

    @Autowired
    lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var feedRepository: FeedRepository

    private lateinit var feedController: FeedController
    private lateinit var mockMvc: MockMvc
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()

        feedController = FeedController(directWebAuthenticationSource, feedWebService)
        mockMvc = mockMvcCreator.setup(feedController).build()

        testAuthHelper.setSubjectUser(clientInfo.uid)
        testAuthHelper.setOperator(clientInfo.uid)
        testAuthHelper.setSecurityContext()
    }

    @Test
    fun refreshFeed_success() {
        val lastChange = LocalDateTime.now().withNano(0).minusDays(1)
        val feedInfo = steps.feedSteps().createFeed(FeedInfo()
            .withFeed(TestFeeds.defaultFeed(null).withUpdateStatus(UpdateStatus.DONE).withLastChange(lastChange))
            .withClientInfo(clientInfo))

        val params = mapOf(
            "feed_id" to feedInfo.feedId.toString()
        )
        val answer = sendRequest(params)

        val success: Boolean = answer.get("success").asBoolean()
        val feedId = answer.get("result").get("feed_id").asLong()
        val actualFeed = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, mutableListOf(feedInfo.feedId))[0]
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(feedId).`as`("feedId").isEqualTo(feedInfo.feedId)

            it.assertThat(actualFeed.updateStatus).isEqualTo(UpdateStatus.NEW)
            it.assertThat(actualFeed.lastChange).isNotEqualTo(lastChange)
        }
    }

    @Test
    fun refreshFeed_withFileSource_failure() {
        val lastChange = LocalDateTime.now().withNano(0).minusDays(1)
        val feedInfo = steps.feedSteps().createFeed(FeedInfo()
            .withFeed(TestFeeds.defaultFeed(null).withUpdateStatus(UpdateStatus.DONE).withLastChange(lastChange).withSource(Source.FILE))
            .withClientInfo(clientInfo))

        val params = mapOf(
            "feed_id" to feedInfo.feedId.toString()
        )
        val answer = sendRequest(params)

        val success: Boolean = answer.get("success").asBoolean()
        val errorCode: String = answer.getValue("validation_result/errors/0/code")
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isFalse
            it.assertThat(errorCode).`as`("errorCode").isEqualTo("FeedDefectIds.Gen.FEED_WITHOUT_URL_SOURCE_CANNOT_BE_REFRESHED")
        }
    }

    @Test
    fun refreshFeed_withLastChangeLessThanPeriodHours_failure() {
        val lastChange = LocalDateTime.now().withNano(0)
        val feedInfo = steps.feedSteps().createFeed(FeedInfo()
            .withFeed(TestFeeds.defaultFeed(null).withUpdateStatus(UpdateStatus.DONE).withLastChange(lastChange))
            .withClientInfo(clientInfo))

        val params = mapOf(
            "feed_id" to feedInfo.feedId.toString()
        )
        val answer = sendRequest(params)

        val success: Boolean = answer.get("success").asBoolean()
        val errorCode: String = answer.getValue("validation_result/errors/0/code")
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isFalse
            it.assertThat(errorCode).`as`("errorCode")
                .isEqualTo("FeedDefectIds.Number.FEED_CANNOT_BE_REFRESHED_MORE_OFTEN_THAN_REFRESH_HOURS_PERIOD")
        }
    }

    @Test
    fun refreshFeed_withFeedNotExist_failure() {
        val lastChange = LocalDateTime.now().withNano(0).minusDays(1)
        val feedInfo = steps.feedSteps().createFeed(FeedInfo()
            .withFeed(TestFeeds.defaultFeed(null).withUpdateStatus(UpdateStatus.DONE).withLastChange(lastChange))
            .withClientInfo(clientInfo))

        val params = mapOf(
            "feed_id" to (feedInfo.feedId + 1).toString()
        )
        val answer = sendRequest(params)

        val success: Boolean = answer.get("success").asBoolean()
        val errorCode: String = answer.getValue("validation_result/errors/0/code")
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isFalse
            it.assertThat(errorCode).`as`("errorCode").isEqualTo("AdGroupDefectIds.ModelId.FEED_NOT_EXIST")
        }
    }

    private fun sendRequest(params: Map<String, String>): JsonNode {
        val mvm = LinkedMultiValueMap<String, String>()
        params.forEach { mvm.add(it.key, it.value) }

        val requestBuilder = MockMvcRequestBuilders.multipart(METHOD_PATH)
        requestBuilder.accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        requestBuilder.params(mvm)

        val result = mockMvc.perform(requestBuilder).andReturn()
        check(result.response.status == 200) { "Unexpected response status" }
        return JsonUtils.fromJson(result.response.contentAsString)
    }
}
