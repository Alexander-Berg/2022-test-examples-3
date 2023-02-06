'use strict';

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/[0-9]+\/offers/;

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
            count: 30,
            total: 0,
        },
        processingOptions: {
            adult: false,
        },
        id: '1539257707944/165bc87650ac40bcf6951375482f2f4a',
        time: '2018-10-11T14:35:08.006+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4',
    },
    offers: [],
    filters: [
        {
            id: '-17',
            name: 'Рекомендуется производителем',
            type: 'BOOLEAN',
            values: [],
        },
        {
            id: '-9',
            name: 'Наличие скидки',
            type: 'BOOLEAN',
            values: [],
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
