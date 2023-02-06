var stubs = require('@yandex-int/gemini-serp-stubs'),
    params = { color: 'aa0000', patternSize: 25, format: 'png' };

module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        snippets: {
            full: {
                applicable: 1,
                counter_prefix: '/snippet/entity_search/',
                data: {
                    base_info: {
                        description: 'Город в Верхнеуслонском районе Республики Татарстан. Входит в Казанскую агломерацию. В городе расположены Университет Иннополис и особая экономическая зона «Иннополис».',
                        description_source: {
                            code: 0,
                            name: 'Википедия',
                            url: 'http://ru.wikipedia.org/wiki/Иннополис'
                        },
                        entref: '0oCgpydXczNjY4NDg4GAJfkflL',
                        filter_rank: 281026,
                        id: 'ruw3668488',
                        ids: {
                            geoid: '121642',
                            wikipedia: [
                                'http://ru.wikipedia.org/wiki/Иннополис',
                                'http://en.wikipedia.org/wiki/Innopolis'
                            ]
                        },
                        image: {
                            avatar: stubs.imageUrlStub(225, 150, params),
                            avatar_type: 'default',
                            color_wiz: {
                                back: '#A2A9BA',
                                button: '#8F96A8',
                                button_text: '#232A42',
                                text: '#313338'
                            },
                            image_search: 'https://yandex.ru/images/touch/search?rpt=simage&noreask=1&source=qa&text=Иннополис Казань',
                            original: 'http://upload.wikimedia.org/wikipedia/commons/5/55/%D0%98%D0%BD%D0%BD%D0%BE%D0%BF%D0%BE%D0%BB%D0%B8%D1%81._%D0%A2%D0%B5%D1%85%D0%BD%D0%BE%D0%BF%D0%B0%D1%80%D0%BA_%D0%B8%D0%BC._%D0%90.%D0%A1._%D0%9F%D0%BE%D0%BF%D0%BE%D0%B2%D0%B0.JPG',
                            original_size: {
                                height: 3648,
                                width: 5472
                            },
                            src_page_host: 'wikipedia.org',
                            src_page_url: 'https://yandex.ru/images/touch/search?rpt=simage&noreask=1&source=qa&text=Иннополис Казань'
                        },
                        name: 'Иннополис',
                        quality: 5,
                        search_request: 'Иннополис Казань',
                        select_rank: 156916,
                        source: {
                            code: 0,
                            name: 'Википедия',
                            url: 'http://ru.wikipedia.org/wiki/Иннополис'
                        },
                        sources: [
                            {
                                code: 0,
                                name: 'Википедия',
                                url: 'http://ru.wikipedia.org/wiki/Иннополис'
                            }
                        ],
                        subtitle: 'Город в России',
                        title: 'Иннополис',
                        type: 'Geo',
                        wikipedia: {
                            en: 'http://en.wikipedia.org/wiki/Innopolis',
                            ru: 'http://ru.wikipedia.org/wiki/Иннополис'
                        },
                        wsubtype: [
                            'Locality@on'
                        ],
                        wtype_id: 2
                    },
                    client: 'nmeta',
                    db: [
                        'main',
                        'assoc_base'
                    ],
                    display_options: {
                        is_explicit: false,
                        log: '1|main-assoc_base|Geo/Locality|ruw3668488||ru||',
                        move_video_wizard: 'False',
                        show_badge: 1,
                        show_parent_collection: 0,
                        show_related_object: 1,
                        show_wiki_snippet: 1
                    },
                    tags: [
                        'City'
                    ],
                    url: [
                        'http://www.innopolis.ru',
                        'http://geocode-net.int01e.tst.maps.yandex.ru/1.x/ymapsbm1://geo?ll=48.745%2C55.752&spn=0.091%2C0.056&text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%A0%D0%B5%D1%81%D0%BF%D1%83%D0%B1%D0%BB%D0%B8%D0%BA%D0%B0%20%D0%A2%D0%B0%D1%82%D0%B0%D1%80%D1%81%D1%82%D0%B0%D0%BD%2C%20%D0%92%D0%B5%D1%80%D1%85%D0%BD%D0%B5%D1%83%D1%81%D0%BB%D0%BE%D0%BD%D1%81%D0%BA%D0%B8%D0%B9%20%D1%80%D0%B0%D0%B9%D0%BE%D0%BD%2C%20%D0%98%D0%BD%D0%BD%D0%BE%D0%BF%D0%BE%D0%BB%D0%B8%D1%81',
                        'http://www.wikidata.org/wiki/q4201239',
                        'http://ru.wikipedia.org/wiki/Иннополис',
                        'oktmo_id:92620109001',
                        'okato_id:92220000073',
                        'http://en.wikipedia.org/wiki/Innopolis',
                        'http://freebase.com/m/013b9ql8'
                    ],
                    voiceInfo: {
                        ru: [
                            {
                                lang: 'ru-RU',
                                text: 'По данным русской википедии: Иннополис - Город в Верхнеуслонском районе Республики Татарстан. Входит в Казанскую агломерацию.'
                            }
                        ]
                    },
                    wiki_snippet: {
                        item: [
                            {
                                name: 'Погода',
                                value: [
                                    {
                                        search_request: 'Иннополис погода сегодня',
                                        text: '-3°C, Облачно'
                                    }
                                ]
                            },
                            {
                                name: 'Местное время',
                                value: [
                                    {
                                        text: '23 марта, 00:02 '
                                    }
                                ]
                            },
                            {
                                key: 'InitDate@on',
                                name: 'Дата возникновения',
                                value: [
                                    {
                                        _source_code: 0,
                                        text: 'сентябрь 2012 г.'
                                    }
                                ]
                            },
                            {
                                key: 'Population@on',
                                name: 'Население',
                                value: [
                                    {
                                        _source_code: 0,
                                        text: '96 чел.'
                                    }
                                ]
                            },
                            {
                                key: 'PhoneCode@on',
                                name: 'Телефонный код',
                                value: [
                                    {
                                        _source_code: 0,
                                        text: '843'
                                    }
                                ]
                            },
                            {
                                key: 'Area@on',
                                name: 'Площадь',
                                value: [
                                    {
                                        _source_code: 0,
                                        text: '2,2 км²'
                                    }
                                ]
                            }
                        ]
                    }
                },
                serp_info: {
                    format: 'json',
                    type: 'entity_search'
                },
                slot: 'full',
                slot_rank: 0,
                template: 'entity_search',
                type: 'entity_search',
                types: {
                    all: [
                        'snippets',
                        'entity_search'
                    ],
                    kind: 'wizard',
                    main: 'entity_search'
                }
            }
        }
    }
};
