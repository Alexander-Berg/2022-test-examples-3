package ru.yandex.travel.api.services.dictionaries.train.readable_timezone

import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.travel.dicts.rasp.proto.TReadableTimezone
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

class TrainReadableTimezoneServiceTest {
    private val readableTimezoneService: TrainReadableTimezoneService

    init {
        val config = TrainReadableTimezoneDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-readable-timezone-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TReadableTimezone>()
            .setLuceneData(listOf(
                TReadableTimezone.newBuilder().setLanguage("ru")
                    .setKey("by_tz_Europe/Moscow")
                    .setValue("по Московскому времени").build(),
                TReadableTimezone.newBuilder().setLanguage("en")
                    .setKey("by_tz_Europe/Moscow")
                    .setValue("Moscow time").build()
            ))

        readableTimezoneService = TrainReadableTimezoneService(config, luceneIndexBuilder)
    }

    @Test
    fun testGetByKey() {
        val actual = readableTimezoneService.getByKey("en/by_tz_Europe/Moscow")

        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual.key).isEqualTo("by_tz_Europe/Moscow")
        Assertions.assertThat(actual.value).isEqualTo("Moscow time")
    }

    @Test
    fun testGetByKeyNotFound() {
        Assertions.assertThatThrownBy {
            readableTimezoneService.getByKey("by/by_tz_Europe/Moscow")
        }
    }
}
