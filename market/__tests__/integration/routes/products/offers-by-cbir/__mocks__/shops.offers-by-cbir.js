const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/shops/i;

const RESPONSE = {
    status: 'OK',
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
        processingOptions: {
            filters: {
                geoId: 0,
                host: 'fmarconi.ru',
            },
        },
        id: '1571860682854/382535d2a339c620bf4fa35599950500',
        time: '2019-10-23T22:58:02.857+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4',
    },
    shops: [
        {
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
            id: 301193,
            name: 'Francesco Marconi',
            domain: 'fmarconi.ru',
            registered: '2015-07-13',
            type: 'DEFAULT',
            opinionUrl:
                'https://market.yandex.ru/shop--francesco-marconi/301193/reviews?pp=1002&clid=2210590&distr_type=4',
            outlets: [],
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
