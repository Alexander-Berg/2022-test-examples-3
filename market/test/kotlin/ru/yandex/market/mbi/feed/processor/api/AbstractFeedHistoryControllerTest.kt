package ru.yandex.market.mbi.feed.processor.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.HttpClientErrorException
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.environment.EnvironmentService
import ru.yandex.market.mbi.feed.processor.model.ApiError
import ru.yandex.market.mbi.feed.processor.model.FeedHistoryResponse
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord
import ru.yandex.market.mbi.feed.processor.test.getString
import ru.yandex.market.yt.binding.YTBinder
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import java.net.URI

internal abstract class AbstractFeedHistoryControllerTest : FunctionalTest() {
    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var ytSamovarFeedDownloadClientProxySource: YtClientProxySource

    @Autowired
    protected lateinit var ytFeedParsingClientProxySource: YtClientProxySource

    @Autowired
    protected lateinit var ytDatacampParsingHistoryClientProxySource: YtClientProxySource

    @Autowired
    protected lateinit var ytFeedParsingClientProxy: YtClientProxy

    @Autowired
    protected lateinit var ytSamovarFeedDownloadClientProxy: YtClientProxy

    @Autowired
    protected lateinit var ytDatacampParsingHistoryClientProxy: YtClientProxy

    @Autowired
    protected lateinit var environmentService: EnvironmentService

    protected val partnerId = 666999L
    protected val businessId = 2222L
    protected val recordId = "111"

    @BeforeEach
    internal fun setUp() {
        doReturn(ytSamovarFeedDownloadClientProxy).`when`(ytSamovarFeedDownloadClientProxySource).currentClient
        doReturn(ytFeedParsingClientProxy).`when`(ytFeedParsingClientProxySource).currentClient
        doReturn(ytDatacampParsingHistoryClientProxy).`when`(ytDatacampParsingHistoryClientProxySource).currentClient
    }

    protected fun checkBadRequestMessagePost(
        requestPath: String,
        errorPath: String,
        url: String,
        placeholders: Map<String, String>? = null
    ) {
        val request = getString<FeedUpdatesHistoryControllerTest>(requestPath)
        val jsonString = getJsonStringWithReplacedPlaceholders(errorPath, placeholders)
        val expectedErrors = objectMapper.readValue(jsonString, object : TypeReference<List<ApiError>>() {})
        Assertions.assertThatThrownBy {
            ru.yandex.market.mbi.feed.processor.FunctionalTestHelper.get(
                URI.create(url),
                FeedHistoryResponse::class.java
            )
            FunctionalTestHelper.postForJson(url, request)
        }
            .isInstanceOf(HttpClientErrorException.BadRequest::class.java)
            .hasMessageContaining(objectMapper.writeValueAsString(expectedErrors))
    }

    protected fun checkBadRequestMessage(jsonpath: String, url: String, placeholders: Map<String, String>? = null) {
        val jsonString = getJsonStringWithReplacedPlaceholders(jsonpath, placeholders)
        val expectedErrors = objectMapper.readValue(jsonString, object : TypeReference<List<ApiError>>() {})
        Assertions.assertThatThrownBy {
            ru.yandex.market.mbi.feed.processor.FunctionalTestHelper.get(
                URI.create(url),
                FeedHistoryResponse::class.java
            )
        }
            .isInstanceOf(HttpClientErrorException.BadRequest::class.java)
            .hasMessageContaining(objectMapper.writeValueAsString(expectedErrors))
    }

    private fun getJsonStringWithReplacedPlaceholders(jsonpath: String, placeholders: Map<String, String>?): String? {
        var result = getResourceAsString(jsonpath)
        result?.let {
            placeholders?.forEach { (placeholder, value) ->
                result = result!!.replace(Regex("\\{$placeholder\\}"), value)
            }
        }
        return result
    }

    protected fun mockFeedParsingYtClient(path: String): KArgumentCaptor<String> {
        val feedParsingRecords = objectMapper.readValue(
            getResourceAsString(path),
            object : TypeReference<List<DatacampParsingHistoryRecord>>() {}
        )

        val captorQuery: KArgumentCaptor<String> = argumentCaptor()

        doReturn(
            feedParsingRecords
        ).`when`(
            ytDatacampParsingHistoryClientProxy
        ).selectRows(
            captorQuery.capture(),
            eq(YTBinder.getBinder(DatacampParsingHistoryRecord::class.java))
        )
        return captorQuery
    }

    private fun getResourceAsString(path: String): String? {
        return {}.javaClass.getResource(path)?.readText()
    }
}
