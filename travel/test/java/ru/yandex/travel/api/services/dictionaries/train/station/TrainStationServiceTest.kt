package ru.yandex.travel.api.services.dictionaries.train.station

import org.assertj.core.api.Assertions
import org.junit.Test

import ru.yandex.travel.dicts.rasp.proto.TStation
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

class TrainStationServiceTest {
    private val stationService: TrainStationService

    init {
        val config = TrainStationDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-station-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TStation>()
            .setLuceneData(listOf(
                TStation.newBuilder().setId(84).setTitleDefault("Одинцово").build(),
                TStation.newBuilder().setId(20).setTitleDefault("Москва (Белорусский вокзал)").build(),
                TStation.newBuilder().setId(2).setTitleDefault("Санкт-Петербург (Витебский вокзал)").build()
            ))

        stationService = TrainStationService(config, luceneIndexBuilder)
    }

    @Test
    fun testGetById() {
        val actual = stationService.getById(20)

        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual.id).isEqualTo(20)
        Assertions.assertThat(actual.titleDefault).isEqualTo("Москва (Белорусский вокзал)")
    }

    @Test
    fun testGetByIdNotFound() {
        Assertions.assertThatThrownBy {
            stationService.getById(-1)
        }
    }
}
