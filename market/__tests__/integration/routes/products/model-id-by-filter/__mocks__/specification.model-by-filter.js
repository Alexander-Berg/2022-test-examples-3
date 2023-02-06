/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/[0-9]+\/specification/;

const RESPONSE = {
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
        id: '1571689543821/86bdf462484977aaf3e8f47c71950500',
        time: '2019-10-21T23:25:43.885+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002',
    },
    status: 'OK',
    link:
        'https://market.yandex.ru/product--elektricheskaia-zubnaia-shchetka-oral-b-vitality-3d-white/364880001/spec?pp=1002',
    specification: [
        {
            name: 'Основные характеристики',
            features: [
                {
                    name: 'Тип',
                    value: 'обычная',
                },
                {
                    name: 'Форма основной насадки',
                    value: 'круглая',
                },
                {
                    name: 'Насадок в комплекте',
                    value: '1',
                },
                {
                    name: 'Типы насадок',
                    value: 'отбеливающая',
                },
                {
                    name: 'Маркерные пометки',
                    value: '1',
                },
                {
                    name: 'Режимы работы',
                    value: 'режим отбеливания',
                },
                {
                    name: 'Максимальная скорость',
                    value: '7600 возвратно-вращательных движений в минуту',
                },
                {
                    name: 'Питание',
                    value: 'от аккумулятора, время работы 28 мин., время зарядки 16 ч.',
                },
                {
                    name: 'Потребляемая мощность',
                    value: '1.2 Вт',
                },
            ],
        },
        {
            name: 'Особенности',
            features: [
                {
                    name: 'Датчик нажима на зуб',
                    value: 'есть',
                },
                {
                    name: 'Индикация',
                    value: 'зарядки, износа щетинок насадки',
                },
                {
                    name: 'Таймер',
                    value: 'есть',
                },
                {
                    name: 'Хранение',
                    value: 'подставка',
                },
                {
                    name: 'Вес',
                    value: '130 г',
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
