/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /\/v2\.1\.4\/models\/1732210983/;

const query = {};

const result = {
    comment: 'model/1732210983',
    status: 200,
    body: {
        status: 'OK',
        context: {
            region: {
                id: 213,
                name: 'Москва',
                type: 'CITY',
                childCount: 14,
                country: {
                    id: 225,
                    name: 'Россия',
                    type: 'COUNTRY',
                    childCount: 10
                }
            },
            currency: {
                id: 'RUR',
                name: 'руб.'
            },
            id: '1540385879729/f7e41d591d4b682eee62e89eea18240a',
            time: '2018-10-24T15:57:59.772+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002&amp;clid=2210590&amp;distr_type=4'
        },
        model: {
            id: 1732210983,
            name: 'Apple iPhone X 256GB',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description: '',
            photo: {
                width: 321,
                height: 620,
                url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/4hq',
            },
            category: {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                viewType: 'GRID'
            },
            price: {
                max: '123990',
                min: '80720',
                avg: '85000',
            },
            vendor: {
                id: 153043,
                name: 'Apple',
                site: 'http://www.apple.com/ru',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                link: 'https://market.yandex.ru/brands/153043?pp=1002&amp;clid=2210590&amp;distr_type=4',
                isFake: false
            },
            rating: {
                value: 3.5,
                count: 88,
                distribution: [
                    {
                        value: 1,
                        count: 5,
                        percent: 8
                    },
                    {
                        value: 2,
                        count: 7,
                        percent: 11
                    },
                    {
                        value: 3,
                        count: 8,
                        percent: 12
                    },
                    {
                        value: 4,
                        count: 13,
                        percent: 20
                    },
                    {
                        value: 5,
                        count: 33,
                        percent: 50
                    }
                ]
            },
            link: 'https://market.yandex.ru/product/1732210983?hid=91491&amp;pp=1002&amp;clid=2210590&amp;distr_type=4',
            modelOpinionsLink: 'https://market.yandex.ru/product/1732210983/reviews?hid=91491&amp;track=partner&amp;pp=1002&amp;clid=2210590&amp;distr_type=4',
            offerCount: 190,
            opinionCount: 21,
            reviewCount: 21
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
