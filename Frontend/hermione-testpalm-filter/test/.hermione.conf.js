const path = require('path');

module.exports = {
    gridUrl: 'http://default@sw.yandex-team.ru:80/v0',
    baseUrl: 'https://yandex.ru',
    retry: 0,
    sessionsPerBrowser: 1,
    testsPerSession: 10000,

    sets: {
        common: {
            files: [path.join(__dirname, 'tests')],
            browsers: ['chrome'],
        },
    },

    browsers: {
        chrome: {
            desiredCapabilities: {},
            resetCursor: false,
        },
    },

    plugins: {
        [path.join(__dirname, '../index.js')]: {},
        [path.join(__dirname, './reporter.js')]: {},
    },

    system: {
        debug: false,
    },
};
