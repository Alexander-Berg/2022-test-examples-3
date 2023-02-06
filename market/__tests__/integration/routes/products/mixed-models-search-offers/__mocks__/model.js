/* eslint-disable max-len */

'use strict';

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/[0-9]+/;

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
        id: '1565179934817/4ee5d8542da6f1fcd58c92b056c4a1a3',
        time: '2019-08-07T15:12:14.877+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4',
    },
    model: {
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
        price: {
            max: '17099',
            min: '11706',
            avg: '13210',
        },
        vendor: {
            id: 153082,
            name: 'Panasonic',
            site: 'http://www.panasonic.ru',
            picture: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id5365692426735929740/orig',
            recommendedShops: 'http://www.panasonic.ru/recom_shops',
            link: 'https://market.yandex.ru/brands--panasonic/153082?pp=1002&clid=2210590&distr_type=4',
            isFake: false,
        },
        rating: {
            value: 4.5,
            count: 477,
            distribution: [
                {
                    value: 1,
                    count: 9,
                    percent: 3,
                },
                {
                    value: 2,
                    count: 6,
                    percent: 2,
                },
                {
                    value: 3,
                    count: 5,
                    percent: 2,
                },
                {
                    value: 4,
                    count: 27,
                    percent: 9,
                },
                {
                    value: 5,
                    count: 248,
                    percent: 84,
                },
            ],
        },
        link:
            'https://market.yandex.ru/product--khlebopechka-panasonic-sd-2501wts/7696024?hid=90600&pp=1002&clid=2210590&distr_type=4',
        modelOpinionsLink:
            'https://market.yandex.ru/product--khlebopechka-panasonic-sd-2501wts/7696024/reviews?hid=90600&track=partner&pp=1002&clid=2210590&distr_type=4',
        offerCount: 48,
        opinionCount: 295,
        reviewCount: 2,
        reasonsToBuy: [
            {
                factor_name: 'удобство модели',
                type: 'consumerFactor',
                factor_id: '687',
                value: 0.9723756909,
                factor_priority: '1',
                id: 'best_by_factor',
            },
            {
                factor_name: 'количество режимов',
                type: 'consumerFactor',
                factor_id: '685',
                value: 0.9213483334,
                factor_priority: '2',
                id: 'best_by_factor',
            },
            {
                value: 1.552966952,
                type: 'consumerFactor',
                id: 'bestseller',
            },
            {
                value: 93.17801666,
                type: 'statFactor',
                id: 'bought_n_times',
            },
            {
                value: 0.9605262876,
                type: 'consumerFactor',
                id: 'customers_choice',
            },
            {
                author_puid: '201891631',
                feedback_id: '41313455',
                value: 5,
                type: 'consumerFactor',
                text:
                    'Прекрасная печь. И хлеб печет, и тесто месит, и варенье варит. Моя помощница на кухне! Хлеб получился идеальный с первого раза. Теперь у нас ни одно утро не начинается без свежего хлеба. Даже гости предпочитают наш хлеб к чаю, вместо "магазиных печенюшек". По состоянию здоровья, не могу месить тесто руками, а в моей семье все любят печеное, так эта печка просто спасение - тесто месит превосходное, два часа и тесто для пирожков готово, идеально вымешанное и поднявшееся до нужной кондиции, а про тесто для пельмений и вареников даже говорит не стоит - оно супер! А варенье! Просто супер! Попробовали все рецепты, которые прилагались к печке, кроме заварного ржаного хлеба, т.к. солод не могу купить в нашем городе. Есть любимые, например французский и с изюмом. А такой кулич мы вообще никогда не ели. Если хлебопечь, то только Панасоник. Лучшая печь, для лучшего хлеба.',
                id: 'positive_feedback',
            },
            {
                value: 7264,
                type: 'statFactor',
                id: 'viewed_n_times',
            },
        ],
        showUid: '',
    },
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
