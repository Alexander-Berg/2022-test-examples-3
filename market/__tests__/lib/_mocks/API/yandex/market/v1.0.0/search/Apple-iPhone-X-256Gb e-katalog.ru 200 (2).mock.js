/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/search\.json/;

const query = {
    text: 'url:"e-katalog.ru/APPLE-IPHONE-X-256GB.htm*"'
};

const result = {
    comment: 'text = "url:"e-katalog.ru/APPLE-IPHONE-X-256GB.htm*""',
    status: 200,
    body: {
        searchResult: {
            page: 1,
            count: 0,
            total: 0,
            requestParams: {
                text: 'url:"e-katalog.ru/APPLE-IPHONE-X-256GB.htm*"',
                actualText: 'url:"e-katalog.ru/APPLE-IPHONE-X-256GB.htm*"',
                checkSpelling: false
            },
            results: [],
            categories: []
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
