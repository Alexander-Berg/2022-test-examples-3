package ru.yandex.travel.api.services.dictionaries.train.settlement

import ru.yandex.travel.dicts.rasp.proto.TSettlement
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

object MockTrainSettlementDataProvider {

    fun build(settlements: List<TSettlement>): TrainSettlementDataProvider {
        val config = TrainSettlementDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-settlement-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TSettlement>()
            .setLuceneData(settlements)

        return TrainSettlementService(config, luceneIndexBuilder)
    }
}
