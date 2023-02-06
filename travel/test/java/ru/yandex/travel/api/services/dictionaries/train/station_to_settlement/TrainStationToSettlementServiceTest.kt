package ru.yandex.travel.api.services.dictionaries.train.station_to_settlement

import org.assertj.core.api.Assertions
import org.junit.Test

import ru.yandex.travel.dicts.rasp.proto.TStation2Settlement
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

class TrainStationToSettlementServiceTest {
    private val stationToSettlementService: TrainStationToSettlementService

    init {
        val config = TrainStationToSettlementDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-station-to-settlement-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TStation2Settlement>()
            .setLuceneData(
                listOf(
                    TStation2Settlement.newBuilder().setStationId(1001).setSettlementId(1).build(),
                    TStation2Settlement.newBuilder().setStationId(1002).setSettlementId(1).build(),
                )
            )

        stationToSettlementService = TrainStationToSettlementService(config, luceneIndexBuilder)
    }

    @Test
    fun testGetStationIds() {
        val actual = stationToSettlementService.getStationIds(1)

        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isEqualTo(listOf(1001, 1002))
    }

    @Test
    fun testGetStationIdsNotFound() {
        val actual = stationToSettlementService.getStationIds(2)

        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isEmpty()
    }
}
