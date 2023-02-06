/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.0\.0\/models\/match/;

const query = {
    name: 'LED телевизор LG 43LJ510V',
    price: 25999,
    category_count: 3
};

const result = {
    comment: 'models/1724547969',
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
            processingOptions: {
                source: 'MATCHER'
            },
            id: '1516809394802/2f8a388c29fe1daca1336115837a39a2',
            time: '2018-01-24T18: 56: 34.848+03: 00',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        models: [
            {
                id: 1724547969,
                name: 'LG 43LJ510V',
                kind: 'телевизор',
                type: 'MODEL',
                isNew: false,
                description: 'ЖК-телевизор, LED, 43\', 1920x1080, 1080p Full HD, мощность звука 10 ВтHDMI x2',
                photo: {
                    width: 701,
                    height: 466,
                    url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id1791511517718709854/orig'
                },
                category: {
                    id: 90639,
                    name: 'Телевизоры',
                    fullName: 'Телевизоры',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'LIST'
                },
                filters: {

                }
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
