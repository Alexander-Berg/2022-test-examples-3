'use strict';

/* eslint-disable max-len */

const response = {
    status: 'OK',
    context: {
        region: {
            id: 213,
            name: 'Москва',
            type: 'CITY',
            childCount: 14,
            country: 225
        },
        currency: {
            id: 'RUR',
            name: 'руб.'
        },
        page: {
            number: 1,
            count: 30,
            total: 0,
            totalItems: 0
        },
        processingOptions: {
            checkSpelled: true,
            text: 'Платье, Adelin Fostayn',
            actualText: 'Платье, Adelin Fostayn',
            highlightedText: '',
            adult: false
        },
        id: '1518007436608/d37a3a913d7a0410b042a41be9960b3e',
        time: '2018-02-07T15:43:56.683+03:00',
        link:
            'https://market.yandex.ru/search?hid=7811901&pricefrom=744&priceto=1488&onstock=0&fesh=-4827&text=%D0%9F%D0%BB%D0%B0%D1%82%D1%8C%D0%B5%2C+Adelin+Fostayn&free-delivery=0&how&pp=1002&clid=2210590&distr_type=4',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
    },
    items: [],
    categories: []
};

const expected = [];

module.exports = {
    response,
    expected
};
