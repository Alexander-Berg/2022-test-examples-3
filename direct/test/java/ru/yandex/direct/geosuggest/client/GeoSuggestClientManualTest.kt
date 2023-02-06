package ru.yandex.direct.geosuggest.client

import org.assertj.core.api.Assertions.assertThat
import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.Ignore
import org.junit.Test
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory

@Ignore("Uses external service")
// Uses real handle, response may change!
class GeoSuggestClientManualTest : GeoSuggestTestBase() {

    private val client = GeoSuggestClient(
        ParallelFetcherFactory(
            DefaultAsyncHttpClient(),
            FetcherSettings()
        ),
        "https://suggest-maps.yandex.ru/"
    )

    @Test
    fun geoSuggestClientGetManualTest() {
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
