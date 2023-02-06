package ru.yandex.market.pricingmgmt.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.pricingmgmt.api.ControllerTest

internal class TranslationServiceTest : ControllerTest() {

    @Autowired
    lateinit var translationService: TranslationService

    @ParameterizedTest
    @CsvSource(
        "test,TEST",
        "ЭиБТ,EIBT",
        "DIY & Auto,DIY_AND_AUTO",
        "FMCG,FMCG",
        "Фарма,FARMA",
        "Товары для дома,TOVARY_DLYA_DOMA",
        "Детские товары,DETSKIE_TOVARY",
        "Fashion,FASHION",
        "тест#разных@знаков)пунктуации;1№2,TEST_RAZNYKH_ZNAKOV_PUNKTUATSII_1_2"
    )
    fun testRusToEng(input: String, expected: String) {
        assertEquals(expected, translationService.rusToEng(input))
    }
}
