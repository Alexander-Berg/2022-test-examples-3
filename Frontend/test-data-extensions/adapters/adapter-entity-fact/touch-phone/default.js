var avatar = require('@yandex-int/gemini-serp-stubs').imageUrlStub(122, 162);

module.exports = {
    // jscs:disable maximumLineLength
    type: 'snippet',
    data_stub: {
        num: 0,
        "construct" : [
            {
                "base_info" : {
                    "description" : "Советский и российский политический деятель, заместитель председателя Государственной думы, основатель и председатель Либерально-демократической партии России, член Парламентской ассамблеи Совета Европы. Пять раз участвовал в выборах президента России.",
                    "description_source" : {
                        "code" : 0,
                        "name" : "Википедия",
                        "url" : "http://ru.wikipedia.org/wiki/Жириновский, Владимир Вольфович"
                    },
                    "entref" : "0oCghydXcyMzY5NxgC27ktGg",
                    "filter_rank" : 3012518,
                    "id" : "ruw23697",
                    "ids" : {
                        "kinopoisk" : "name/261695",
                        "wikipedia" : [
                            "http://ru.wikipedia.org/wiki/Жириновский, Владимир Вольфович",
                            "http://en.wikipedia.org/wiki/Vladimir Zhirinovsky",
                            "http://tr.wikipedia.org/wiki/Vladimir Jirinovski",
                            "http://uk.wikipedia.org/wiki/Жириновський Володимир Вольфович",
                            "http://id.wikipedia.org/wiki/Vladimir Zhirinovsky",
                            "http://es.wikipedia.org/wiki/Vladímir Zhirinovski",
                            "http://de.wikipedia.org/wiki/Wladimir Wolfowitsch Schirinowski",
                            "http://it.wikipedia.org/wiki/Vladimir Žirinovskij"
                        ],
                        "yamusic" : "artist/3341898"
                    },
                    "image" : {
                        "avatar" : avatar,
                        "avatar_type" : "face",
                        "color_wiz" : {
                            "back" : "#203344",
                            "button" : "#536E7C",
                            "button_text" : "#CAF8FF",
                            "text" : "#9C998E"
                        },
                        "image_search" : "https://yandex.ru/images/search?rpt=simage&noreask=1&source=qa&text=Жириновский",
                        "original" : avatar,
                        "original_size" : {
                            "height" : 570,
                            "width" : 360
                        },
                        "src_page_host" : "yandex.net",
                        "src_page_url" : "https://yandex.ru/images/search?rpt=simage&noreask=1&source=qa&text=Жириновский"
                    },
                    "name" : "Владимир Жириновский",
                    "quality" : 5,
                    "search_request" : "Жириновский",
                    "select_rank" : 972201,
                    "source" : {
                        "code" : 0,
                        "name" : "Википедия",
                        "url" : "http://ru.wikipedia.org/wiki/Жириновский, Владимир Вольфович"
                    },
                    "sources" : [
                        {
                            "code" : 0,
                            "name" : "Википедия",
                            "url" : "http://ru.wikipedia.org/wiki/Жириновский, Владимир Вольфович"
                        },
                        {
                            "code" : 10,
                            "name" : "КиноПоиск",
                            "url" : "http://kinopoisk.ru/name/261695"
                        },
                        {
                            "code" : 99,
                            "name" : "rostwes.ru",
                            "url" : "http://rostwes.ru/vladimir-zhirinovskij/"
                        }
                    ],
                    "subtitle" : [
                        {
                            "text" : "Советский политик"
                        }
                    ],
                    "title" : "Владимир Жириновский",
                    "type" : "Hum",
                    "wikipedia" : {
                        "de" : "http://de.wikipedia.org/wiki/Wladimir Wolfowitsch Schirinowski",
                        "en" : "http://en.wikipedia.org/wiki/Vladimir Zhirinovsky",
                        "es" : "http://es.wikipedia.org/wiki/Vladímir Zhirinovski",
                        "id" : "http://id.wikipedia.org/wiki/Vladimir Zhirinovsky",
                        "it" : "http://it.wikipedia.org/wiki/Vladimir Žirinovskij",
                        "ru" : "http://ru.wikipedia.org/wiki/Жириновский, Владимир Вольфович",
                        "tr" : "http://tr.wikipedia.org/wiki/Vladimir Jirinovski",
                        "uk" : "http://uk.wikipedia.org/wiki/Жириновський Володимир Вольфович"
                    },
                    "wtype_id" : 3
                },
                "counter" : {
                    "path" : "/wiz/entity-fact",
                    "vars" : {
                        "pos" : "pimportant"
                    }
                },
                "requested_facts" : {
                    "item" : [
                        {
                            "key" : "Age@on",
                            "name" : "Возраст",
                            "value" : [
                                {
                                    "_source_code" : 98,
                                    "text" : "70 лет"
                                }
                            ]
                        }
                    ]
                },
                "subtype" : "entity-fact",
                "type" : "entity-fact",
                "ugc" : {
                    "blocks" : [
                        {
                            "questions" : [
                                {
                                    "answers" : [
                                        {
                                            "id" : "wrong_object",
                                            "text" : "Меня интересовало другое"
                                        },
                                        {
                                            "id" : "bad_image",
                                            "text" : "Не подходит картинка"
                                        },
                                        {
                                            "id" : "bad_fact",
                                            "text" : "Ответ ошибочный или неточный"
                                        },
                                        {
                                            "id" : "other_reason",
                                            "text" : "Другая причина"
                                        }
                                    ],
                                    "id" : "facts-common",
                                    "text" : "Почему вам не понравился ответ?",
                                    "type" : "radio"
                                }
                            ]
                        }
                    ],
                    "custom" : {
                        "onlyFeedback" : true
                    },
                    "request" : "BXLiweuSc1R30_Vhb1VaeElK1pensKWirhgiGu_AgouqSO2nESpvU2P8YYxqLk_tXOK-5BLRfLK4wxgn1kh5h0wVEkh11CPTCapf7-Rtpiwr4AOmpuD58dZ0YgwQQWOyEmD9Gv3YsD3YZerOuTYulgM-kqa501W877uEZhdoKsrIdHwmY06p8KcaAVYZ93uAd2RZ9O-X0kTkdXgUQrVKR8LFimvazB8mqbx2z7_x549pDueVHE7-BJSGxk-_bejiW-G84wga1OPj95Y0tlGjixR1vrvF3HyS91l67XVDtevbwprihGwfV1kpaGdSM9Ar2mIGLgeW_U8RNwsxQlOyCV4-jTAVCNXWOwfClCkMBBWT57IlirgtklUZhoQ5TqVuNiuDtyqUUTlwBEQJRDdCF-fRLs4j5p0jIqZLnJpIy5V6DwTEaS841o0TWPmxNCkK"
                }
            }
        ]
    }
    // jscs:enable maximumLineLength
};
