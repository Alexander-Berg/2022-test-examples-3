/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/search/;

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
            text: 'url:"avito.ru/sankt-peterburg/bytovaya_tehnika/hlebopech_panasonic_sd-2501_1786340164*"',
            actualText: 'url:"avito.ru/sankt-peterburg/bytovaya_tehnika/hlebopech_panasonic_sd-2501_1786340164*"',
            highlightedText: '',
        },
        id: '1565179934811/2e94948940b44e61f02bb6a791fe1710',
        time: '2019-08-07T15:12:14.902+03:00',
        link:
            'https://market.yandex.ru/search?onstock=0&text=url%3A%22avito.ru%2Fsankt-peterburg%2Fbytovaya_tehnika%2Fhlebopech_panasonic_sd-2501_1786340164*%22&free-delivery=0&how&pp=1002&clid=2210590&distr_type=4',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4',
    },
    items: [],
    categories: [],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
