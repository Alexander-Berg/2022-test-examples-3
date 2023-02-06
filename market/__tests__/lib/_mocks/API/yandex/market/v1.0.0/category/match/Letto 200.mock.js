'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/category\/match\.json/;

const query = {
    name: 'Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto',
    domain: 'www.wildberries.ru'
};

const result = {
    comment: 'name = "Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto"',
    status: 200,
    body: {
        time: 1518085587208,
        categories: [
            {
                id: 12894020,
                name: 'Комплекты',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: 0.638723867913658
            },
            {
                id: 10752690,
                name: 'Постельное белье и комплекты',
                type: 'GURU',
                advertisingModel: 'HYBRID',
                rank: -2.9257784604739348
            },
            {
                id: 12894026,
                name: 'Наволочки',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -4.888531513742836
            },
            {
                id: 14868726,
                name: 'Конверты и спальные мешки',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -6.832743569108284
            },
            {
                id: 2190938,
                name: 'Души, душевые панели, гарнитуры',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.121156164307066
            },
            {
                id: 7812043,
                name: 'Комплекты',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -7.201754848254366
            },
            {
                id: 11911278,
                name: 'Чехлы для мебели и подушек',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -7.228217440148169
            },
            {
                id: 91520,
                name: 'Спальные мешки',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.238125924262845
            },
            {
                id: 91664,
                name: 'Водяные насосы',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.601483239470408
            },
            {
                id: 1003092,
                name: 'Матрасы',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.716859828770558
            },
            {
                id: 91610,
                name: 'Смесители',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -7.767316087546943
            },
            {
                id: 10470551,
                name: 'Пеленки, клеенки',
                type: 'GURU',
                advertisingModel: 'HYBRID',
                rank: -7.82691075465502
            },
            {
                id: 91662,
                name: 'Шлифовальные машины',
                type: 'GURU',
                advertisingModel: 'CPA',
                rank: -8.28586363255871
            },
            {
                id: 13793706,
                name: 'Средства для защиты растений',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.297901217421803
            },
            {
                id: 90716,
                name: 'Зеркала',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.61025888679576
            },
            {
                id: 14454631,
                name: 'Фасонные части',
                type: 'GENERAL',
                advertisingModel: 'HYBRID',
                rank: -8.691407915033817
            },
            {
                id: 7812113,
                name: 'Свитеры и кардиганы',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.843768319799041
            },
            {
                id: 91387,
                name: 'Грибы',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.888118951779305
            },
            {
                id: 7812078,
                name: 'Джинсы',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.903489873206288
            },
            {
                id: 7811909,
                name: 'Свитеры и кардиганы',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.915488945686857
            },
            {
                id: 13626354,
                name: 'Мулине и нитки',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -8.923864904800135
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
