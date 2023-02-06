/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/match\.json/;

const query = {
    name: 'Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
    price: 2299,
    currency: 'RUR',
    categories_count: 3
};

const result = {
    comment: 'name = Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
    status: 200,
    body: {
        time: 1514299430860,
        model: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
