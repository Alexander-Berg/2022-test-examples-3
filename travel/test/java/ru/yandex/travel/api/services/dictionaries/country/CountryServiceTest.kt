package ru.yandex.travel.api.services.dictionaries.country

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

import ru.yandex.travel.dicts.rasp.proto.TCountry
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

class CountryServiceTest {
    private val countryService: CountryService

    init {
        val config =
            CountryDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./dict-country-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TCountry>()
            .setLuceneData(listOf(
                TCountry.newBuilder().setGeoId(100).setId(84).build(),
                TCountry.newBuilder().setGeoId(10).setId(20).build(),
                TCountry.newBuilder().setGeoId(2).setId(2).build()
            ))

        countryService = CountryService(config, luceneIndexBuilder)
    }

    @Test
    fun testGetById() {
        val actual = countryService.getById(84)

        assertThat(actual).isNotNull
        assertThat(actual.id).isEqualTo(84)
        assertThat(actual.geoId).isEqualTo(100)
    }

    @Test
    fun testGetByIdNotFound() {
        assertThatThrownBy {
            countryService.getById(-1)
        }
    }

    @Test
    fun testGetByGeoId() {
        val actual = countryService.getByGeoId(10)

        assertThat(actual).isNotNull
        assertThat(actual.id).isEqualTo(20)
        assertThat(actual.geoId).isEqualTo(10)
    }

    @Test
    fun testGetByGeoIdNotFound() {
        assertThatThrownBy {
            countryService.getByGeoId(-1)
        }
    }
}
