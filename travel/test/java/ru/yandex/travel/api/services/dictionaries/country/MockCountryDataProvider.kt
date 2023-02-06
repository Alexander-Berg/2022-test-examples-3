package ru.yandex.travel.api.services.dictionaries.country

import ru.yandex.travel.dicts.rasp.proto.TCountry
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

object MockCountryDataProvider {

    fun build(countries: List<TCountry>): CountryDataProvider {
        val config =
            CountryDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./dict-country-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TCountry>()
            .setLuceneData(countries)

        return CountryService(config, luceneIndexBuilder)
    }
}
