/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/category\/match\.json/;

const query = {
    name: 'LED телевизор LG 43LJ510V',
    category_name: 'Телевизоры, аудио, видео/Телевизоры'
};

const result = {
    comment: 'name = "LED телевизор LG 43LJ510V"\ncategory_name = "Телевизоры, аудио, видео/Телевизоры"',
    status: 200,
    body: {
        time: 1514385001983,
        categories: [
            {
                id: 90639,
                name: 'Телевизоры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: 0.7319687003523168
            },
            {
                id: 10469630,
                name: 'Рекламные дисплеи и интерактивные панели',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -2.0509925072575053
            },
            {
                id: 90417,
                name: 'Автомобильные телевизоры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -2.6996986050235723
            },
            {
                id: 90633,
                name: 'DVD и Blu-ray плееры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -3.6869664681325833
            },
            {
                id: 90629,
                name: 'Подставки и кронштейны',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -4.5315637490688765
            },
            {
                id: 91052,
                name: 'Мониторы',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -5.031211164548472
            },
            {
                id: 459013,
                name: 'Запасные части',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -5.140514483360736
            },
            {
                id: 90636,
                name: 'Домашние кинотеатры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -5.812866635639823
            },
            {
                id: 91105,
                name: 'TV-тюнеры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -5.866853192627981
            },
            {
                id: 2724669,
                name: 'Портативная акустика',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -6.340003272651064
            },
            {
                id: 278420,
                name: 'Системы караоке',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -6.841859600143174
            },
            {
                id: 91013,
                name: 'Ноутбуки',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.310034640955061
            },
            {
                id: 90555,
                name: 'Наушники и Bluetooth-гарнитуры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.441531542503523
            },
            {
                id: 488061,
                name: 'Кабели и разъемы',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -7.530985594977657
            },
            {
                id: 90554,
                name: 'Усилители и ресиверы',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.8483365843758515
            },
            {
                id: 90578,
                name: 'Кондиционеры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.9695128229550924
            },
            {
                id: 90595,
                name: 'Микроволновые печи',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -8.028788521788393
            },
            {
                id: 91042,
                name: 'Картриджи',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.073669427748506
            },
            {
                id: 90548,
                name: 'Акустические системы',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -8.214385233671857
            },
            {
                id: 12857697,
                name: 'Запчасти для принтеров и МФУ',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.233730776640863
            },
            {
                id: 7776143,
                name: 'Заправка картриджей',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.400134255538877
            },
            {
                id: 12407737,
                name: 'Светодиодные ленты',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.404989042778581
            },
            {
                id: 14375087,
                name: 'Чернила, тонеры, фотобарабаны',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.441285878447909
            },
            {
                id: 91069,
                name: 'Аксессуары для принтеров и МФУ',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.491968001453852
            },
            {
                id: 13239135,
                name: 'Лампы для сушки',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.631392337541945
            },
            {
                id: 13776214,
                name: 'Подсветка',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.710934981391311
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
