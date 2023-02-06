package ru.yandex.direct.core.testing.stub

import org.asynchttpclient.DefaultAsyncHttpClient
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import ru.yandex.direct.asynchttp.Result
import ru.yandex.direct.bmapi.client.BmapiClient
import ru.yandex.direct.bmapi.client.model.BmApiError
import ru.yandex.direct.bmapi.client.model.BmApiFeedInfoResponse
import ru.yandex.direct.bmapi.client.model.BmApiRequest
import ru.yandex.direct.bmapi.client.model.BmApiWarning
import ru.yandex.direct.bmapi.client.model.Category
import ru.yandex.direct.bmapi.configuration.BmapiConfiguration
import ru.yandex.direct.core.entity.feed.model.FeedOfferExamples
import ru.yandex.direct.core.entity.feed.model.FeedType

class BmapiClientStub : BmapiClient(
    BmapiConfiguration("http://bmapi-test01i.yandex.ru/fcgi-bin/"),
    ParallelFetcherFactory(DefaultAsyncHttpClient(), FetcherSettings())
) {

    var resultsByFeedUrl: MutableMap<String, Result<BmApiFeedInfoResponse>> = HashMap()

    fun addResult(url: String, result: Result<BmApiFeedInfoResponse>) {
        resultsByFeedUrl[url] = result
    }

    override fun processFeeds(bmapiRequests: List<BmApiRequest>) =
        bmapiRequests.associate { it.feedId to (resultsByFeedUrl[it.url] ?: getDefaultResult()) }

    companion object {
        fun getDefaultResult(): Result<BmApiFeedInfoResponse> {
            val result: Result<BmApiFeedInfoResponse> = Result(0)
            val categoryIdsToOffersCount: Map<String, Int> = mapOf("100" to 100)
            val warnings: List<BmApiWarning>? = null
            val errors: List<BmApiError>? = null
            val feedType: String = FeedType.YANDEX_MARKET.typedValue
            val totalOffersAmount: Long = 100
            val domain: Map<String, Int> = mapOf("yandex.ru" to 100)
            val categories: List<Category> = listOf(Category("100", null, "category1"))
            val vendorsToOffersCount: Map<String, Int> = mapOf("vendor" to 100)
            val feedOfferExamples: FeedOfferExamples? = null

            result.success = BmApiFeedInfoResponse(
                categoryIdsToOffersCount,
                warnings,
                errors,
                feedType,
                totalOffersAmount,
                domain,
                categories,
                vendorsToOffersCount,
                feedOfferExamples
            )
            return result
        }
    }
}
