package ru.yandex.market.wms.constraints.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegexTierExtractorTest {

    private val underTest = RegexTierExtractor()

    @Test
    fun `should find tier correctly`() {
        val mezoninLoc = "B2-25-04A3"
        val rackLoc = "D1-04-01C2"
        val pallet1Loc = "R02-32A1S2"
        val pallet2Loc = "R01-122-03"
        val bufLoc = "BUF-PLC1A2"

        assertThat(underTest.extract(mezoninLoc)).isEqualTo("A")
        assertThat(underTest.extract(rackLoc)).isEqualTo("C")
        assertThat(underTest.extract(pallet1Loc)).isEqualTo("1")
        assertThat(underTest.extract(pallet2Loc)).isEqualTo("03")
        assertThat(underTest.extract(bufLoc)).isEqualTo("1")
    }
}
