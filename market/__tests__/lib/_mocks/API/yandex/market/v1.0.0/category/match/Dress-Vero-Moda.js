'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/category\/match\.json/;

const query = {
    name: 'Платье, Vero moda',
    domain: 'www.wildberries.ru'
};

const result = {
    comment: 'name = "Платье, Vero moda"',
    status: 200,
    body: {
        time: 1526466443176,
        categories: [
            {
                id: 7811909,
                name: 'Свитеры и кардиганы',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -1.4982752478145223
            },
            {
                id: 7811901,
                name: 'Платья',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -1.5514186347866952
            },
            {
                id: 7811897,
                name: 'Блузки и кофточки',
                type: 'VISUAL',
                advertisingModel: 'HYBRID',
                rank: -3.041229255503053
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
