/* eslint-disable max-len */

'use strict';

const ApiMock = require('../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/search/;

const query = {
    text: 'url:\"eldorado.ru/cat/detail/71214616*\"'
};

const result = {
    comment: 'text": "url:\"eldorado.ru/cat/detail/71214616*\"',
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
                text: 'url:\'eldorado.ru/cat/detail/71214616*\'',
                actualText: 'url:\'eldorado.ru/cat/detail/71214616*\'',
                highlightedText: '',
                adult: false
            },
            id: '1517325352787/59d6ff94deb940f6825e0427cefd63d3',
            time: '2018-01-30T18:15:52.889+03:00',
            link: 'https://market.yandex.ru/search?onstock=0&text=url%3A%22eldorado.ru%2Fcat%2Fdetail%2F71214616*%22&free-delivery=0&how&pp=1002&clid=2210590&distr_type=4',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        items: [],
        categories: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
