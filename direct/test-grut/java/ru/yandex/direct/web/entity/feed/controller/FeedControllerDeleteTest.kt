package ru.yandex.direct.web.entity.feed.controller

import com.fasterxml.jackson.annotation.JsonProperty
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
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.testing.MockMvcCreator
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.utils.JsonUtils.fromJson
import ru.yandex.direct.utils.JsonUtils.toJson
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.feed.service.FeedWebService

private const val METHOD_PATH = "/feed/delete_feed"

@GrutDirectWebTest
@RunWith(SpringRunner::class)
class FeedControllerDeleteTest {
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

    data class DeleteFeedRequest(@JsonProperty(value = "feed_ids") val feedIds: List<Long>)

    private fun toHttpServletRequestBuilder(request: DeleteFeedRequest) =
        MockMvcRequestBuilders.post(METHOD_PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(toJson(request))

    @Test
    fun deleteFeed_success() {
        val feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)

        val request = DeleteFeedRequest(listOf(feedInfo.feedId))
        val answer = sendRequest(request)

        val success: Boolean = answer.get("success").asBoolean()
        val actualFeeds = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, mutableListOf(feedInfo.feedId))
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(actualFeeds).`as`("actualFeeds").isEmpty()
        }
    }

    @Test
    fun deleteFeed_whenFeedUsed_failure() {
        val filterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter(clientInfo)

        val request = DeleteFeedRequest(listOf(filterInfo.feedId))
        val answer = sendRequest(request)

        val success: Boolean = answer.get("success").asBoolean()
        val errorCode = answer.get("validation_result").get("errors").get(0).get("code").asText()
        val path = answer.get("validation_result").get("errors").get(0).get("path").asText()
        val errorFeedId = answer.get("validation_result").get("errors").get(0).get("value").asLong()
        val actualFeeds = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, mutableListOf(filterInfo.feedId))
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isFalse
            it.assertThat(errorCode).`as`("errorCode").isEqualTo("FeedDefectIds.Gen.FEED_USED_IN_AD_GROUP")
            it.assertThat(path).`as`("path").isEqualTo("[0]")
            it.assertThat(errorFeedId).`as`("errorFeedId").isEqualTo(filterInfo.feedId)
            it.assertThat(actualFeeds).`as`("actualFeeds").isNotEmpty()
        }
    }

    @Test
    fun deleteFeed_whenOneOfTwo() {
        val unusedFeedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId
        val usedFeedId = steps.performanceFilterSteps().createDefaultPerformanceFilter(clientInfo).feedId

        val request = DeleteFeedRequest(listOf(unusedFeedId, usedFeedId))
        val answer = sendRequest(request)

        val success: Boolean = answer.get("success").asBoolean()
        val result: List<Long> = answer.get("result").map { it.longValue() }
        val errorCode = answer.get("validation_result").get("errors").get(0).get("code").asText()
        val path = answer.get("validation_result").get("errors").get(0).get("path").asText()
        val errorFeedId = answer.get("validation_result").get("errors").get(0).get("value").asLong()
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(result).`as`("result").isEqualTo(listOf(unusedFeedId))
            it.assertThat(errorCode).`as`("errorCode").isEqualTo("FeedDefectIds.Gen.FEED_USED_IN_AD_GROUP")
            it.assertThat(path).`as`("path").isEqualTo("[1]")
            it.assertThat(errorFeedId).`as`("errorFeedId").isEqualTo(usedFeedId)
        }
    }

    private fun sendRequest(request: DeleteFeedRequest): JsonNode {
        val content = toJson(request)
        val mockHttpServletRequestBuilder = MockMvcRequestBuilders.post(METHOD_PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(content)
        val result = mockMvc.perform(mockHttpServletRequestBuilder).andReturn()
        check(result.response.status == 200) { "Unexpected response status" }
        return fromJson(result.response.contentAsString)
    }

}
