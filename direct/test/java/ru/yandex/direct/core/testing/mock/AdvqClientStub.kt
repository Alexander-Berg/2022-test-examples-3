package ru.yandex.direct.core.testing.mock

import org.asynchttpclient.AsyncHttpClient
import org.mockito.Mockito.mock
import ru.yandex.direct.advq.AdvqClient
import ru.yandex.direct.advq.AdvqClientSettings
import ru.yandex.direct.advq.AdvqSearchOptions
import ru.yandex.direct.advq.CheckMinHitsResult
import ru.yandex.direct.advq.SearchKeywordResult
import ru.yandex.direct.advq.SearchRequest
import ru.yandex.direct.advq.search.AdvqRequestKeyword
import ru.yandex.direct.advq.search.SearchItem
import ru.yandex.direct.advq.search.Statistics
import java.time.Duration
import java.util.IdentityHashMap

/**
 * Стаб AdvqClient, который отвечает на все запросы search одинаковым прогнозом [AdvqClientStub.defaultForecast]
 */
class AdvqClientStub(val defaultForecast: Long = 500L) :
    AdvqClient(mock(AdvqClientSettings::class.java), mock(AsyncHttpClient::class.java)) {

    override fun search(
        requests: Collection<SearchRequest>,
        options: AdvqSearchOptions?,
        timeout: Duration?
    ): IdentityHashMap<SearchRequest, Map<AdvqRequestKeyword, SearchKeywordResult>> =
        requests.associateWithTo(IdentityHashMap()) {
            it.keywords.associateWith {
                SearchKeywordResult.success(
                    SearchItem()
                        .withReq(it.phrase)
                        .withStat(Statistics().withTotalCount(defaultForecast))
                )
            }
        }

    override fun checkMinHits(
        keywords: MutableList<String>,
        regionIds: MutableList<Long>,
        devices: MutableList<String>
    ): CheckMinHitsResult {
        return CheckMinHitsResult.success(emptyMap());
    }
}
