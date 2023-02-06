package ru.yandex.market.contentmapping.services.datacamp.offer.processor

import Market.DataCamp.DataCampUnitedOffer.UnitedOffer
import com.google.protobuf.util.JsonFormat
import io.kotest.assertions.asClue
import io.kotest.assertions.fail
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import ru.yandex.market.contentmapping.services.datacamp.offer.field.converters.UnitedOfferWrapper
import ru.yandex.market.contentmapping.services.datacamp.offer.processor.DatacampOfferProcessingData.ItemResult
import ru.yandex.market.contentmapping.services.datacamp.offer.processor.DatacampOfferProcessingData.TotalResult
import ru.yandex.market.contentmapping.utils.patchDataCampJsonFormat
import java.nio.charset.StandardCharsets

class DatacampOfferProcessingDataExtractorTest {
    private val extractor = DatacampOfferProcessingDataExtractor()

    @Test
    fun `should extract errors from verdicts`() {
        val offerPath = "services/datacamp/offer/processor/offer-with-verdicts.json"
        val json = this.javaClass.classLoader
            .getResourceAsStream(offerPath)
            ?.readAllBytes()
            ?.let { String(it, StandardCharsets.UTF_8) }
            ?: error("Can't read test resource $offerPath")

        patchDataCampJsonFormat()
        val unitedOffer = UnitedOffer.newBuilder()
        JsonFormat.parser().merge(json, unitedOffer)

        val offerWrapper = UnitedOfferWrapper(unitedOffer.build())

        val offerProcessingData = extractor.extract(offerWrapper) ?: fail("extract returned null")
        offerProcessingData.asClue {
            it.result shouldBe TotalResult.TOTAL_ERROR
            it.items shouldHaveSize 1
            it.items[0].asClue { item ->
                item.result shouldBe ItemResult.ERROR
                item.message.text shouldContain "Не заполнен обязательный параметер"
            }
        }
    }
}
