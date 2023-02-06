/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/match\.json/;

const query = {
    name: 'Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto',
    price: 1071,
    currency: 'RUR',
    categories_count: 3
};

const result = {
    comment:
        'name = "Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto"\nprice = 1071\ncurrency = "RUR"',
    status: 200,
    body: {
        time: 1518091697105,
        model: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
