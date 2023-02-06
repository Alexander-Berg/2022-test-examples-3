/* eslint-disable max-len */

'use strict';

const ApiMock = require('../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/search/;

const query = {
    text: 'url:"www.cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"'
};

const result = {
    comment: 'text = "url:"www.cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*""',
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
                text: 'url:\'www.cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*\'',
                actualText: 'url:\'www.cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*\'',
                highlightedText: '',
                adult: false
            },
            id: '1514501569963/98bddf457c3dabf9c484b32f4ae08ace',
            time: '2017-12-29T01:52:49.996+03:00',
            link: 'https://market.yandex.ru/search?onstock=0&text=url%3A%22www.cultgoods.com%2Fproducts%2Fsmartfon-apple-iphone-se-64gb-silver-a1662*%22&free-delivery=0&how&pp=1002&clid=2210364&distr_type=4',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210364&distr_type=4'
        },
        items: [],
        categories: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
