/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/category\/match\.json/;

const query = {
    name: 'Мобильный телефон Apple iPhone X 256GB',
    category_name: 'Мобильные и связь/Мобильные и аксессуары/Мобильные телефоны/Apple/Мобильные телефоны/Apple'
};

const result = {
    comment: 'name = "Мобильный телефон Apple iPhone X 256GB"\ncategory_name = "Мобильные и связь/Мобильные и аксессуары/Мобильные телефоны/Apple/Мобильные телефоны/Apple"',
    status: 200,
    body: {
        time: 1514302261612,
        categories: [
            {
                id: 91491,
                name: 'Мобильные телефоны',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: 2.073835906607096
            },
            {
                id: 91498,
                name: 'Чехлы',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -2.413292280467597
            },
            {
                id: 10834023,
                name: 'Чехлы-аккумуляторы',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -2.453011833347297
            },
            {
                id: 91499,
                name: 'Аккумуляторы',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -2.5647943960849346
            },
            {
                id: 2662954,
                name: 'Чехлы для планшетов',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -2.7624361433936127
            },
            {
                id: 10498025,
                name: 'Умные часы и браслеты',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -2.8401946819659987
            },
            {
                id: 459013,
                name: 'Запасные части',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -3.0970974603376
            },
            {
                id: 91072,
                name: 'Защитные пленки и стекла',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -3.6709753134531935
            },
            {
                id: 6427100,
                name: 'Планшеты',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -4.1519760830336985
            },
            {
                id: 90560,
                name: 'Цифровые плееры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -4.191709756269971
            },
            {
                id: 8353924,
                name: 'Универсальные внешние аккумуляторы',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -4.222248731526422
            },
            {
                id: 91033,
                name: 'Жесткие диски, SSD и сетевые накопители',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -4.833372630006405
            },
            {
                id: 12429672,
                name: 'Объективы',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -4.876469854022879
            },
            {
                id: 91503,
                name: 'Зарядные устройства и адаптеры',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -5.0248963434360245
            },
            {
                id: 91013,
                name: 'Ноутбуки',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -5.0932592568870785
            },
            {
                id: 91463,
                name: 'Проводные телефоны',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -5.492524462874188
            },
            {
                id: 91074,
                name: 'Компьютерные кабели, разъемы, переходники',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -5.571744767579883
            },
            {
                id: 90555,
                name: 'Наушники и Bluetooth-гарнитуры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -6.114534836095919
            },
            {
                id: 91011,
                name: 'Настольные компьютеры',
                type: 'GENERAL',
                advertisingModel: 'HYBRID',
                rank: -6.181210806083984
            },
            {
                id: 91303,
                name: 'Декоративные телефоны',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -6.281826979262629
            },
            {
                id: 5081621,
                name: 'Спутниковые телефоны',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -6.67954084908035
            },
            {
                id: 91469,
                name: 'Системные телефоны',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -6.803111017265438
            },
            {
                id: 13793401,
                name: 'Шланги и комплекты для полива',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.354489962783559
            },
            {
                id: 13776137,
                name: 'Декоративные фонтаны',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -8.805244472162634
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
