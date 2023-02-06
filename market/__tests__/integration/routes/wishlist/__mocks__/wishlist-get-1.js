/* eslint-disable max-len */

'use strict';

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/user\/wishlist\/items/;

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
        page: {
            number: 1,
            count: 10,
            total: 1,
        },
        id: '1553863278664/f464f79dffe6ed36da6c51f16f9f43e1',
        time: '2019-03-29T15:41:18.723+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002',
    },
    items: [
        {
            id: 317540941,
            model: {
                id: 1732210983,
                name: 'Смартфон Apple iPhone X 256GB',
                kind: '',
                type: 'MODEL',
                isNew: false,
                description:
                    'тип: смартфон, линейка: iPhone X, iOS, диагональ экрана: 5.5"-5.9", разрешение экрана: 2436×1125, 4G LTE, память: 256 Гб, NFC, количество основных камер: 2, год анонсирования: 2017',
                photo: {
                    width: 321,
                    height: 620,
                    url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPC',
                    viewType: 'GRID',
                },
                link: 'https://market.yandex.ru/product--smartfon-apple-iphone-x-256gb/1732210983?hid=91491&pp=1002',
            },
            createDate: '2019-03-29T15:40:08+03:00',
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
