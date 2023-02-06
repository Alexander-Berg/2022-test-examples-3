'use strict';

module.exports = {
    type: 'snippet',
    request_text: 'авиабилеты',
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
                    green_tail: '?from=waviablank',
                    green_domain: 'https://avia.yandex.ru',
                    domain_href: 'https://avia.yandex.ru/'
                }
            ]
        },
        favicon_domain: 'avia.yandex.ru',
        green_url: 'https://avia.yandex.ru/?from=waviablank',
        url: 'https://avia.yandex.ru/?from=waviablank',
        server_descr: 'BUYTICKETS',
        markers: {
            WizardPos: '0',
            Rule: 'Vertical/buy_tickets'
        },
        doctitle: 'https://avia.yandex.ru/?from=waviablank',
        url_parts: {
            hostname: 'avia.yandex.ru',
            scheme: 'https',
            cut_www: null,
            anchor: null,
            link: '/?from=waviablank',
            __is_plain: 1,
            query_string: 'from=waviablank',
            path: '/',
            __package: 'YxWeb::Util::Url',
            port: null,
            canonical: 'https://avia.yandex.ru/?from=waviablank'
        },
        host: 'avia.yandex.ru',
        num: '0',
        _markers: [],
        mime: '',
        signed_saved_copy_url: 'http://hghltd.yandex.net/yandbtm?fmode=inject&url=https%3A%2F%2Favia.yandex.ru%2F%3Ffrom%3Dwaviablank&tld=ru&lang=&la=&tm=1486105986&text=%D0%B0%D0%B2%D0%B8%D0%B0%D0%B1%D0%B8%D0%BB%D0%B5%D1%82%D1%8B&l10n=ru',
        is_recent: '1',
        snippets: {
            full: {
                slot: 'full',
                slot_rank: 0,
                type: 'buy_tickets',
                applicable: 1,
                regions: {
                    to: '',
                    from: ''
                },
                template: 'buy_tickets',
                counter_prefix: '/snippet/buy_tickets/',
                serp_info: {
                    slot: 'full',
                    flat: true,
                    type: 'buy_tickets',
                    format: 'json'
                },
                data: {},
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
