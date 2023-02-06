/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.0\.0\/models\/match/;

const query = {
    name: 'Мобильный телефон Apple iPhone X 256GB',
    price: 80950,
    category_count: 3
};

const result = {
    comment: 'models/1732210983',
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
            id: '1516803136519/5c8be8f52a0239dc0d984e05e93e32f9',
            time: '2018-01-24T17:12:16.564+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        models: [
            {
                id: 1732210983,
                name: 'Смартфон Apple iPhone X 256GB',
                kind: '',
                type: 'MODEL',
                isNew: false,
                description: 'GSM, LTE-A, смартфон, iOS 11, вес 174 г, ШхВхТ 70.9x143.6x7.7 мм, экран 5.8\', 2436x1125, Bluetooth, NFC, Wi-Fi, GPS, ГЛОНАСС, фотокамера 12 МП, память 256 Гб',
                photo: {
                    width: 321,
                    height: 620,
                    url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638'
                        }
                    ]
                },
                'category': {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                filters: {}
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
