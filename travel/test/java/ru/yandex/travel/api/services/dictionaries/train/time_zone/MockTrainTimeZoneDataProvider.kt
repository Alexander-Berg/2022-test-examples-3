package ru.yandex.travel.api.services.dictionaries.train.time_zone

import ru.yandex.travel.dicts.rasp.proto.TTimeZone
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

object MockTrainTimeZoneDataProvider {

    fun build(timeZones: List<TTimeZone>): TrainTimeZoneDataProvider {
        val config =
            TrainTimeZoneDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-time-zone-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TTimeZone>()
            .setLuceneData(timeZones)

        return TrainTimeZoneService(config, luceneIndexBuilder)
    }
}
