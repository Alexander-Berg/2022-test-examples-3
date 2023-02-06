require('../../register')();

const {mergeDeepRight} = require('ramda');

const config = require('../../ginny/market-billing-admin');

module.exports = mergeDeepRight(config, {
    baseUrl: 'https://billing-admin-mono.tst.vs.market.yandex.ru/',

    kadavr: {
        pageBlank: '/blank',
    },

    suiteManager: {
        caseFilter: {
            environment: 'testing|all',
        },
    },
});
