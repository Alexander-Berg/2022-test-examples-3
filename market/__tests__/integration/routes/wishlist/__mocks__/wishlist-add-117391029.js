/* eslint-disable max-len */

'use strict';

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/user\/wishlist\/models\/117391029/;

const RESPONSE = {
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
                childCount: 10,
            },
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        id: '1553863646817/c57785be6ac64b8b86570dca1e6f3eeb',
        time: '2019-03-29T15:47:26.914+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002',
    },
    items: [
        {
            id: 317541611,
            model: {
                id: 117391029,
                name: 'Ноутбук Apple MacBook Pro 15 with Retina display Mid 2018',
                kind: '',
                type: 'GROUP',
                isNew: false,
                description: '15.4 ", AMD Radeon Pro 560, 1.83 кг, DVD нет, 4G LTE — нет, Bluetooth, Wi-Fi',
                photo: {
                    width: 701,
                    height: 646,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1336510/img_id3048877379063872469.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898641',
                        },
                        {
                            id: '14871214',
                            value: '15277521',
                        },
                    ],
                },
                category: {
                    id: 91013,
                    name: 'Ноутбуки',
                    fullName: 'Ноутбуки',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPC',
                    viewType: 'LIST',
                },
                link:
                    'https://market.yandex.ru/product--noutbuk-apple-macbook-pro-15-with-retina-display-mid-2018/117391029?hid=91013&pp=1002',
                modificationCount: 3,
            },
            createDate: '2019-03-29T15:47:26.873+03:00',
            labels: [
                {
                    id: 226722646,
                    name: 'Список покупок',
                },
            ],
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
