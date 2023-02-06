/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/match\.json/;

const query = {
    name: 'Мобильный телефон Apple iPhone X 256GB',
    price: 80950,
    currency: 'RUR',
    categories_count: 3
};

const result = {
    comment: 'name = "Мобильный телефон Apple iPhone X 256GB"\nprice = 80950\ncurrency = "RUR"',
    status: 200,
    body: {
        time: 1514299430860,
        model: [
            {
                id: 1732210983,
                name: 'Apple iPhone X 256GB',
                kind: 'смартфон',
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    type: 'GURU',
                    advertisingModel: 'CPA'
                },
                photo: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/orig',
                type: 'MODEL',
                isNew: true
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
