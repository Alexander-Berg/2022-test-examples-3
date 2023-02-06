package ru.yandex.direct.oneshot.oneshots.marketrescuer

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.oneshot.oneshots.marketrescuer.GenerateBannersForMarketOneshot.Companion.normalizeQuery
import ru.yandex.direct.oneshot.oneshots.marketrescuer.GenerateBannersForMarketOneshot.Companion.transformQueryToTitle

@RunWith(JUnitParamsRunner::class)
class GenerateBannersForMarketOneshotMethodTest {

    fun testDataFor_normalizeQuery() = listOf(
            // comma to dot
            listOf("1,0", "1.0"),
            listOf("12,345", "12.345"),
            listOf(",345", "345"),

            // trim and delete double space chars
            listOf("  яндекс маркет      super  ", "яндекс маркет super"),

            // delete invalid dots
            listOf(".123", "123"),
            listOf("123.", "123"),
            listOf("... .. .", ""),
            listOf("средство от тараканов . аэрозоль", "средство от тараканов аэрозоль"),

            // delete invalid chars
            listOf("купить Xiaomi REDMI 9\" 4/64GB (NFC)", "купить Xiaomi REDMI 9 4 64GB NFC"),

            // other
            listOf("яндекс-маркет интернет-магазин", "яндекс маркет интернет магазин"),
    )

    @Test
    @Parameters(method = "testDataFor_normalizeQuery")
    @TestCaseName("normalize query: {0}; and expect {1}")
    fun checkNormalizeQuery(query: String, expectedNormalizedQuery: String) {
        val result = normalizeQuery(query)

        assertThat(result)
                .isEqualTo(expectedNormalizedQuery)
    }


    fun testDataFor_transformQueryToTitle() = listOf(
            listOf("LENOVO ThinkPad E14 7 495 104 41 51", "LENOVO Thinkpad E14 7 495 104 41 51"),
            listOf("APPLE VRF6640LVR АВП34ТО", "APPLE VRF6640LVR АВП34ТО"),
            listOf("APpLE НЕТ vRF6640LVR АВп34ТО", "Apple Нет Vrf6640lvr Авп34то"),

            listOf("купить Xiaomi REDMI 9 4 64GB NFC", "Купить Xiaomi REDMI 9 4 64GB NFC"),
            listOf("средство от тараканов аэрозоль", "Средство от Тараканов Аэрозоль"),
            listOf("яндекс маркет интернет магазин", "Яндекс Маркет Интернет Магазин"),
    )

    @Test
    @Parameters(method = "testDataFor_transformQueryToTitle")
    @TestCaseName("transform query: {0}; to title {1}")
    fun checkTransformQueryToTitle(query: String, expectedNormalizedQuery: String) {
        val result = transformQueryToTitle(query)

        assertThat(result)
                .isEqualTo(expectedNormalizedQuery)
    }

}
