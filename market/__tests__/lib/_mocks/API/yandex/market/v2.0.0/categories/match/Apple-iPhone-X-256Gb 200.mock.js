/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';
const pathname = /v2\.0\.0\/categories\/match/;

const query = {
    name: 'Мобильный телефон Apple iPhone X 256GB',
    category_name: 'Мобильные и связь/Мобильные и аксессуары/Мобильные телефоны/Apple/Мобильные телефоны/Apple'
};

const result = {
    comment:
        'name = "Мобильный телефон Apple iPhone X 256GB"\ncategory_name = "Мобильные и связь/Мобильные и аксессуары/Мобильные телефоны/Apple/Мобильные телефоны/Apple"',
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
            id: '1520844430721/f10d09202262b09a9f46fabe26e1bbd2',
            time: '2018-03-12T11:47:10.794+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        categories: [
            {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: 2.076076003606003
            },
            {
                id: 10382050,
                name: 'Док-станции',
                fullName: 'Док-станции для мобильных телефонов и умных часов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -1.7272781972087836
            },
            {
                id: 10498025,
                name: 'Умные часы и браслеты',
                fullName: 'Умные часы и браслеты',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -2.835646395095832
            },
            {
                id: 91498,
                name: 'Чехлы',
                fullName: 'Чехлы для мобильных телефонов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'HYBRID',
                rank: -4.811993026815034
            },
            {
                id: 6427100,
                name: 'Планшеты',
                fullName: 'Планшеты',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -5.088015784674267
            },
            {
                id: 6126496,
                name: 'Аксессуары для цифровых плееров',
                fullName: 'Аксессуары для цифровых плееров',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'HYBRID',
                rank: -5.209040878841426
            },
            {
                id: 91074,
                name: 'Компьютерные кабели, разъемы, переходники',
                fullName: 'Компьютерные кабели, разъемы, переходники',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'HYBRID',
                rank: -5.545502234940497
            },
            {
                id: 10834023,
                name: 'Чехлы-аккумуляторы',
                fullName: 'Чехлы-аккумуляторы для телефонов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -5.622374792029936
            },
            {
                id: 2724669,
                name: 'Портативная акустика',
                fullName: 'Портативная акустика',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -5.631608811931999
            },
            {
                id: 91072,
                name: 'Защитные пленки и стекла',
                fullName: 'Защитные пленки и стекла для телефонов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'HYBRID',
                rank: -5.677686720450201
            },
            {
                id: 2662954,
                name: 'Чехлы для планшетов',
                fullName: 'Чехлы для планшетов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'HYBRID',
                rank: -5.812452039641858
            },
            {
                id: 91503,
                name: 'Зарядные устройства и адаптеры',
                fullName: 'Зарядные устройства и адаптеры для мобильных телефонов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'HYBRID',
                rank: -5.837982402178148
            },
            {
                id: 12429672,
                name: 'Объективы',
                fullName: 'Объективы для смартфонов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -5.908196379060329
            },
            {
                id: 90555,
                name: 'Наушники и Bluetooth-гарнитуры',
                fullName: 'Наушники и Bluetooth-гарнитуры',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -5.951260004330442
            },
            {
                id: 288003,
                name: 'USB Flash drive',
                fullName: 'USB Flash drive',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -6.124227735958764
            },
            {
                id: 459013,
                name: 'Запасные части',
                fullName: 'Запасные части для мобильных телефонов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'HYBRID',
                rank: -6.352628042263522
            },
            {
                id: 90560,
                name: 'Цифровые плееры',
                fullName: 'Портативные цифровые плееры',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -6.361664272826521
            },
            {
                id: 91013,
                name: 'Ноутбуки',
                fullName: 'Ноутбуки',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -6.555739238669742
            },
            {
                id: 13776137,
                name: 'Декоративные фонтаны',
                fullName: 'Декоративные фонтаны для сада и дачи',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'CPA',
                rank: -8.7120170106173
            },
            {
                id: 13776278,
                name: 'Декор',
                fullName: 'Декор для фонтанов и прудов',
                type: 'VISUAL',
                childCount: 0,
                advertisingModel: 'HYBRID',
                rank: -8.895065024282939
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
