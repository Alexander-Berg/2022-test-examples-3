package ru.yandex.travel.api.services.dictionaries.train.station_to_settlement

import ru.yandex.travel.dicts.rasp.proto.TStation2Settlement
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

object MockTrainStationToSettlementDataProvider {
    fun build(stationToSettlements: List<TStation2Settlement>): TrainStationToSettlementDataProvider {
        val config = TrainStationToSettlementDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-station-to-settlement-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TStation2Settlement>()
            .setLuceneData(stationToSettlements)

        return TrainStationToSettlementService(config, luceneIndexBuilder)
    }
}
