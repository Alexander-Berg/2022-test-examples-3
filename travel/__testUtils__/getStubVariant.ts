import {IResultAviaVariant} from 'selectors/avia/utils/denormalization/variant';
export function getStubVariant() {
    return {
        key: '1908122045KE59241908131110,1908132030KE7191908132250 1908191755KE57441908192035,1908201335KE9231908201650',
        tag: 'KE 5924.2019-08-12T20:45,KE 719.2019-08-13T20:30KE 5744.2019-08-19T17:55,KE 923.2019-08-20T13:35kupibilet1',
        hybrid: false,
        route: [
            [
                {
                    key: '1908122045KE59241908131110',
                    arrival: {
                        local: '2019-08-13T11:10:00',
                        tzname: 'Asia/Seoul',
                        offset: 540,
                    },
                    departure: {
                        local: '2019-08-12T20:45:00',
                        tzname: 'Europe/Moscow',
                        offset: 180,
                    },
                    aviaCompany: {
                        baggageRulesUrl:
                            'https://www.koreanair.com/global/ru/traveling/baggage-services.html',
                        carryonHeight: 20,
                        carryonWidth: 40,
                        costType: 'normal',
                        baggageRules:
                            'Пассажиры, путешествующие эконом-классом, могут взять в салон авиалайнера один дополнительный предмет — ноутбук, мужской портфель или дамскую сумочку. Общий вес вещей, взятых в салон, не должен превышать 12 кг.\r\nПеред вылетом обязательно сверьтесь с правилами авиакомпании на её сайте — особенно если у вас с собой много разных вещей.',
                        id: 312,
                        baggageDimensionsSum: 158,
                        carryonLength: 55,
                    },
                    company: {
                        alliance: null,
                        title: 'Korean Air',
                        url: 'http://www.koreanair.com/',
                        logoSvg:
                            'https://avatars.mds.yandex.net/get-avia/233213/2a0000015a80531ac697bf2beeea5f7fd0b5/svg',
                        color: '#8fbb98',
                        id: 312,
                    },
                    companyTariff: {
                        carryon: true,
                        carryonNorm: 12,
                        baggageAllowed: true,
                        published: true,
                        id: 174,
                        baggageNorm: 23,
                    },
                    from: {
                        code: 'SVO',
                        id: 9600213,
                        phraseFrom: 'Шереметьева',
                        phraseIn: 'Шереметьеве',
                        phraseTo: 'Шереметьево',
                        settlement: {
                            countryId: 225,
                            title: 'Москва',
                            phraseFrom: 'Москвы',
                            phraseIn: 'Москве',
                            phraseTo: 'Москву',
                            id: 213,
                        },
                        stationId: 9600213,
                        stationType: {
                            prefix: 'а/п',
                            title: 'аэропорт',
                        },
                        tType: 'plane',
                        title: 'Шереметьево',
                    },
                    to: {
                        code: 'ICN',
                        id: 9600406,
                        phraseFrom: 'Инчеона',
                        phraseIn: 'Инчеоне',
                        phraseTo: 'Инчеон',
                        settlement: {
                            countryId: 135,
                            title: 'Сеул',
                            phraseFrom: 'Сеула',
                            phraseIn: 'Сеуле',
                            phraseTo: 'Сеул',
                            id: 10635,
                        },
                        stationId: 9600406,
                        stationType: {
                            prefix: 'а/п',
                            title: 'аэропорт',
                        },
                        tType: 'plane',
                        title: 'Инчеон',
                    },
                    number: 'KE 5924',
                    tModel: null,
                    arrivalTime: 1565662200000,
                    departureTime: 1565631900000,
                },
                {
                    key: '1908132030KE7191908132250',
                    arrival: {
                        local: '2019-08-13T22:50:00',
                        tzname: 'Asia/Tokyo',
                        offset: 540,
                    },
                    departure: {
                        local: '2019-08-13T20:30:00',
                        tzname: 'Asia/Seoul',
                        offset: 540,
                    },
                    aviaCompany: {
                        baggageRulesUrl:
                            'https://www.koreanair.com/global/ru/traveling/baggage-services.html',
                        carryonHeight: 20,
                        carryonWidth: 40,
                        costType: 'normal',
                        baggageRules:
                            'Пассажиры, путешествующие эконом-классом, могут взять в салон авиалайнера один дополнительный предмет — ноутбук, мужской портфель или дамскую сумочку. Общий вес вещей, взятых в салон, не должен превышать 12 кг.\r\nПеред вылетом обязательно сверьтесь с правилами авиакомпании на её сайте — особенно если у вас с собой много разных вещей.',
                        id: 312,
                        baggageDimensionsSum: 158,
                        carryonLength: 55,
                    },
                    company: {
                        alliance: null,
                        title: 'Korean Air',
                        url: 'http://www.koreanair.com/',
                        logoSvg:
                            'https://avatars.mds.yandex.net/get-avia/233213/2a0000015a80531ac697bf2beeea5f7fd0b5/svg',
                        color: '#8fbb98',
                        id: 312,
                    },
                    companyTariff: {
                        carryon: true,
                        carryonNorm: 12,
                        baggageAllowed: true,
                        published: true,
                        id: 174,
                        baggageNorm: 23,
                    },
                    from: {
                        code: 'ICN',
                        id: 9600406,
                        phraseFrom: 'Инчеона',
                        phraseIn: 'Инчеоне',
                        phraseTo: 'Инчеон',
                        settlement: {
                            countryId: 135,
                            title: 'Сеул',
                            phraseFrom: 'Сеула',
                            phraseIn: 'Сеуле',
                            phraseTo: 'Сеул',
                            id: 10635,
                        },
                        stationId: 9600406,
                        stationType: {
                            prefix: 'а/п',
                            title: 'аэропорт',
                        },
                        tType: 'plane',
                        title: 'Инчеон',
                    },
                    to: {
                        code: 'HND',
                        id: 9626208,
                        phraseFrom: 'Ханеды',
                        phraseIn: 'Ханеде',
                        phraseTo: 'Ханеду',
                        settlement: {
                            countryId: 137,
                            title: 'Токио',
                            phraseFrom: 'Токио',
                            phraseIn: 'Токио',
                            phraseTo: 'Токио',
                            id: 10636,
                        },
                        stationId: 9626208,
                        stationType: {
                            prefix: 'а/п',
                            title: 'аэропорт',
                        },
                        tType: 'plane',
                        title: 'Ханеда',
                    },
                    number: 'KE 719',
                    tModel: null,
                    arrivalTime: 1565704200000,
                    departureTime: 1565695800000,
                },
            ],
            [
                {
                    key: '1908191755KE57441908192035',
                    arrival: {
                        local: '2019-08-19T20:35:00',
                        tzname: 'Asia/Seoul',
                        offset: 540,
                    },
                    departure: {
                        local: '2019-08-19T17:55:00',
                        tzname: 'Asia/Tokyo',
                        offset: 540,
                    },
                    aviaCompany: {
                        baggageRulesUrl:
                            'https://www.koreanair.com/global/ru/traveling/baggage-services.html',
                        carryonHeight: 20,
                        carryonWidth: 40,
                        costType: 'normal',
                        baggageRules:
                            'Пассажиры, путешествующие эконом-классом, могут взять в салон авиалайнера один дополнительный предмет — ноутбук, мужской портфель или дамскую сумочку. Общий вес вещей, взятых в салон, не должен превышать 12 кг.\r\nПеред вылетом обязательно сверьтесь с правилами авиакомпании на её сайте — особенно если у вас с собой много разных вещей.',
                        id: 312,
                        baggageDimensionsSum: 158,
                        carryonLength: 55,
                    },
                    company: {
                        alliance: null,
                        title: 'Korean Air',
                        url: 'http://www.koreanair.com/',
                        logoSvg:
                            'https://avatars.mds.yandex.net/get-avia/233213/2a0000015a80531ac697bf2beeea5f7fd0b5/svg',
                        color: '#8fbb98',
                        id: 312,
                    },
                    companyTariff: {
                        carryon: true,
                        carryonNorm: 12,
                        baggageAllowed: true,
                        published: true,
                        id: 174,
                        baggageNorm: 23,
                    },
                    from: {
                        code: 'NRT',
                        id: 9600455,
                        phraseFrom: 'Нариты',
                        phraseIn: 'Нарите',
                        phraseTo: 'Нариту',
                        settlement: {
                            countryId: 137,
                            title: 'Токио',
                            phraseFrom: 'Токио',
                            phraseIn: 'Токио',
                            phraseTo: 'Токио',
                            id: 10636,
                        },
                        stationId: 9600455,
                        stationType: {
                            prefix: 'а/п',
                            title: 'аэропорт',
                        },
                        tType: 'plane',
                        title: 'Нарита',
                    },
                    to: {
                        code: 'ICN',
                        id: 9600406,
                        phraseFrom: 'Инчеона',
                        phraseIn: 'Инчеоне',
                        phraseTo: 'Инчеон',
                        settlement: {
                            countryId: 135,
                            title: 'Сеул',
                            phraseFrom: 'Сеула',
                            phraseIn: 'Сеуле',
                            phraseTo: 'Сеул',
                            id: 10635,
                        },
                        stationId: 9600406,
                        stationType: {
                            prefix: 'а/п',
                            title: 'аэропорт',
                        },
                        tType: 'plane',
                        title: 'Инчеон',
                    },
                    number: 'KE 5744',
                    tModel: null,
                    arrivalTime: 1566214500000,
                    departureTime: 1566204900000,
                },
                {
                    key: '1908201335KE9231908201650',
                    arrival: {
                        local: '2019-08-20T16:50:00',
                        tzname: 'Europe/Moscow',
                        offset: 180,
                    },
                    departure: {
                        local: '2019-08-20T13:35:00',
                        tzname: 'Asia/Seoul',
                        offset: 540,
                    },
                    aviaCompany: {
                        baggageRulesUrl:
                            'https://www.koreanair.com/global/ru/traveling/baggage-services.html',
                        carryonHeight: 20,
                        carryonWidth: 40,
                        costType: 'normal',
                        baggageRules:
                            'Пассажиры, путешествующие эконом-классом, могут взять в салон авиалайнера один дополнительный предмет — ноутбук, мужской портфель или дамскую сумочку. Общий вес вещей, взятых в салон, не должен превышать 12 кг.\r\nПеред вылетом обязательно сверьтесь с правилами авиакомпании на её сайте — особенно если у вас с собой много разных вещей.',
                        id: 312,
                        baggageDimensionsSum: 158,
                        carryonLength: 55,
                    },
                    company: {
                        alliance: null,
                        title: 'Korean Air',
                        url: 'http://www.koreanair.com/',
                        logoSvg:
                            'https://avatars.mds.yandex.net/get-avia/233213/2a0000015a80531ac697bf2beeea5f7fd0b5/svg',
                        color: '#8fbb98',
                        id: 312,
                    },
                    companyTariff: {
                        carryon: true,
                        carryonNorm: 12,
                        baggageAllowed: true,
                        published: true,
                        id: 174,
                        baggageNorm: 23,
                    },
                    from: {
                        code: 'ICN',
                        id: 9600406,
                        phraseFrom: 'Инчеона',
                        phraseIn: 'Инчеоне',
                        phraseTo: 'Инчеон',
                        settlement: {
                            countryId: 135,
                            title: 'Сеул',
                            phraseFrom: 'Сеула',
                            phraseIn: 'Сеуле',
                            phraseTo: 'Сеул',
                            id: 10635,
                        },
                        stationId: 9600406,
                        stationType: {
                            prefix: 'а/п',
                            title: 'аэропорт',
                        },
                        tType: 'plane',
                        title: 'Инчеон',
                    },
                    to: {
                        code: 'SVO',
                        id: 9600213,
                        phraseFrom: 'Шереметьева',
                        phraseIn: 'Шереметьеве',
                        phraseTo: 'Шереметьево',
                        settlement: {
                            countryId: 225,
                            title: 'Москва',
                            phraseFrom: 'Москвы',
                            phraseIn: 'Москве',
                            phraseTo: 'Москву',
                            id: 213,
                        },
                        stationId: 9600213,
                        stationType: {
                            prefix: 'а/п',
                            title: 'аэропорт',
                        },
                        tType: 'plane',
                        title: 'Шереметьево',
                    },
                    number: 'KE 923',
                    tModel: null,
                    arrivalTime: 1566309000000,
                    departureTime: 1566275700000,
                },
            ],
        ],
        price: {
            baggage: [
                [
                    {
                        included: {
                            count: 1,
                            source: 'db',
                        },
                        pc: {
                            count: 1,
                            source: 'partner',
                        },
                        wt: {
                            count: 23,
                            source: 'db',
                        },
                    },
                    {
                        included: {
                            count: 1,
                            source: 'db',
                        },
                        pc: {
                            count: 1,
                            source: 'partner',
                        },
                        wt: {
                            count: 23,
                            source: 'db',
                        },
                    },
                ],
                [
                    {
                        included: {
                            count: 1,
                            source: 'db',
                        },
                        pc: {
                            count: 1,
                            source: 'partner',
                        },
                        wt: {
                            count: 23,
                            source: 'db',
                        },
                    },
                    {
                        included: {
                            count: 1,
                            source: 'db',
                        },
                        pc: {
                            count: 1,
                            source: 'partner',
                        },
                        wt: {
                            count: 23,
                            source: 'db',
                        },
                    },
                ],
            ],
            charter: false,
            partner: {
                code: 'kupibilet',
                id: 94,
                logoSvg: null,
                title: 'kupibilet',
            },
            queryTime: 11.5300118923,
            tariff: {
                currency: 'RUR',
                value: 70790,
            },
        },
        forwardRoute: 'KE 5924.2019-08-12T20:45,KE 719.2019-08-13T20:30',
        backwardRoute: 'KE 5744.2019-08-19T17:55,KE 923.2019-08-20T13:35',
        hasBaggage: true,
    } as IResultAviaVariant;
}
