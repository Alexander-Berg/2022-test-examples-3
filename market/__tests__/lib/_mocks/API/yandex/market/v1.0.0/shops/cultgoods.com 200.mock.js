/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/shops\.json/;

const query = {
    host: 'cultgoods.com'
};

const result = {
    comment: 'host = "cultgoods.com"',
    status: 200,
    body: {
        time: 1514500540577,
        metadata: {
            filters: {
                geoId: 0,
                host: 'cultgoods.com'
            }
        },
        shops: [
            {
                id: 115587,
                name: 'ИП Тетера Василий Васильевич',
                shopName: 'www.CULTGOODS.com',
                url: 'cultgoods.com',
                status: 'actual',
                rating: 5,
                gradeTotal: 639,
                regionId: 213,
                createdAt: '2012-08-20'
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
