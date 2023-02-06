package ru.yandex.travel.api.services.dictionaries.train.station_express_alias

import ru.yandex.travel.dicts.rasp.proto.TStationExpressAlias
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

object MockTrainStationExpressAliasDataProvider {

    fun build(stationExpressAliases: List<TStationExpressAlias>): TrainStationExpressAliasDataProvider {
        val config = TrainStationExpressAliasDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-station-express-alias-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TStationExpressAlias>()
            .setLuceneData(stationExpressAliases)

        return TrainStationExpressAliasService(config, luceneIndexBuilder)
    }
}
