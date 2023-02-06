'use strict';

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/user\/wishlist\/models\/117391029/;

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
        id: '1553863885463/70d3868e43be290b3b0b5065b0ad2862',
        time: '2019-03-29T15:51:25.565+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002',
    },
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
