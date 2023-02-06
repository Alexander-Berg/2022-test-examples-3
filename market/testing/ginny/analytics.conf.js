require('../../register')();

const {mergeDeepRight} = require('ramda');

const config = require('../../ginny/analytics');

module.exports = mergeDeepRight(config, {
    baseUrl: 'https://front-analytics.tst.market.yandex.ru',

    kadavr: {
        pageBlank: '/blank',
        noSessionLog: true,
    },

    allure: {
        targetDir: 'spec/hermione/allure/results',
        reportDir: process.env.REPORTS_DIR || 'spec/hermione/allure/report',
        testsManagementPattern: 'https://testpalm.yandex-team.ru/testcase/%s',
        issuesTrackerPattern: 'https://st.yandex-team.ru/%s',
    },

    suiteManager: {
        caseFilter: {
            environment: 'testing|all',
        },
    },
});
