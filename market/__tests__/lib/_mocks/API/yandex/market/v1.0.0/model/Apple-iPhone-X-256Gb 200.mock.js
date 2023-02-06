/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/1732210983\.json/;

const query = {};

const result = {
    comment: 'model/1732210983',
    status: 200,
    body: {
        model: {
            id: 1732210983,
            name: 'Apple iPhone X 256GB',
            kind: 'смартфон',
            description: 'GSM, LTE-A, смартфон, iOS 11, вес 174 г, ШхВхТ 70.9x143.6x7.7 мм, экран 5.8\', 2436x1125, Bluetooth, NFC, Wi-Fi, GPS, ГЛОНАСС, фотокамера 12 МП, память 256 Гб',
            categoryId: 91491,
            category: {
                id: 91491,
                type: 'GURU',
                advertisingModel: 'CPA',
                name: 'Мобильные телефоны'
            },
            prices: {
                max: '123990',
                min: '80720',
                avg: '85000',
                curCode: 'RUR',
                curName: 'руб.'
            },
            mainPhoto: {
                url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/orig',
                width: 321,
                height: 620
            },
            previewPhoto: {
                url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/4hq',
                width: 150,
                height: 150
            },
            link: 'https://market.yandex.ru/product/1732210983?hid=91491&pp=1002&clid=2210590&distr_type=4',
            vendorId: 153043,
            vendor: 'Apple',
            isGroup: false,
            reviewsCount: 21,
            rating: 3.5,
            offersCount: 190,
            gradeCount: 88,
            articlesCount: 5,
            isNew: 1,
            photos: {
                photo: [
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/orig',
                        width: 321,
                        height: 620
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1668394135474795668.png/orig',
                        width: 321,
                        height: 620
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/331398/img_id8726999918077059129.png/orig',
                        width: 59,
                        height: 620
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id3300352705198418224.png/orig',
                        width: 321,
                        height: 620
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id977450801477243664.png/orig',
                        width: 321,
                        height: 620
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id5110396935647702049.png/orig',
                        width: 59,
                        height: 620
                    }
                ]
            },
            previewPhotos: [
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id1668394135474795668.png/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/331398/img_id8726999918077059129.png/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id3300352705198418224.png/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id977450801477243664.png/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id5110396935647702049.png/4hq',
                    width: 150,
                    height: 150
                }
            ]
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
