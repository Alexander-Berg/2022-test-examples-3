package ru.yandex.travel.api.services.dictionaries.train.station_code

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import ru.yandex.travel.dicts.rasp.proto.ECodeSystem
import ru.yandex.travel.dicts.rasp.proto.TStationCode
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

class TrainStationCodeServiceTest {
    private val stationCodeService: TrainStationCodeService

    init {
        val config = TrainStationCodeDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-station-code-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TStationCode>()
            .setLuceneData(
                listOf(
                    TStationCode.newBuilder().setStationId(9845342).setSystemId(ECodeSystem.CODE_SYSTEM_EXPRESS)
                        .setCode("2000898").build(),
                    TStationCode.newBuilder().setStationId(9845342).setSystemId(ECodeSystem.CODE_SYSTEM_ESR)
                        .setCode("375").build(),
                    TStationCode.newBuilder().setStationId(9725747).setSystemId(ECodeSystem.CODE_SYSTEM_ROADCONSUFA)
                        .setCode("375").build(),
                )
            )

        stationCodeService = TrainStationCodeService(config, luceneIndexBuilder)
    }

    @Test
    fun testGetStationExpressCode() {
        val actual = stationCodeService.getStationExpressCode(9845342)

        assertThat(actual).isNotNull
        assertThat(actual.stationId).isEqualTo(9845342)
        assertThat(actual.code).isEqualTo("2000898")
    }

    @Test
    fun testGetStationExpressCodeNotFound() {
        assertThatThrownBy {
            stationCodeService.getStationExpressCode(9725747)
        }
    }

    @Test
    fun testGetStationIdByCode() {
        val actual = stationCodeService.getStationIdByCode("2000898")

        assertThat(actual).isEqualTo(9845342)
    }

    @Test
    fun testGetStationIdByCodeNotFound() {
        assertThatThrownBy {
            stationCodeService.getStationIdByCode("375")
        }
    }
}
