/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/category\/match\.json/;

const query = {
    name: 'Смартфон Apple iPhone SE 64Gb Silver A1662',
    domain: 'cultgoods.com'
};

const result = {
    comment: 'name = "Смартфон Apple iPhone SE 64Gb Silver A1662"\ndomain = "cultgoods.com"',
    status: 200,
    body: {
        time: 1514498181180,
        categories: [
            {
                id: 91491,
                name: 'Мобильные телефоны',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: 0.8214808737111632
            },
            {
                id: 10834023,
                name: 'Чехлы-аккумуляторы',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -1.2889164355594547
            },
            {
                id: 91498,
                name: 'Чехлы',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -1.5030274759601436
            },
            {
                id: 91503,
                name: 'Зарядные устройства и адаптеры',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -2.697322345242016
            },
            {
                id: 288003,
                name: 'USB Flash drive',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -3.0610756887990034
            },
            {
                id: 10382050,
                name: 'Док-станции',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -3.2504415012863985
            },
            {
                id: 90560,
                name: 'Цифровые плееры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -3.7365874743167895
            },
            {
                id: 459013,
                name: 'Запасные части',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -3.7973230687504733
            },
            {
                id: 91011,
                name: 'Настольные компьютеры',
                type: 'GENERAL',
                advertisingModel: 'HYBRID',
                rank: -4.163240497962594
            },
            {
                id: 90555,
                name: 'Наушники и Bluetooth-гарнитуры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -4.193054752665068
            },
            {
                id: 91013,
                name: 'Ноутбуки',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -4.209948922289753
            },
            {
                id: 6427100,
                name: 'Планшеты',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -4.482780916524501
            },
            {
                id: 91033,
                name: 'Жесткие диски, SSD и сетевые накопители',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -4.5520372501452595
            },
            {
                id: 12429672,
                name: 'Объективы',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -6.012571238017619
            },
            {
                id: 13776137,
                name: 'Декоративные фонтаны',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -7.878839825566872
            },
            {
                id: 13793401,
                name: 'Шланги и комплекты для полива',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.304911368938958
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
