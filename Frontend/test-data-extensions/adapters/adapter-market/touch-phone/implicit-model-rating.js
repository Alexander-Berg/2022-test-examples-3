'use strict';

let stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    data_stub: {
        num: '0',
        snippets: {
            main: {
                passages: [],
                attrs: {},
                types: {
                    kind: 'snippets',
                    extra: [],
                    all: ['snippets', 'generic'],
                    main: 'generic'
                },
                template: 'generic',
                is_generic: 1,
                headline_src: null,
                counter_prefix: '/snippet/generic/',
                applicable: 1,
                passage_attrs: [],
                by_link: '',
                type: 'generic',
                links: {},
                headline: ''
            }
        },
        doctitle: '//market.yandex.ru/search?text=iphone%206&clid=698',
        url_parts: {
            link: '/search?text=iphone%206&clid=698',
            canonical: 'http://market.yandex.ru/search?text=iphone%206&clid=698',
            cut_www: null,
            query_string: 'text=iphone%206&clid=698',
            __package: 'YxWeb::Util::Url',
            path: '/search',
            hostname: 'market.yandex.ru',
            scheme: 'http',
            port: null,
            __is_plain: 1,
            anchor: null
        },
        size: 0,
        construct: [
            {
                priority: '9',
                model_count: '10',
                favicon: {
                    faviconDomain: 'market.yandex.ru'
                },
                sitelinks: [
                    {
                        urlTouch: '//m.market.yandex.ru/search?hid=91491&nid=54726&text=iphone%206&clid=721',
                        text: 'Мобильные телефоны',
                        hint: '49',
                        url: '//market.yandex.ru/search?hid=91491&nid=54726&text=iphone%206&clid=698'
                    },
                    {
                        urlTouch: '//m.market.yandex.ru/search?hid=91072&nid=56030&text=iphone%206&clid=721',
                        text: 'Защитные пленки и стекла',
                        hint: '1151',
                        url: '//market.yandex.ru/search?hid=91072&nid=56030&text=iphone%206&clid=698'
                    },
                    {
                        urlTouch: '//m.market.yandex.ru/search?hid=91498&nid=56036&text=iphone%206&clid=721',
                        text: 'Чехлы',
                        hint: '535',
                        url: '//market.yandex.ru/search?hid=91498&nid=56036&text=iphone%206&clid=698'
                    }
                ],
                urlTouch: '//m.market.yandex.ru/search?text=iphone%206&clid=721',
                subtype: 'market_implicit_model',
                greenUrl: [
                    {
                        urlTouch: '//m.market.yandex.ru?clid=721',
                        text: 'market.yandex.ru',
                        url: '//market.yandex.ru?clid=698'
                    },
                    {
                        urlTouch: '//m.market.yandex.ru/search?text=iphone%206&clid=721',
                        text: '\u0007[Iphone\u0007] \u0007[6\u0007]',
                        url: '//market.yandex.ru/search?text=iphone%206&clid=698'
                    }
                ],
                text: [
                    '\u0007[Iphone\u0007] \u0007[6\u0007] — сравнить модели и купить в проверенном магазине. В наличии популярные новинки и лидеры продаж. Поиск по параметрам, удобное сравнение моделей и цен.'
                ],
                url: '//market.yandex.ru/search?text=iphone%206&clid=698',
                type: 'market_constr',
                title: '\u0007[Iphone\u0007] \u0007[6\u0007] на Маркете',
                button: [
                    {
                        urlTouch: '//m.market.yandex.ru/search?text=iphone%206&clid=721',
                        text: 'Еще 456 предложений',
                        url: '//market.yandex.ru/search?text=iphone%206&clid=698'
                    }
                ],
                counter: {
                    path: '/snippet/market/market_implicit_model'
                },
                shop_count: '121',
                showcase: {
                    widgets: [],
                    isAdv: 0,
                    top_vendors: [],
                    top_models: [],
                    items: [
                        {
                            title: {
                                urlForCounter: '',
                                urlTouch: '//m.market.yandex.ru/product/1724554654?hid=91491&nid=54726&clid=721',
                                text: 'Apple \u0007[iPhone\u0007] \u0007[6\u0007] 32GB',
                                url: '//market.yandex.ru/product/1724554654?hid=91491&nid=54726&clid=698'
                            },
                            price: {
                                priceMinCent: '0',
                                currency: 'RUR',
                                type: 'min',
                                priceMin: '21600'
                            },
                            thumb: {
                                width: '100',
                                source: stubs.imageUrlStub(120, 120),
                                retinaSource: stubs.imageUrlStub(240, 240),
                                height: '100',
                                urlForCounter: '',
                                urlTouch: '//m.market.yandex.ru/product/1724554654?hid=91491&nid=54726&clid=721',
                                text: '',
                                url: '//market.yandex.ru/product/1724554654?hid=91491&nid=54726&clid=698'
                            },
                            rating: {
                                value: '4.5'
                            }
                        },
                        {
                            title: {
                                urlForCounter: '',
                                urlTouch: '//m.market.yandex.ru/product/12859246?hid=91491&nid=54726&clid=721',
                                text: 'Apple \u0007[iPhone\u0007] \u0007[6\u0007]S 128GB',
                                url: '//market.yandex.ru/product/12859246?hid=91491&nid=54726&clid=698'
                            },
                            price: {
                                priceMinCent: '0',
                                currency: 'RUR',
                                type: 'min',
                                priceMin: '39895'
                            },
                            thumb: {
                                width: '100',
                                source: stubs.imageUrlStub(120, 120),
                                retinaSource: stubs.imageUrlStub(240, 240),
                                height: '100',
                                urlForCounter: '',
                                urlTouch: '//m.market.yandex.ru/product/12859246?hid=91491&nid=54726&clid=721',
                                text: '',
                                url: '//market.yandex.ru/product/12859246?hid=91491&nid=54726&clid=698'
                            },
                            rating: {
                                value: '3.5'
                            }
                        },
                        {
                            title: {
                                urlForCounter: '',
                                urlTouch: '//m.market.yandex.ru/product/12859245?hid=91491&nid=54726&clid=721',
                                text: 'Apple \u0007[iPhone\u0007] \u0007[6\u0007]S 64GB',
                                url: '//market.yandex.ru/product/12859245?hid=91491&nid=54726&clid=698'
                            },
                            price: {
                                priceMinCent: '0',
                                currency: 'RUR',
                                type: 'min',
                                priceMin: '35790'
                            },
                            thumb: {
                                width: '100',
                                source: stubs.imageUrlStub(120, 120),
                                retinaSource: stubs.imageUrlStub(240, 240),
                                height: '100',
                                urlForCounter: '',
                                urlTouch: '//m.market.yandex.ru/product/12859245?hid=91491&nid=54726&clid=721',
                                text: '',
                                url: '//market.yandex.ru/product/12859245?hid=91491&nid=54726&clid=698'
                            },
                            rating: {
                                value: '4'
                            }
                        },
                        {
                            title: {
                                urlForCounter: '',
                                urlTouch: '//m.market.yandex.ru/product/12858631?hid=91491&nid=54726&clid=721',
                                text: 'Apple \u0007[iPhone\u0007] \u0007[6\u0007]S Plus 16GB',
                                url: '//market.yandex.ru/product/12858631?hid=91491&nid=54726&clid=698'
                            },
                            price: {
                                priceMinCent: '0',
                                currency: 'RUR',
                                type: 'min',
                                priceMin: '28690'
                            },
                            thumb: {
                                width: '100',
                                source: stubs.imageUrlStub(120, 120),
                                retinaSource: stubs.imageUrlStub(240, 240),
                                height: '100',
                                urlForCounter: '',
                                urlTouch: '//m.market.yandex.ru/product/12858631?hid=91491&nid=54726&clid=721',
                                text: '',
                                url: '//market.yandex.ru/product/12858631?hid=91491&nid=54726&clid=698'
                            },
                            rating: {
                                value: '3.5'
                            }
                        }
                    ]
                }
            }
        ],
        is_recent: 0,
        url: '//market.yandex.ru/search?text=iphone%206&clid=806',
        green_url: 'â¦/search?text=\u0007[iphone\u0007] \u0007[6\u0007]&clid=698',
        supplementary: {
            domain_link: [
                {
                    green_tail: 'search?text=\u0007[iphone\u0007] \u0007[6\u0007]&clid=698',
                    domain_href: 'http://market.yandex.ru/?clid=806',
                    green_domain: 'â¦'
                }
            ],
            generic: [
                {
                    passages: [],
                    attrs: {},
                    types: {
                        kind: 'snippets',
                        extra: [],
                        all: ['snippets', 'generic'],
                        main: 'generic'
                    },
                    template: 'generic',
                    is_generic: 1,
                    headline_src: null,
                    counter_prefix: '/snippet/generic/',
                    applicable: 1,
                    passage_attrs: [],
                    by_link: '',
                    type: 'generic',
                    links: {},
                    headline: ''
                }
            ],
            url_menu: []
        },
        signed_saved_copy_url: 'http://hghltd.yandex.net/yandbtm',
        server_descr: 'MARKET',
        _markers: [],
        relevance: '-1',
        host: 'market.yandex.ru',
        favicon_domain: 'market.yandex.ru',
        markers: {
            blndrViewType: 'market_implicit_model',
            Rule: 'Vertical/market',
            WizardPos: '0'
        },
        mime: ''
    }
};
