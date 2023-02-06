/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.0\.0\/models\/match/;

const query = {
    name: 'Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
    price: 2299,
    category_count: 3
};

const result = {
    comment: 'name = Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
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
            processingOptions: {},
            id: '1518790422605/99881805c647f106746093aa0ea975a3',
            time: '2018-02-16T17:13:42.639+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        models: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
