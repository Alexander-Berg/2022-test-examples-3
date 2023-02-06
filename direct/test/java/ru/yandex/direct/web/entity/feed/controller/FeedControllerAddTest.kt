package ru.yandex.direct.web.entity.feed.controller

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.client.model.ClientLimitsBase.FEEDS_COUNT_LIMIT
import ru.yandex.direct.core.entity.feed.model.BusinessType.RETAIL
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.Source.*
import ru.yandex.direct.core.entity.feed.model.UpdateStatus.NEW
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.testing.MockMvcCreator
import ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.gemini.GeminiClient
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripDomainTail
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripProtocol
import ru.yandex.direct.market.client.MarketClient
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.utils.JsonUtils.fromJson
import ru.yandex.direct.web.common.getValue
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.feed.service.FeedWebService
import java.util.concurrent.TimeUnit.DAYS

private const val METHOD_PATH = "/feed/add_feed"

@DirectWebTest
@RunWith(SpringRunner::class)
class FeedControllerAddTest {
    @Autowired
    private lateinit var feedWebService: FeedWebService

    @Autowired
    private lateinit var marketClient: MarketClient

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

    @Autowired
    private lateinit var geminiClient: GeminiClient

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private val secondsInDay = DAYS.toSeconds(1)
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
        ppcPropertiesSupport.set(PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE,
            PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE.type.serialize(emptySet()))
    }

    @Test
    fun addUrlFeed_success() {
        val name = "feed name"
        val url = "https://yandex.ru/"
        val login = "feed_login"
        val password = "feed_password"
        val isRemoveUtm = true

        val expectedFeed = Feed().apply {
            this.name = name
            this.url = url
            this.login = login
            plainPassword = password
            businessType = RETAIL
            source = URL
            this.isRemoveUtm = isRemoveUtm
            this.updateStatus = NEW
            this.refreshInterval = secondsInDay
        }

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.SEND_FEEDS_TO_MBI_ALLOWED, false)
        val params = mapOf(
            "name" to name, "source" to "URL", "business_type" to "RETAIL", "url" to url,
            "login" to login, "password" to password, "is_remove_utm" to isRemoveUtm.toString().toLowerCase()
        )
        val answer = sendRequest(params, null)

        val success = answer.get("success").asBoolean()
        val feedId = answer.get("result").get("feed_id").asLong()
        val actualFeed = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, mutableListOf(feedId))[0]
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(actualFeed).`as`("actualFeed")
                .`is`(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())))
        }
    }

    @Test
    fun addFileFeed_success() {
        val name = "feed name"
        val fileName = "feed.tsv"
        val fileContent = ByteArray(3) { 32 } // Пробелы

        val expectedFeed = Feed().apply {
            this.name = name
            this.filename = filename
            businessType = RETAIL
            source = FILE
            this.updateStatus = NEW
            this.refreshInterval = 0L
        }

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.SEND_FEEDS_TO_MBI_ALLOWED, false)
        val multipartFile = MockMultipartFile("file", fileName, "text/tab-separated-values", fileContent)
        val params = mapOf("name" to name, "source" to "FILE", "business_type" to "RETAIL")
        val answer = sendRequest(params, multipartFile)

        val success = answer.get("success").asBoolean()
        val feedId = answer.get("result").get("feed_id").asLong()
        val actualFeed = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, mutableListOf(feedId))[0]
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(actualFeed).`as`("actualFeed")
                .`is`(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())))
        }
    }

    data class MarketClientArgument<T>(var feedInfo: T? = null)

    @Test
    fun addFileFeed_whenWrongName_failure() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.SEND_FEEDS_TO_MBI_ALLOWED, false)

        val params = mapOf(
            "name" to "",
            "source" to "URL",
            "business_type" to "RETAIL",
            "url" to "https://yandex.ru/"
        )
        val answer = sendRequest(params, null)

        val success: Boolean = answer.getValue("success")
        val errorCode: String = answer.getValue("validation_result/errors/0/code")
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isFalse
            it.assertThat(errorCode).`as`("errorCode")
                .isEqualTo("FeedDefectIds.Gen.FEED_NAME_CANNOT_BE_EMPTY")
        }
    }

    @Test
    fun addFileFeed_whenFeedLimit_failure() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.SEND_FEEDS_TO_MBI_ALLOWED, false)
        steps.clientSteps().setClientLimit(clientInfo, FEEDS_COUNT_LIMIT, 1L)
        steps.feedSteps().createDefaultFeed(clientInfo)

        val params = mapOf(
            "name" to "feed name",
            "source" to "URL",
            "business_type" to "RETAIL",
            "url" to "https://yandex.ru/"
        )
        val answer = sendRequest(params, null)

        val success: Boolean = answer.getValue("success")
        val errorCode: String = answer.getValue("validation_result/errors/0/code")
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isFalse
            it.assertThat(errorCode).`as`("errorCode")
                .isEqualTo("CollectionDefectIds.Size.MAX_ELEMENTS_EXCEEDED")
        }
    }

    @Test
    fun addFileFeed_whenFeedLimitButFeedsWereSynced_success() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.SEND_FEEDS_TO_MBI_ALLOWED, false)
        steps.clientSteps().setClientLimit(clientInfo, FEEDS_COUNT_LIMIT, 1L)
        steps.feedSteps().createDefaultSyncedFeed(clientInfo)
        steps.feedSteps().createDefaultSyncedSiteFeed(clientInfo)

        val params = mapOf(
            "name" to "feed name",
            "source" to "URL",
            "business_type" to "RETAIL",
            "url" to "https://yandex.ru/"
        )
        val answer = sendRequest(params, null)

        val success: Boolean = answer.getValue("success")
        val feedId: Long = answer.getValue("result/feed_id")
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(feedId).`as`("feedId").isPositive
        }
    }

    @Test
    fun addSiteFeed_withDuplicatedDomain_success() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.SITE_FEEDS_ALLOWED, true)

        val url = "https://yandex.ru/"

        val feedInfo = steps.feedSteps().createFeed(FeedInfo()
            .withClientInfo(clientInfo)
            .withFeed(defaultFeed(clientInfo.clientId)
                .withUrl(url)
                .withName(url)
                .withTargetDomain(stripDomainTail(stripProtocol(url)))
                .withSource(SITE)))

        val params = mapOf(
            "name" to "feed name",
            "source" to "SITE",
            "business_type" to "RETAIL",
            "url" to url
        )

        val answer = sendRequest(params, null)

        val success: Boolean = answer.getValue("success")
        val feedId: Long = answer.getValue("result/feed_id")

        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(feedId).`as`("feedId").isEqualTo(feedInfo.feedId)
        }
    }

    private fun sendRequest(params: Map<String, String>, multipartFile: MockMultipartFile?): JsonNode {
        val mvm = LinkedMultiValueMap<String, String>()
        params.forEach { mvm.add(it.key, it.value) }

        val requestBuilder = MockMvcRequestBuilders.multipart(METHOD_PATH)
        requestBuilder.accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        requestBuilder.params(mvm)
        if (multipartFile != null) {
            requestBuilder.file(multipartFile)
        }

        val result = mockMvc.perform(requestBuilder).andReturn()
        check(result.response.status == 200) { "Unexpected response status" }
        return fromJson(result.response.contentAsString)
    }

}
