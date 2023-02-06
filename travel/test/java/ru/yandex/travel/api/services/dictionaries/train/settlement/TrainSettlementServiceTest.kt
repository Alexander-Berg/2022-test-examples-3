package ru.yandex.travel.api.services.dictionaries.train.settlement

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

import ru.yandex.travel.dicts.rasp.proto.TCountry
import ru.yandex.travel.dicts.rasp.proto.TSettlement
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder

class TrainSettlementServiceTest {
    private val settlementService: TrainSettlementDataProvider

    init {
        val config = TrainSettlementDataProviderProperties()
        config.tablePath = "tablePath"
        config.indexPath = "./train-settlement-index"
        config.proxy = emptyList()

        val luceneIndexBuilder = TestLuceneIndexBuilder<TSettlement>()
            .setLuceneData(listOf(
                TSettlement.newBuilder().setGeoId(100).setId(84).build(),
                TSettlement.newBuilder().setGeoId(10).setId(20).setIata("LED").setSirenaId("СПТ").build(),
                TSettlement.newBuilder().setGeoId(2).setId(2).setSirenaId("СПТ").build(),
                TSettlement.newBuilder().setGeoId(50).setId(60).setIata("").setSirenaId("").build()
            ))

        settlementService = TrainSettlementService(config, luceneIndexBuilder)
    }

    @Test
    fun testGetById() {
        val actual = settlementService.getById(84)

        assertThat(actual).isNotNull
        assertThat(actual.id).isEqualTo(84)
        assertThat(actual.geoId).isEqualTo(100)
    }

    @Test
    fun testGetByIdNotFound() {
        assertThatThrownBy {
            settlementService.getById(-1)
        }
    }

    @Test
    fun testGetOptionalSettlementByGeoId() {
        val actual = settlementService.getOptionalSettlementByGeoId(10)

        assertThat(actual.isPresent).isTrue
        assertThat(actual.get().id).isEqualTo(20)
        assertThat(actual.get().geoId).isEqualTo(10)
    }

    @Test
    fun testGetOptionalSettlementByGeoIdNotFound() {
        val actual = settlementService.getOptionalSettlementByGeoId(-1)

        assertThat(actual.isPresent).isFalse
    }

    @Test
    fun testGetSettlementCodeByIata() {
        val actual = settlementService.getSettlementCode(20)

        assertThat(actual).isEqualTo("LED")
    }

    @Test
    fun testGetSettlementCodeBySirenaId() {
        val actual = settlementService.getSettlementCode(2)

        assertThat(actual).isEqualTo("СПТ")
    }

    @Test
    fun testGetSettlementCodeWithEmptyIds() {
        val actual = settlementService.getSettlementCode(60)

        assertThat(actual).isNull()
    }
}
