/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/13584123\/offers\.json/;

const query = {
    text: 'Смартфон Apple iPhone SE 64Gb Silver A1662',
    category_id: '91491',
    price_min: '12225',
    price_max: '24449',
    '-1': '12225,24449'
};

const result = {
    comment: 'text = "Смартфон Apple iPhone SE 64Gb Silver A1662"\ncategory_id = "91491"\nprice_min = "12225"\nprice_max = "24449"',
    status: 200,
    body: {
        offers: {
            items: [],
            page: 1,
            total: 0,
            count: 0
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
