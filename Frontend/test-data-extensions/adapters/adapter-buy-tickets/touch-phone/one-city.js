'use strict';

module.exports = {
    type: 'snippet',
    request_text: 'авиабилеты бангкок',
    data_stub: {
        supplementary: {
            generic: [
                {
                    passages: [],
                    links: {},
                    headline_src: null,
                    headline: '',
                    type: 'generic',
                    applicable: 1,
                    by_link: '',
                    is_generic: 1,
                    attrs: {},
                    template: 'generic',
                    counter_prefix: '/snippet/generic/',
                    passage_attrs: [],
                    types: {
                        kind: 'snippets',
                        main: 'generic',
                        all: [
                            'snippets',
                            'generic'
                        ],
                        extra: []
                    }
                }
            ],
            domain_link: [
                {
                    green_tail: '…/kazan-<b>bangkok<\/b>?…',
                    green_domain: 'https://avia.yandex.ru',
                    domain_href: 'https://avia.yandex.ru/'
                }
            ]
        },
        favicon_domain: 'avia.yandex.ru',
        green_url: 'https://avia.yandex.ru/…/kazan-<b>bangkok<\/b>?…',
        url: 'https://avia.yandex.ru/routes/kzn/bkk/kazan-bangkok?lang=ru&maxDate=2017-03-09&from=aviawizard_pp&minDate=2017-02-07',
        server_descr: 'BUYTICKETS',
        doctitle: '',
        url_parts: {
            hostname: 'avia.yandex.ru',
            scheme: 'https',
            cut_www: null,
            anchor: null,
            link: '/routes/kzn/bkk/kazan-bangkok?lang=ru&maxDate=2017-03-09&from=aviawizard_pp&minDate=2017-02-07',
            __is_plain: 1,
            query_string: 'lang=ru&maxDate=2017-03-09&from=aviawizard_pp&minDate=2017-02-07',
            path: '/routes/kzn/bkk/kazan-bangkok',
            __package: 'YxWeb::Util::Url',
            port: null,
            canonical: 'https://avia.yandex.ru/routes/kzn/bkk/kazan-bangkok?lang=ru&maxDate=2017-03-09&from=aviawizard_pp&minDate=2017-02-07'
        },
        host: 'avia.yandex.ru',
        num: '0',
        _markers: [],
        mime: '',
        signed_saved_copy_url: 'http://hghltd.yandex.net/yandbtm?fmode=inject&url=https%3A%2F%2Favia.yandex.ru%2F%3Ffrom%3Dwaviablank&tld=ru&lang=&la=&tm=1486105986&text=%D0%B0%D0%B2%D0%B8%D0%B0%D0%B1%D0%B8%D0%BB%D0%B5%D1%82%D1%8B%20%D0%B1%D0%B0%D0%BD%D0%B3%D0%BA%D0%BE%D0%BA&l10n=ru',
        is_recent: '1',
        snippets: {
            full: {
                slot: 'full',
                slot_rank: 0,
                type: 'buy_tickets',
                applicable: 1,
                subtype: 'city',
                template: 'buy_tickets',
                counter_prefix: '/snippet/buy_tickets/city/',
                serp_info: {
                    slot: 'full',
                    subtype: 'city',
                    type: 'buy_tickets',
                    flat: false,
                    format: 'json'
                },
                data: {
                    from: {
                        title: 'Казань',
                        id: 43,
                        point_key: 'c43'
                    },
                    to: {
                        title: 'Бангкок',
                        id: 10620,
                        point_key: 'c10620'
                    },
                    button: {
                        url: 'https://avia.yandex.ru/routes/kzn/bkk/kazan-bangkok?lang=ru&maxDate=2017-03-09&from=aviawizard_pp&minDate=2017-02-07',
                        text: 'Найти авиабилеты'
                    },
                    title: {
                        url: 'https://avia.yandex.ru/routes/kzn/bkk/kazan-bangkok?lang=ru&maxDate=2017-03-09&from=aviawizard_pp&minDate=2017-02-07',
                        text: '<b>Авиабилеты</b> Казань – <b>Бангкок</b>'
                    },
                    content: {
                        prices: {
                            minimum_departure: '2017-02-07',
                            average: {
                                currency: 'RUR',
                                value: 19866.43
                            },
                            minimum: {
                                currency: 'RUR',
                                value: 15500
                            },
                            month: 2
                        },
                        text: 'Выбирайте, когда лететь и что смотреть, а билеты на самолёт найдутся на Яндексе. \nБилеты продаются онлайн у партнёров сервиса.',
                        flight_count: 0,
                        type: 'settlement'
                    },
                    subtype: 'city',
                    flags: {},
                    error: null,
                    type: 'city'
                },
                types: {
                    kind: 'wizard',
                    main: 'buy_tickets',
                    all: [
                        'snippets',
                        'buy_tickets'
                    ]
                }
            }
        },
        size: 0
    }
};
