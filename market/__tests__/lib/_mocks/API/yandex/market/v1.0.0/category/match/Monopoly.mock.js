/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/category\/match\.json/;

const query = {
    name: 'Настольная игра MONOPOLY Монополия с банковскими картами, обновленная (B6677)',
    category_name: 'Настольные игры и хобби/Настольные игры/Настольные игры MONOPOLY'
};

const result = {
    comment: 'name = "LED телевизор LG 43LJ510V"\ncategory_name = "Телевизоры, аудио, видео/Телевизоры"',
    status: 200,
    body: {
        time: 1517316175298,
        categories: [
            {
                id: 10682647,
                name: 'Настольные игры',
                type: 'GURU',
                advertisingModel: 'HYBRID',
                rank: 3.6314986425975793 },
            {
                id: 13887809,
                name: 'Шахматы, шашки, нарды',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -6.959287279601282 },
            {
                id: 10682651,
                name: 'Напольные игры и коврики',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -7.215353393003733 },
            {
                id: 13491603,
                name: 'Настольный футбол, хоккей, бильярд',
                type: 'GURU',
                advertisingModel: 'HYBRID',
                rank: -7.272368423330536 },
            {
                id: 14960839,
                name: 'Игры для приставок и ПК',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -7.563953223075583 },
            {
                id: 10790730,
                name: 'Обучающие материалы и авторские методики',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -7.649534520912312 },
            {
                id: 13887899,
                name: 'Домино и лото',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -7.893547360359866 },
            {
                id: 90787,
                name: 'Пазлы',
                type: 'GURU',
                advertisingModel: 'HYBRID',
                rank: -7.968744960784751 },
            {
                id: 10682641,
                name: 'Мозаика',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.011175252107522 },
            {
                id: 10682659,
                name: 'Детские компьютеры',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.15444180655319 },
            {
                id: 90711,
                name: 'Настольные лампы',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.154508145995795 },
            {
                id: 10470548,
                name: 'Конструкторы',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -8.326218055854214 },
            {
                id: 10790731,
                name: 'Наборы для исследований',
                type: 'GURU',
                advertisingModel: 'HYBRID',
                rank: -8.357672687542125 },
            {
                id: 10683227,
                name: 'Игровые наборы и фигурки',
                type: 'GURU',
                advertisingModel: 'HYBRID',
                rank: -8.482749747826839 },
            {
                id: 237420,
                name: 'Плиты',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -8.611386810666048 },
            {
                id: 10682592,
                name: 'Машинки и техника',
                type: 'GURU',
                advertisingModel: 'HYBRID',
                rank: -8.653910801651039 },
            {
                id: 6430985,
                name: 'Аппликация и декорирование',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.69045464189084 },
            {
                id: 14421210,
                name: 'Таблички',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.874123116045117 },
            {
                id: 995787,
                name: 'Карты и программы GPS-навигации',
                type: 'VISUAL',
                advertisingModel: 'CPA',
                rank: -8.929566307577181 },
            {
                id: 294661,
                name: 'GPS-навигаторы',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -8.962027202657152 }

        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
