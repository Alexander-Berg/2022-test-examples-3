module.exports = {
    host: 'https://api.content.market.yandex.ru',
    route: /\/v[0-9.]+\/shops/,
    response: {
        status: 'ERROR',
        context: {
            region: {
                id: 225,
                name: 'Россия',
                type: 'COUNTRY',
                childCount: 10,
                country: {
                    id: 225,
                    name: 'Россия',
                    type: 'COUNTRY',
                    childCount: 10,
                },
            },
            currency: {
                id: 'RUR',
                name: 'руб.',
            },
            id: '1577102790267/31621c8186ede9f07f9296db5d9a0500',
            time: '2019-12-23T15:06:30.27+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002',
        },
        shop: {
            region: {
                id: 213,
                name: 'Москва',
                type: 'CITY',
                childCount: 14,
                country: {
                    id: 225,
                    name: 'Россия',
                    type: 'COUNTRY',
                    childCount: 10,
                },
            },
            id: 720,
            name: 'www.Pleer.ru',
            domain: 'pleer.ru',
            registered: '2003-12-18',
            type: 'DEFAULT',
            opinionUrl: 'https://market.yandex.ru/shop--www-pleer-ru/720/reviews?pp=1002',
            outlets: [],
        },
    },
};
