package ru.yandex.direct.geosuggest.client

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.Test
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory

class GeoSuggestClientTest : GeoSuggestTestBase() {

    private val mockWebServer: MockWebServer = MockWebServer()
    private val client: GeoSuggestClient = GeoSuggestClient(
        ParallelFetcherFactory(
            DefaultAsyncHttpClient(),
            FetcherSettings()
        ),
        mockWebServer.url("/").toString()
    )

    @Test
    fun geoSuggestClientGetTest() {
        val jsonResponse = """
        [
            "Мос",
            [
                {
                    "name":"Москва, Москва и Московская область",
                    "name_short":"Москва",
                    "kind":"geobase_city",
                    "lat":55.75321579,
                    "lon":37.62250519,
                    "geoid":213,
                    "url":"//yandex.ru/pogoda/213/"
                },
                {
                    "name":"Москва и Московская область",
                    "name_short":"Москва и Московская область",
                    "kind":"geobase_region",
                    "lat":55.81579208,
                    "lon":37.38003159,
                    "geoid":1,
                    "url":"//yandex.ru/pogoda/1/"
                },
                {
                    "name":"Московский, Москва и Московская область",
                    "name_short":"Московский",
                    "kind":"geobase_city",
                    "lat":55.60214233,
                    "lon":37.34654999,
                    "geoid":103817,
                    "url":"//yandex.ru/pogoda/103817/"
                },
                {
                    "name":"Мосул, Мухафаза Нинава, Ирак",
                    "name_short":"Мосул",
                    "kind":"geobase_city",
                    "lat":36.35606766,
                    "lon":43.15882874,
                    "geoid":40608,
                    "url":"//yandex.ru/pogoda/40608/"
                },
                {
                    "name":"Мосоро, Штат Риу-Гранди-ду-Норти, Бразилия",
                    "name_short":"Мосоро",
                    "kind":"geobase_city",
                    "lat":-5.186385155,
                    "lon":-37.34098053,
                    "geoid":111333,
                    "url":"//yandex.ru/pogoda/111333/"
                },
                {
                    "name":"Мостар, Босния и Герцеговина",
                    "name_short":"Мостар",
                    "kind":"geobase_city",
                    "lat":43.34907913,
                    "lon":17.79063988,
                    "geoid":111209,
                    "url":"//yandex.ru/pogoda/111209/"
                },
                {
                    "name":"Мосальск, Калужская область",
                    "name_short":"Мосальск",
                    "kind":"geobase_city",
                    "lat":54.4913063,
                    "lon":34.98419571,
                    "geoid":20204,
                    "url":"//yandex.ru/pogoda/20204/"
                },
                {
                    "name":"Намибе, Ангола",
                    "name_short":"Намибе",
                    "kind":"geobase_city",
                    "lat":-15.19766521,
                    "lon":12.14912128,
                    "geoid":110987,
                    "url":"//yandex.ru/pogoda/110987/"
                },
                {
                    "name":"Шахрихан, Андижанская область, Узбекистан",
                    "name_short":"Шахрихан",
                    "kind":"geobase_city",
                    "lat":40.71159744,
                    "lon":72.05094147,
                    "geoid":190407,
                    "url":"//yandex.ru/pogoda/190407/"
                },
                {
                    "name":"Узункёпрю, Провинция Эдирне, Турция",
                    "name_short":"Узункёпрю",
                    "kind":"geobase_city",
                    "lat":41.2677002,
                    "lon":26.68784142,
                    "geoid":104738,
                    "url":"//yandex.ru/pogoda/104738/"
                }
            ]
        ]
    """
        val requestPath = "/suggest-geo?results=10&client_id=rmp.uac&" +
            "bases=geobase_country%2Cgeobase_country_part%2Cgeobase_region%2Cgeobase_city&" +
            "callback=&highlight=0&v=8&ll=37.588628%2C55.734046&lang=ru&part=%D0%9C%D0%BE%D1%81"

        mockWebServer.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.path == requestPath) {
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(jsonResponse)
                } else {
                    MockResponse()
                        .setResponseCode(500)
                }
            }
        })

        val response = client.getSuggestedRegions(
            10,
            "rmp.uac",
            "geobase_country,geobase_country_part,geobase_region,geobase_city",
            false,
            8,
            "37.588628,55.734046",
            "ru",
            "Мос"
        )
        assertThat(response).isEqualTo(expectedResponse)
    }
}
