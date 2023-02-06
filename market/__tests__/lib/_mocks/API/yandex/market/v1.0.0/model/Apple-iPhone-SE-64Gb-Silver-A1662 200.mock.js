/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/13584123\.json/;

const query = {};

const result = {
    comment: 'model/13584123',
    status: 200,
    body: {
        model: {
            id: 13584123,
            name: 'Apple iPhone SE 64GB',
            kind: 'смартфон',
            description: 'GSM, LTE, смартфон, iOS 10, вес 113 г, ШхВхТ 58.6x123.8x7.6 мм, экран 4\', 1136x640, Bluetooth, NFC, Wi-Fi, GPS, ГЛОНАСС, фотокамера 12 МП, память 64 Гб, аккумулятор 1624 мА⋅ч',
            categoryId: 91491,
            category: {
                id: 91491,
                type: 'GURU',
                advertisingModel: 'CPA',
                name: 'Мобильные телефоны'
            },
            prices: {
                max: '33990',
                min: '24450',
                avg: '26790',
                curCode: 'RUR',
                curName: 'руб.'
            },
            mainPhoto: {
                url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id4712451804472673330/orig',
                width: 354,
                height: 701
            },
            previewPhoto: {
                url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id4712451804472673330/4hq',
                width: 150,
                height: 150
            },
            facts: {
                pro: [
                    'Аккумулятор',
                    'Экран',
                    'Скорость',
                    'Удобство использования',
                    'Камера'
                ],
                contra: [
                    'Конструкция',
                    'Цена'
                ]
            },
            link: 'https://market.yandex.ru/product/13584123?hid=91491&pp=1002&clid=2210364&distr_type=4',
            vendorId: 153043,
            vendor: 'Apple',
            isGroup: false,
            reviewsCount: 95,
            rating: 4.5,
            offersCount: 19,
            gradeCount: 163,
            articlesCount: 25,
            isNew: 0,
            photos: {
                photo: [
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id4712451804472673330/orig',
                        width: 354,
                        height: 701
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id2910472172872553778/orig',
                        width: 344,
                        height: 701
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id735522111096421559/orig',
                        width: 74,
                        height: 701
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id4311969669872198158/orig',
                        width: 404,
                        height: 516
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id2463048659596931832/orig',
                        width: 387,
                        height: 701
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id3636153105481442155/orig',
                        width: 289,
                        height: 701
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id8087490892847982813/orig',
                        width: 575,
                        height: 442
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id7263877666878508290/orig',
                        width: 595,
                        height: 361
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id4296801506211466925/orig',
                        width: 701,
                        height: 638
                    }
                ]
            },
            previewPhotos: [
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id4712451804472673330/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id2910472172872553778/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id735522111096421559/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id4311969669872198158/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id2463048659596931832/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id3636153105481442155/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/466729/img_id8087490892847982813/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id7263877666878508290/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/96484/img_id4296801506211466925/4hq',
                    width: 150,
                    height: 150
                }
            ]
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
