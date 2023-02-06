package ru.yandex.market.logistics.yard_v2.domain.converter

import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard_v2.util.LicensePlateTranslatorUtil

class LicensePlateTranslatorUtilTest : SoftAssertionSupport() {

    @Test
    fun cyrillicToLatin() {
        val result = LicensePlateTranslatorUtil.translateLicensePlateToLatin("УУУУ")
        softly.assertThat(result).isEqualTo("YYYY")
    }

    @Test
    fun cyrillicWithLatinToLatin() {
        val result = LicensePlateTranslatorUtil.translateLicensePlateToLatin("УУYY")
        softly.assertThat(result).isEqualTo("YYYY")
    }

    @Test
    fun latinToLatin() {
        val result = LicensePlateTranslatorUtil.translateLicensePlateToLatin("YYYY")
        softly.assertThat(result).isEqualTo("YYYY")
    }

    @Test
    fun cyrillicWithNumbersToLatin() {
        val result = LicensePlateTranslatorUtil.translateLicensePlateToLatin("УУ123УУ")
        softly.assertThat(result).isEqualTo("YY123YY")
    }

    @Test
    fun cyrillicWithNumbersAndLatinToLatin() {
        val result = LicensePlateTranslatorUtil.translateLicensePlateToLatin("УУ123YY")
        softly.assertThat(result).isEqualTo("YY123YY")
    }
}
