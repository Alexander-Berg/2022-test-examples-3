package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.showcondition

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository
import ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler.MobileAppsCommon.getVersionParts
import ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.showcondition.handler.AdGroupShowConditionMobileContentHandler

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdGroupShowConditionMobileContentHandlerTest {
    private lateinit var adGroupRepository: AdGroupRepository
    private lateinit var exporter: AdGroupShowConditionExporter
    private lateinit var mobileContentRepository: MobileContentRepository
    private lateinit var mobileHandler: AdGroupShowConditionMobileContentHandler

    @BeforeEach
    fun before() {
        adGroupRepository = mock()
        exporter = mock()
        mobileContentRepository = mock()
        mobileHandler = AdGroupShowConditionMobileContentHandler(adGroupRepository, exporter, mobileContentRepository)
    }

    private val defaultParts = listOf(0, 0)

    @Suppress("UNUSED")
    fun versionCases() = listOf(
        "" to defaultParts,
        "0.0." to defaultParts,
        "text" to defaultParts,
        "text.with.dots" to defaultParts,
        "." to defaultParts,
        ".." to defaultParts,
        "..1" to defaultParts,
        "a1.." to defaultParts,
        "-5.-4" to defaultParts,
        "-5.0" to defaultParts,
        "-5" to defaultParts,
        ".-4" to defaultParts,
        "0.-5" to defaultParts,
        "Android M" to defaultParts,
        "зависит от устройства" to defaultParts,
        "Cihaza göre değişir" to defaultParts,
        // valid
        "0" to defaultParts,
        "0.0" to defaultParts,
        "0.0.0" to defaultParts,
        "0.1" to listOf(0, 1),
        "0.42" to listOf(0, 42),
        "12.34.56" to listOf(12, 34),
        "8.4.1 beta 3" to listOf(8, 4),
        "4.2 или более поздняя" to listOf(4, 2),
        // mixed
        "1.." to listOf(1, 0),
        ".1." to listOf(0, 1),
        "a.1." to listOf(0, 1),
        "1.a." to listOf(1, 0),
        "-2.2." to listOf(0, 2),
        "2.-2." to listOf(2, 0),
        "8.4a" to listOf(8, 0),
    )

    @ParameterizedTest
    @MethodSource("versionCases")
    fun test(testCase: Pair<String, List<Int>>) {
        val got = getVersionParts(testCase.first)
        assertThat(got).isEqualTo(testCase.second)
    }
}
