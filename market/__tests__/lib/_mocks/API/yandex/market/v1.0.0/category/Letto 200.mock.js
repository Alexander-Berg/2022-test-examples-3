'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/category\/12894020\.json/;

const query = {
    geo_id: 213,
    remote_ip: ''
};

const result = {
    comment: 'name = "Комплект постельного Лисички, 1,5-спальный, наволочка 50*70, хлопок, Letto"',
    status: 200,
    body: {
        category: {
            id: 12894020,
            name: 'Комплекты',
            uniqName: 'Комплекты постельного белья',
            parentId: 12894017,
            childrenCount: 0,
            offersNum: 178179,
            modelsNum: 0,
            visual: false,
            viewType: 'LIST',
            guru: false,
            type: 'nonguru',
            advertisingModel: 'HYBRID'
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
