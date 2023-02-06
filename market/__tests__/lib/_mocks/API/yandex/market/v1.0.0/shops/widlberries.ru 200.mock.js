'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/shops\.json/;

const query = {
    host: 'wildberries.ru'
};

const result = {
    comment: 'host = "wildberries.ru"',
    status: 200,
    body: {
        time: 1518088419122,
        metadata: {
            filters: {
                geoId: 0,
                host: 'wildberries.ru'
            }
        },
        shops: [
            {
                id: 396605,
                name: 'ООО «Вайлдберриз»',
                shopName: 'WILDBERRIES.RU',
                url: 'wildberries.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 32489,
                regionId: 76,
                createdAt: '2017-01-09'
            },
            {
                id: 443704,
                name: 'Вайлдберриз',
                shopName: 'WILDBERRIES.RU',
                url: 'wildberries.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 32489,
                regionId: 35,
                createdAt: '2017-10-26'
            },
            {
                id: 443703,
                name: '«ООО «Вайлдберриз»',
                shopName: 'WILDBERRIES.RU',
                url: 'wildberries.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 32489,
                regionId: 54,
                createdAt: '2017-10-26'
            },
            {
                id: 372927,
                name: 'Вайлдберриз',
                shopName: 'WILDBERRIES.RU',
                url: 'wildberries.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 32489,
                regionId: 65,
                createdAt: '2016-08-17'
            },
            {
                id: 4827,
                name: 'Вайлдберриз',
                shopName: 'WILDBERRIES.RU',
                url: 'wildberries.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 32489,
                regionId: 213,
                createdAt: '2007-05-30'
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
