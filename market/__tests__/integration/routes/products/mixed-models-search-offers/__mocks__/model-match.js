/* eslint-disable max-len */

'use strict';

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/match/;

const RESPONSE = {
    status: 'OK',
    context: {
        region: {
            id: 213,
            name: 'Москва',
            type: 'CITY',
            childCount: 14,
            country: 225,
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        processingOptions: {
            source: 'MATCHER',
        },
        id: '1565179934811/97e82b9df0e00354ba457b44466295d4',
        time: '2019-08-07T15:12:14.833+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4',
    },
    models: [
        {
            id: 7696024,
            name: 'Хлебопечка Panasonic SD-2501WTS',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'вес выпечки до 1250 г, мощность 550 Вт, программ выпечки: 12, варенье, сладкая выпечка, безглютеновая выпечка, ржаной хлеб, кекс, пшеничный хлеб, хлеб из муки грубого помола, выбор цвета корочки, таймер, регулировка веса выпечки, диспенсер, замес теста, ускоренная выпечка',
            photo: {
                width: 307,
                height: 409,
                url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id2510012562451940697/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13887686',
                    },
                    {
                        id: '14871214',
                        value: '14899397',
                    },
                ],
            },
            category: {
                id: 90600,
                name: 'Хлебопечки',
                fullName: 'Хлебопечки',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            showUid: '',
            filters: {},
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
