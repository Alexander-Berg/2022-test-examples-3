package ru.yandex.travel.api.services.dictionaries.train.station

import ru.yandex.travel.dicts.rasp.proto.TStation
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

object MockTrainStationDataProvider {

    fun build(stations: List<TStation>): TrainStationDataProvider {
        val config = TrainStationDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-station-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TStation>()
            .setLuceneData(stations)

        return TrainStationService(config, luceneIndexBuilder)
    }

}
