package ru.yandex.travel.api.services.dictionaries.train.station_code

import ru.yandex.travel.dicts.rasp.proto.TStationCode
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

object MockTrainStationCodeDataProvider {

    fun build(stationCodes: List<TStationCode>): TrainStationCodeDataProvider {
        val config = TrainStationCodeDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-station-code-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TStationCode>()
            .setLuceneData(stationCodes)

        return TrainStationCodeService(config, luceneIndexBuilder)
    }
}
