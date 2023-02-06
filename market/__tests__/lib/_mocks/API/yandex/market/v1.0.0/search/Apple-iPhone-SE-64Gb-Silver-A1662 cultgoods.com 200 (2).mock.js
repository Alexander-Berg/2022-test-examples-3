/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/search\.json/;

const query = {
    text: 'url:"www.cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"'
};

const result = {
    comment: 'text = "url:"www.cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*""',
    status: 200,
    body: {
        searchResult: {
            page: 1,
            count: 0,
            total: 0,
            requestParams: {
                text: 'url:"www.cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"',
                actualText: 'url:"www.cultgoods.com/products/smartfon-apple-iphone-se-64gb-silver-a1662*"',
                checkSpelling: false
            },
            results: [],
            categories: []
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
