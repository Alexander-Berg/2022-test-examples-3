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
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.Source.SITE
import ru.yandex.direct.core.entity.feed.model.UpdateStatus.DONE
import ru.yandex.direct.core.entity.feed.model.UpdateStatus.NEW
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.testing.MockMvcCreator
import ru.yandex.direct.core.testing.data.TestFeeds
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripDomainTail
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripProtocol
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.utils.HashingUtils.getMd5HashAsBase64YaStringWithoutPadding
import ru.yandex.direct.utils.JsonUtils.fromJson
import ru.yandex.direct.web.common.getValue
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.feed.service.FeedWebService

private const val METHOD_PATH = "/feed/update_feed"

@DirectWebTest
@RunWith(SpringRunner::class)
class FeedControllerUpdateTest {
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
    fun updateUrlFeed_success() {
        val name = "new feed name"
        val url = "https://yandex.ru/new"
        val login = "new_feed_login"
        val password = "new_feed_password"
        val isRemoveUtm = true

        val feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)

        val expectedFeed = Feed().apply {
            this.name = name
            this.url = url
            this.login = login
            plainPassword = password
            this.isRemoveUtm = isRemoveUtm
            this.updateStatus = NEW
        }

        val params = mapOf(
            "feed_id" to feedInfo.feedId.toString(),
            "name" to name,
            "url" to url,
            "login" to login,
            "password" to password,
            "is_remove_utm" to isRemoveUtm.toString().toLowerCase()
        )
        val answer = sendRequest(params, null)

        val success: Boolean = answer.get("success").asBoolean()
        val feedId = answer.get("result").get("feed_id").asLong()
        val actualFeed = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, mutableListOf(feedInfo.feedId))[0]
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(feedId).`as`("feedId").isEqualTo(feedInfo.feedId)
            it.assertThat(actualFeed).`as`("actualFeed")
                .`is`(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())))
        }
    }

    @Test
    fun updateUrlFeed_whenWrongName_failure() {
        val feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)

        val params = mapOf(
            "feed_id" to feedInfo.feedId.toString(),
            "name" to "",
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
    fun updateFileFeed_withNewFile_success() {
        val name = "new feed name"
        val fileName = "new_file_name.tsv"
        val fileContent = ByteArray(3) { 32 } // Пробелы
        val multipartFile = MockMultipartFile("file", fileName, "text/tab-separated-values", fileContent)

        val feedInfo = steps.feedSteps().createDefaultFileFeed(clientInfo)

        val expectedFeed = Feed().apply {
            this.name = name
            this.filename = fileName
            this.updateStatus = NEW
            this.cachedFileHash = getMd5HashAsBase64YaStringWithoutPadding(fileContent)
        }

        val params = mapOf("feed_id" to feedInfo.feedId.toString(), "name" to name)
        val answer = sendRequest(params, multipartFile)

        val success: Boolean = answer.get("success").asBoolean()
        val feedId = answer.get("result").get("feed_id").asLong()
        val actualFeed = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, mutableListOf(feedInfo.feedId))[0]
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(feedId).`as`("feedId").isEqualTo(feedInfo.feedId)
            it.assertThat(actualFeed).`as`("actualFeed")
                .`is`(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())))
        }
    }

    @Test
    fun updateFileFeed_withTheSameFile_success() {
        val name = "new feed name"
        val fileName = "new_file_name.tsv"
        val feedInfo = steps.feedSteps().createDefaultFileFeed(clientInfo)
        val multipartFile =
            MockMultipartFile("file", fileName, "text/tab-separated-values", TestFeeds.FILE_DATA)

        val expectedFeed = Feed().apply {
            this.name = name
            this.filename = fileName
            this.updateStatus = DONE
            this.cachedFileHash = TestFeeds.FILE_HASH
        }

        val params = mapOf("feed_id" to feedInfo.feedId.toString(), "name" to name)
        val answer = sendRequest(params, multipartFile)

        val success: Boolean = answer.get("success").asBoolean()
        val feedId = answer.get("result").get("feed_id").asLong()
        val actualFeed = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, mutableListOf(feedInfo.feedId))[0]
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isTrue
            it.assertThat(feedId).`as`("feedId").isEqualTo(feedInfo.feedId)
            it.assertThat(actualFeed).`as`("actualFeed")
                .`is`(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())))
        }
    }

    data class MarketClientArgument<T>(var feedInfo: T? = null)

    @Test
    fun updateSiteFeed_withDuplicatedDomain_failure() {
        val url1 = "https://ya.ru"
        val url2 = "https://yandex.ru/"

        val existingFeed = steps.feedSteps().createFeed(
            FeedInfo()
                .withClientInfo(clientInfo)
                .withFeed(
                    TestFeeds.defaultFeed(clientInfo.clientId)
                        .withUrl(url1)
                        .withName(url1)
                        .withTargetDomain(stripDomainTail(stripProtocol(url1)))
                        .withSource(SITE)
                )
        )

        val updatingFeed = steps.feedSteps().createFeed(
            FeedInfo()
                .withClientInfo(clientInfo)
                .withFeed(
                    TestFeeds.defaultFeed(clientInfo.clientId)
                        .withUrl(url2)
                        .withName(url2)
                        .withTargetDomain(stripDomainTail(stripProtocol(url2)))
                        .withSource(SITE)
                )
        )

        val params = mapOf(
            "feed_id" to updatingFeed.feedId.toString(),
            "name" to "feed name",
            "url" to url1
        )

        val answer = sendRequest(params, null)

        val success: Boolean = answer.getValue("success")
        val errorCode: String = answer.getValue("validation_result/errors/0/code")
        val errorParams: Long = answer.getValue("validation_result/errors/0/params/id")
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("success").isFalse
            it.assertThat(errorCode).`as`("errorCode")
                .isEqualTo("FeedDefectIds.ModelId.FEED_BY_SITE_WITH_SAME_DOMAIN_ALREADY_EXISTS")
            it.assertThat(errorParams).`as`("errorParams")
                .isEqualTo(existingFeed.feedId)
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
