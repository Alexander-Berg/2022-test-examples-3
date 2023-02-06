const path = require('path');

module.exports = {
    gridUrl: 'http://default@sw.yandex-team.ru:80/v0',
    baseUrl: 'https://yandex.ru',
    retry: 1,
    sessionsPerBrowser: 1,
    testsPerSession: 10000,

    sets: {
        common: {
            files: [path.join(__dirname, 'tests')],
            browsers: ['chrome', 'firefox'],
        },
        ffonly: {
            files: [path.join(__dirname, 'tests-firefox')],
            browsers: ['firefox'],
        },
    },

    browsers: {
        chrome: {
            desiredCapabilities: {
                browserName: 'chrome',
            },
            resetCursor: false,
        },
        firefox: {
            desiredCapabilities: {
                browserName: 'firefox',
            },
            resetCursor: false,
        },
    },

    plugins: {
        [path.join(__dirname, './reporter.js')]: {},
        [path.join(__dirname, '../index.js')]: {},
    },

    system: {
        debug: false,
    },
};
