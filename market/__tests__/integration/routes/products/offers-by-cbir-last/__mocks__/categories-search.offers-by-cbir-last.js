/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/categories\/[0-9]+\/search/;

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
            totalItems: 0,
        },
        processingOptions: {
            adult: false,
            checkSpelled: true,
            text: 'ПОРТУГАЛИЯ, 1000 ЭСКУДО 1996г., СЕРЕБРО! КРАСИВАЯ!',
            actualText: 'ПОРТУГАЛИЯ, 1000 ЭСКУДО 1996 г., СЕРЕБРО! КРАСИВАЯ!',
            highlightedText: 'ПОРТУГАЛИЯ, 1000 ЭСКУДО 1996 г., СЕРЕБРО! КРАСИВАЯ!',
        },
        id: '1572125498474/16b02594225b93130601e1fdd6950500',
        time: '2019-10-27T00:31:38.773+03:00',
        marketUrl: 'https://market.yandex.ru?pp=490&clid=2210590&distr_type=4',
    },
    items: [],
    categories: [],
    sorts: [
        {
            text: 'по популярности',
            options: [],
        },
        {
            text: 'по цене',
            field: 'PRICE',
            options: [
                {
                    id: 'aprice',
                    how: 'ASC',
                    text: 'Сначала дешёвые',
                },
                {
                    id: 'dprice',
                    how: 'DESC',
                    text: 'Сначала дорогие',
                },
            ],
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
