package ru.yandex.direct.geosuggest.client

import ru.yandex.direct.geosuggest.client.model.GeoSuggestResponseElement

open class GeoSuggestTestBase {
    val expectedResponse = listOf(
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
        ),
    )
}
