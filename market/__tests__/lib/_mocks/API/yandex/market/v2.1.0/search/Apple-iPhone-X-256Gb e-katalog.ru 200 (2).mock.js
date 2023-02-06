/* eslint-disable max-len */

'use strict';

const ApiMock = require('../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/search/;

const query = {
    text: 'url:"e-katalog.ru/APPLE-IPHONE-X-256GB.htm*"'
};

const result = {
    comment: 'text = "url:"e-katalog.ru/APPLE-IPHONE-X-256GB.htm*""',
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
                text: 'url:"e-katalog.ru/APPLE-IPHONE-X-256GB.htm*"',
                actualText: 'url:"e-katalog.ru/APPLE-IPHONE-X-256GB.htm*"',
                highlightedText: '',
                adult: false
            },
            id: '1514495401283/a51afb484b038115c5da6aa73319b435',
            time: '2017-12-29T00:10:01.337+03:00',
            link: 'https://market.yandex.ru/search?onstock=0&text=url%3A%22e-katalog.ru%2FAPPLE-IPHONE-X-256GB.htm*%22&free-delivery=0&how&pp=1002&clid=2210590&distr_type=4',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        items: [],
        categories: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
