'use strict';

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/user\/wishlist\/items/;

const RESPONSE = {
    status: 'OK',
    context: {
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
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        page: {
            number: 1,
            count: 10,
            total: 0,
        },
        id: '1553863133529/583a70bb6d8841fcf493067269d38b61',
        time: '2019-03-29T15:38:53.559+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002',
    },
    items: [],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
