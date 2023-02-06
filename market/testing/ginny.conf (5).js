const {merge} = require('lodash');

const {kadavrHost, kadavrPort} = require('./node');
const commonConfig = require('../ginny/common.conf');

module.exports = merge({}, commonConfig, {
    baseUrl: 'https://vendor.market.fslb.yandex.ru/',

    kadavr: {
        host: kadavrHost,
        port: kadavrPort,
        pageBlank: '/ping',
        noSessionLog: true,
    },

    hermione: {
        retry: 3,
        sessionsPerBrowser: 5,
        // @see https://st.yandex-team.ru/DEVTOOLSSUPPORT-9526
        testsPerSession: 20,
    },

    gemini: {
        system: {
            ctx: {
                environment: 'testing',
            },
        },
    },

    suiteManager: {
        caseFilter: {
            environment: 'testing|all',
        },
    },
});
