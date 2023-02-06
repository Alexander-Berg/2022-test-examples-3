package ru.yandex.market.contentmapping.services

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

import org.junit.Assert.*
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.model.MarketParameterValue
import ru.yandex.market.contentmapping.dto.model.Picture
import ru.yandex.market.contentmapping.dto.model.ValueSource

class ModelDataChecksumServiceAdler32ImplTest {

    @Test
    fun calculateChecksumCategoryTest() {
        val checksumService = ModelDataChecksumServiceAdler32Impl()
        val withoutCategory = checksumService.calculateChecksum(emptyMap(), emptyList(), null)
        val withZeroCategory = checksumService.calculateChecksum(emptyMap(), emptyList(), 0)
        val withNonZeroCategory = checksumService.calculateChecksum(emptyMap(), emptyList(), 1)

        withoutCategory shouldNotBe withZeroCategory
        withoutCategory shouldNotBe withNonZeroCategory
        withZeroCategory shouldNotBe withNonZeroCategory
    }

    @Test
    fun calculateChecksumMarketValuesTest() {
        val checksumService = ModelDataChecksumServiceAdler32Impl()
        var values = createValues()

        val withoutValues = checksumService.calculateChecksum(emptyMap(), emptyList(), null)
        val withValues = checksumService.calculateChecksum(mapOf(1L to values), emptyList(), 0)
        val pair = Pair(values[0], values[1])
        values[0] = pair.second
        values[1] = pair.first
        val withValuesInAnotherOrder = checksumService.calculateChecksum(mapOf(1L to values), emptyList(), 0)
        values = createValues()
        values[0] = values[0].copy(value = MarketParamValue.StringValue("v2"))
        val withValuesStringChanged = checksumService.calculateChecksum(mapOf(1L to values), emptyList(), 0)
        values = createValues()
        values[1] = values[1].copy(value = MarketParamValue.NumericValue(2.0))
        val withValuesNumericChanged = checksumService.calculateChecksum(mapOf(1L to values), emptyList(), 0)
        values = createValues()
        values[2] = values[2].copy(value = MarketParamValue.BooleanValue(true))
        val withValuesBooleanChanged = checksumService.calculateChecksum(mapOf(1L to values), emptyList(), 0)
        values = createValues()
        values[3] = values[3].copy(value = MarketParamValue.OptionValue(2))
        val withValuesOptionChanged = checksumService.calculateChecksum(mapOf(1L to values), emptyList(), 0)
        values = createValues()
        values[3] = values[3].copy(value = MarketParamValue.HypothesisValue("v2"))
        val withValuesHypothesisChanged = checksumService.calculateChecksum(mapOf(1L to values), emptyList(), 0)

        withoutValues shouldNotBe withValues
        withValues shouldBe withValuesInAnotherOrder
        withValues shouldNotBe withValuesStringChanged
        withValues shouldNotBe withValuesNumericChanged
        withValues shouldNotBe withValuesBooleanChanged
        withValues shouldNotBe withValuesOptionChanged
        withValues shouldNotBe withValuesHypothesisChanged
    }

    private fun createValues() = mutableListOf(
            MarketParameterValue(
                    1L,
                    ValueSource.MANUAL,
                    MarketParamValue.StringValue("v")
            ),
            MarketParameterValue(
                    1L,
                    ValueSource.MANUAL,
                    MarketParamValue.NumericValue(1.0)
            ),
            MarketParameterValue(
                    1L,
                    ValueSource.MANUAL,
                    MarketParamValue.BooleanValue(false)
            ),
            MarketParameterValue(
                    1L,
                    ValueSource.MANUAL,
                    MarketParamValue.OptionValue(1, "v")
            ),
            MarketParameterValue(
                    1L,
                    ValueSource.MANUAL,
                    MarketParamValue.HypothesisValue("v")
            )
    )

    @Test
    fun calculateChecksumPicturesTest() {
        val checksumService = ModelDataChecksumServiceAdler32Impl()
        val pictures = listOf(
                Picture("url1", ValueSource.IMPORT, 0, true),
                Picture("url2", ValueSource.IMPORT, 0, true),
                Picture("url3", ValueSource.IMPORT, 0, true),
        )
        val picturesInAnotherOrder = listOf(
                Picture("url3", ValueSource.IMPORT, 0, true),
                Picture("url2", ValueSource.IMPORT, 0, true),
                Picture("url1", ValueSource.IMPORT, 0, true),
        )
        val picturesWithChanges = listOf(
                Picture("url1", ValueSource.FORMALIZATION, 0, true),
                Picture("url2", ValueSource.IMPORT, 1, true),
                Picture("url3", ValueSource.IMPORT, 0, false)
        )
        val withoutPictures = checksumService.calculateChecksum(emptyMap(), emptyList(), null)
        val withPictures = checksumService.calculateChecksum(emptyMap(), pictures, null)
        val withAnotherSourcePictures = checksumService.calculateChecksum(emptyMap(), picturesWithChanges, null)
        val withPicturesInAnotherOrder = checksumService.calculateChecksum(emptyMap(), picturesInAnotherOrder, null)

        withoutPictures shouldNotBe withPictures
        withoutPictures shouldNotBe withAnotherSourcePictures
        withoutPictures shouldNotBe withPicturesInAnotherOrder

        withPictures shouldBe withAnotherSourcePictures
        withPictures shouldNotBe withPicturesInAnotherOrder
    }
}
