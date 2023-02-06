'use strict';

const _ = require('lodash');
const base = require('./testing');

module.exports = _.merge(base, {
    frontend: {
        startrekUrl: '//tracker.test.yandex.ru',
        staff: {
            ru: 'team.test.yandex.ru',
            com: 'team.test.yandex.com',
        },
    },
    backend: {
        abovemeta: {
            ru: {
                hostname: process.env.BACKEND_HOST || 'bisearch-backend-test.tools.yandex.ru',
            },
            com: {
                hostname: process.env.BACKEND_COM_HOST || 'bisearch-backend-test.tools.yandex.com',
            },
            net: {
                hostname: process.env.BACKEND_HOST || 'bisearch-backend-test.tools.yandex.ru',
            },
        },
        blackbox: {
            protocol: 'https',
            port: 443,
            hostname: 'pass-test.yandex.ru',
        },
        passport: {
            ru: {
                hostname: 'passport-test.yandex.ru',
            },
            com: {
                hostname: 'passport-test.yandex.com',
            },
        },
    },
    tvm: {
        destinations: ['blackbox', 'abovemeta', 'directory'],
    },
    isB2b: true,
    metricaId: 43877744,
    static: {
        prefix: '/static',
    },
});
