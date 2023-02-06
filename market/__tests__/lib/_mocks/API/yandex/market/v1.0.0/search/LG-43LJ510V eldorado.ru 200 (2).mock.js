/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/search\.json/;

const query = {
    text: 'url:"eldorado.ru/cat/detail/71241261*"'
};

const result = {
    comment: 'text = "url:"eldorado.ru/cat/detail/71241261*""',
    status: 200,
    body: {
        searchResult: {
            page: 1,
            count: 0,
            total: 0,
            requestParams: {
                text: 'url:"eldorado.ru/cat/detail/71241261*"',
                actualText: 'url:"eldorado.ru/cat/detail/71241261*"',
                checkSpelling: false
            },
            results: [],
            categories: []
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
