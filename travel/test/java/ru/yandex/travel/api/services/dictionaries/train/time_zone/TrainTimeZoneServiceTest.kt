package ru.yandex.travel.api.services.dictionaries.train.time_zone

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

import ru.yandex.travel.dicts.rasp.proto.TTimeZone
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

class TrainTimeZoneServiceTest {
    private val timeZoneService: TrainTimeZoneService

    init {
        val config =
            TrainTimeZoneDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-time-zone-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TTimeZone>()
            .setLuceneData(listOf(
                TTimeZone.newBuilder().setId(0).setCode("Europe/Moscow").build(),
                TTimeZone.newBuilder().setId(5).setCode("Asia/Yekaterinburg").build(),
                TTimeZone.newBuilder().setId(35).setCode("America/Havana").build()
            ))

        timeZoneService = TrainTimeZoneService(config, luceneIndexBuilder)
    }

    @Test
    fun testByGetId() {
        val actual = timeZoneService.getById(5)

        assertThat(actual).isNotNull
        assertThat(actual.code).isEqualTo("Asia/Yekaterinburg")
    }

    @Test
    fun testByGetIdNotFound() {

        assertThatThrownBy {
            timeZoneService.getById(-1)
        }
    }
}
