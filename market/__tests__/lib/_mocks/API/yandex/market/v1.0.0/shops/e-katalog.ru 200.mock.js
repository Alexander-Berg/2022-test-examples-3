/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/shops\.json/;

const query = {
    host: 'e-katalog.ru'
};

const result = {
    comment: 'host = "e-katalog.ru"',
    status: 200,
    body: {
        time: 1514301971359,
        metadata: {
            filters: {
                geoId: 0,
                host: 'e-katalog.ru'
            }
        },
        shops: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
