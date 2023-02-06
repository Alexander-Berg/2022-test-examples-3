package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.geosuggest.client.GeoSuggestClient
import ru.yandex.direct.geosuggest.client.model.GeoSuggestResponseElement
import ru.yandex.direct.i18n.Language
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacGeoSuggestControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var geoSuggestClient: GeoSuggestClient

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    private fun testGet(queryString: String, expectedRegions: List<GeoSuggestResponseElement>) {
        val res = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/geo_suggest?$queryString")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        assertThat(JsonUtils.MAPPER.readTree(res)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(expectedRegions)
            )
        )
    }

    @Test
    fun testWithoutLangRu() {
        whenever(
            geoSuggestClient.getSuggestedRegions(
                limit = any(),
                clientId = any(),
                basesTypes = any(),
                highlightMatching = any(),
                responseVersion = any(),
                latLong = any(),
                lang = eq("ru"),
                text = eq("Мос")
            )
        ).doReturn(geoSuggestRegionsRu)
        testGet("text=Мос", expectedRegionsRu)
    }

    @Test
    fun testWithLangRu() {
        whenever(
            geoSuggestClient.getSuggestedRegions(
                limit = any(),
                clientId = any(),
                basesTypes = any(),
                highlightMatching = any(),
                responseVersion = any(),
                latLong = any(),
                lang = eq("ru"),
                text = eq("Мос")
            )
        ).doReturn(geoSuggestRegionsRu)
        userInfo.user!!.lang = Language.RU
        testGet("text=Мос&lang=ru", expectedRegionsRu)
    }

    @Test
    fun testTooLongString() {
        val longString = "a".repeat(129)
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/geo_suggest?text=$longString")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    private val expectedRegionsRu = listOf(
        GeoSuggestResponseElement(
            name = "Москва, Москва и Московская область",
            nameShort = "Москва",
            kind = "geobase_city",
            lat = 55.753216f,
            lon = 37.622505f,
            geoid = 213,
            url = "//yandex.ru/pogoda/213/"
        ),
        GeoSuggestResponseElement(
            name = "Москва и Московская область",
            nameShort = "Москва и Московская область",
            kind = "geobase_region",
            lat = 55.815792f,
            lon = 37.38003f,
            geoid = 1,
            url = "//yandex.ru/pogoda/1/"
        ),
    )

    private val geoSuggestRegionsRu = listOf(
        GeoSuggestResponseElement(
            name = "Москва, Москва и Московская область",
            nameShort = "Москва",
            kind = "geobase_city",
            lat = 55.753216f,
            lon = 37.622505f,
            geoid = 213,
            url = "//yandex.ru/pogoda/213/"
        ),
        GeoSuggestResponseElement(
            name = "Москва и Московская область",
            nameShort = "Москва и Московская область",
            kind = "geobase_region",
            lat = 55.815792f,
            lon = 37.38003f,
            geoid = 1,
            url = "//yandex.ru/pogoda/1/"
        ),
        GeoSuggestResponseElement(
            name = "Московский, Москва и Московская область",
            nameShort = "Московский",
            kind = "geobase_city",
            lat = 55.602142f,
            lon = 37.34655f,
            geoid = 103817,
            url = "//yandex.ru/pogoda/103817/"
        ),
        GeoSuggestResponseElement(
            name = "Мосул, Мухафаза Нинава, Ирак",
            nameShort = "Мосул",
            kind = "geobase_city",
            lat = 36.356068f,
            lon = 43.15883f,
            geoid = 40608,
            url = "//yandex.ru/pogoda/40608/"
        ),
        GeoSuggestResponseElement(
            name = "Мосоро, Штат Риу-Гранди-ду-Норти, Бразилия",
            nameShort = "Мосоро",
            kind = "geobase_city",
            lat = -5.186385f,
            lon = -37.34098f,
            geoid = 111333,
            url = "//yandex.ru/pogoda/111333/"
        ),
        GeoSuggestResponseElement(
            name = "Мостар, Босния и Герцеговина",
            nameShort = "Мостар",
            kind = "geobase_city",
            lat = 43.34908f,
            lon = 17.79064f,
            geoid = 111209,
            url = "//yandex.ru/pogoda/111209/"
        ),
        GeoSuggestResponseElement(
            name = "Мосальск, Калужская область",
            nameShort = "Мосальск",
            kind = "geobase_city",
            lat = 54.491306f,
            lon = 34.984196f,
            geoid = 20204,
            url = "//yandex.ru/pogoda/20204/"
        ),
        GeoSuggestResponseElement(
            name = "Намибе, Ангола",
            nameShort = "Намибе",
            kind = "geobase_city",
            lat = -15.197665f,
            lon = 12.149121f,
            geoid = 110987,
            url = "//yandex.ru/pogoda/110987/"
        ),
        GeoSuggestResponseElement(
            name = "Шахрихан, Андижанская область, Узбекистан",
            nameShort = "Шахрихан",
            kind = "geobase_city",
            lat = 40.711597f,
            lon = 72.05094f,
            geoid = 190407,
            url = "//yandex.ru/pogoda/190407/"
        ),
        GeoSuggestResponseElement(
            name = "Узункёпрю, Провинция Эдирне, Турция",
            nameShort = "Узункёпрю",
            kind = "geobase_city",
            lat = 41.2677f,
            lon = 26.687841f,
            geoid = 104738,
            url = "//yandex.ru/pogoda/104738/"
        )
    )
}
