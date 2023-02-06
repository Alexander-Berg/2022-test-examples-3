/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/match\.json/;

const query = {
    name: 'LED телевизор LG 43LJ510V',
    price: 25999,
    currency: 'RUR',
    categories_count: 3
};

const result = {
    comment: 'text = "LED телевизор LG 43LJ510V"\nprice = 2599\ncurrency = "RUR"',
    status: 200,
    body: {
        time: 1514383741823,
        model: [
            {
                id: 1724547969,
                name: 'LG 43LJ510V',
                kind: 'телевизор',
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    type: 'GURU',
                    advertisingModel: 'CPA'
                },
                photo: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id1791511517718709854/orig',
                type: 'MODEL'
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
