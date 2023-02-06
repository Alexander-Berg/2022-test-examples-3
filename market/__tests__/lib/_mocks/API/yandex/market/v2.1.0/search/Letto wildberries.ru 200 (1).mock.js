/* eslint-disable max-len */

'use strict';

const ApiMock = require('../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/search/;

const query = {
    text: 'url:"wildberries.ru/catalog/4726233/detail.aspx*"'
};

const result = {
    comment: 'text = "url:"wildberries.ru/catalog/4726233/detail.aspx*""',
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
            page: {
                number: 1,
                count: 30,
                total: 0,
                totalItems: 0
            },
            processingOptions: {
                checkSpelled: true,
                text: 'url:"wildberries.ru/catalog/4726233/detail.aspx*"',
                actualText: 'url:"wildberries.ru/catalog/4726233/detail.aspx*"',
                highlightedText: '',
                adult: false
            },
            id: '1518088942210/243869c8151a75aac096fb3c5474ebbf',
            time: '2018-02-08T14:22:22.248+03:00',
            link:
                'https://market.yandex.ru/search?onstock=0&text=url%3A%22wildberries.ru%2Fcatalog%2F4726233%2Fdetail.aspx*%22&free-delivery=0&how&pp=1002&clid=2210590&distr_type=4',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        items: [],
        categories: []
    }
};

module.exports = new ApiMock(host, pathname, query, result);
