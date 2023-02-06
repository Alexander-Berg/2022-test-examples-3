/* eslint-disable max-len */

'use strict';

const ApiMock = require('../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/search/;

const query = {
    text: 'url:"www.eldorado.ru/cat/detail/71241261*"'
};

const result = {
    comment: 'text = "url:"www.eldorado.ru/cat/detail/71241261*""',
    status: 200,
    body: {
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
                text: 'url:"www.eldorado.ru/cat/detail/71241261*"',
                actualText: 'url:"www.eldorado.ru/cat/detail/71241261*"',
                highlightedText: '',
                adult: false
            },
            id: '1514494724623/843aa6aaedb8b1c3041ed6bba78a6450',
            time: '2017-12-28T23:58:44.671+03:00',
            link: 'https://market.yandex.ru/search?onstock=0&text=url%3A%22www.e-katalog.ru%2FAPPLE-IPHONE-X-256GB.htm*%22&free-delivery=0&how&pp=1002&clid=2210590&distr_type=4',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        items: [],
        categories: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
