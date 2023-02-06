package ru.yandex.travel.api.services.dictionaries.train.readable_timezone

import ru.yandex.travel.dicts.rasp.proto.TReadableTimezone
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

object MockTrainReadableTimezoneDataProvider {

    fun build(readableTimezones: List<TReadableTimezone>): TrainReadableTimezoneDataProvider {
        val config = TrainReadableTimezoneDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-readable-timezone-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TReadableTimezone>()
            .setLuceneData(readableTimezones)

        return TrainReadableTimezoneService(config, luceneIndexBuilder)
    }

}
