package ru.yandex.travel.api.services.dictionaries.train.station_express_alias

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.travel.dicts.rasp.proto.TStationExpressAlias
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

class TrainStationExpressAliasServiceTest {
    private val stationExpressAliasService: TrainStationExpressAliasService

    init {
        val config = TrainStationExpressAliasDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-station-express-alias-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TStationExpressAlias>()
            .setLuceneData(
                listOf(
                    TStationExpressAlias.newBuilder().setStationId(9845342).setAlias("8 МАРТА").build(),
                    TStationExpressAlias.newBuilder().setStationId(9845342).setAlias("АБАГУРОВСК").build(),
                    TStationExpressAlias.newBuilder().setStationId(9725747).setAlias("АБАЗА").build(),
                )
            )

        stationExpressAliasService = TrainStationExpressAliasService(config, luceneIndexBuilder)
    }

    @Test
    fun testGetStationIdExpressName() {
        val actual = stationExpressAliasService.getStationIdByExpressName("АБАГУРОВСК")

        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo(9845342)
    }

    @Test
    fun testGetStationIdExpressNameNotFound() {
        val actual = stationExpressAliasService.getStationIdByExpressName("НЕИЗВЕСТНЫЙ")

        assertThat(actual).isNull()
    }
}
