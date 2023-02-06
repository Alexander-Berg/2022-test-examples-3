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
                host: 'meshok.net',
            },
        },
        id: '1572125498473/1d353a6c3ab69769e0fbe0fdd6950500',
        time: '2019-10-27T00:31:38.474+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4',
    },
    shops: [],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
