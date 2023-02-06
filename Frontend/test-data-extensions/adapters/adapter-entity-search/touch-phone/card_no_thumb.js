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
                        description: 'Американская актриса российского происхождения.',
                        description_source: {
                            code: 0,
                            name: 'Википедия',
                            url: 'http://ru.wikipedia.org/wiki/Ларк, Мария'
                        },
                        entref: '0oCgpydXcyNDg2MjM0GAL8fM9A',
                        filter_rank: 32651,
                        id: 'ruw2486234',
                        ids: {
                            kinopoisk: 'name/1110453',
                            wikipedia: [
                                'http://ru.wikipedia.org/wiki/Ларк, Мария',
                                'http://en.wikipedia.org/wiki/Maria Lark',
                                'http://de.wikipedia.org/wiki/Maria Lark',
                                'http://it.wikipedia.org/wiki/Maria Lark'
                            ]
                        },
                        name: 'Мария Ларк',
                        quality: 5,
                        search_request: 'Мария Ларк',
                        select_rank: 4642,
                        source: {
                            code: 0,
                            name: 'Википедия',
                            url: 'http://ru.wikipedia.org/wiki/Ларк, Мария'
                        },
                        sources: [
                            {
                                code: 0,
                                name: 'Википедия',
                                url: 'http://ru.wikipedia.org/wiki/Ларк, Мария'
                            },
                            {
                                code: 10,
                                name: 'КиноПоиск',
                                url: 'http://kinopoisk.ru/name/1110453'
                            }
                        ],
                        subtitle: 'Американская актриса',
                        title: 'Мария Ларк',
                        type: 'Hum',
                        wikipedia: {
                            de: 'http://de.wikipedia.org/wiki/Maria Lark',
                            en: 'http://en.wikipedia.org/wiki/Maria Lark',
                            it: 'http://it.wikipedia.org/wiki/Maria Lark',
                            ru: 'http://ru.wikipedia.org/wiki/Ларк, Мария'
                        },
                        wtype_id: 3
                    },
                    client: 'nmeta',
                    db: [
                        'main',
                        'assoc_base'
                    ],
                    display_options: {
                        is_explicit: false,
                        log: '1|main-assoc_base|Hum|ruw2486234||ru||',
                        move_video_wizard: 'False',
                        show_badge: 1,
                        show_parent_collection: 0,
                        show_related_object: 1,
                        show_wiki_snippet: 1
                    },
                    tags: [
                        'Actor',
                        'Creative'
                    ],
                    url: [
                        'http://kinopoisk.ru/name/1110453',
                        'http://www.wikidata.org/wiki/q459854',
                        'viaf_id:108088609',
                        'http://ru.wikipedia.org/wiki/Ларк, Мария',
                        'http://pt.wikipedia.org/wiki/Maria Lark',
                        'lcnaf_id:no2010047065',
                        'http://it.wikipedia.org/wiki/Maria Lark',
                        'http://imdb.com/name/nm1833884',
                        'http://en.wikipedia.org/wiki/Maria Lark',
                        'http://de.wikipedia.org/wiki/Maria Lark',
                        'http://www.marialark.com',
                        'http://freebase.com/m/0db44s'
                    ],
                    voiceInfo: {
                        ru: [
                            {
                                lang: 'ru-RU',
                                text: 'По данным русской википедии: Мария Ларк - Американская актриса российского происхождения.'
                            }
                        ]
                    },
                    wiki_snippet: {
                        item: [
                            {
                                key: 'InitDate@on',
                                name: 'Родилась',
                                value: [
                                    {
                                        _source_code: 0,
                                        entref: '0oCgdydXczODc2GAL_eKez',
                                        id: 'ruw3876',
                                        is_worthy_badge: true,
                                        link_text: 'Сибирь',
                                        search_request: 'Сибирь',
                                        text: '20 июня 1997 г. (19 лет), Сибирь'
                                    }
                                ]
                            },
                            {
                                key: 'Height@on',
                                name: 'Рост',
                                value: [
                                    {
                                        _source_code: 10,
                                        text: '157 см'
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
