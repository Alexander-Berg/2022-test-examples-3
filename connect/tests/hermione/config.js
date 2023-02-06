const fs = require('fs');
const path = require('path');
const argv = require('yargs').argv;

const commandsDir = path.resolve(__dirname, './commands');
const localport = require('../../server/config/default/ui-test').server.port;
const isCI = Boolean(process.env.CI);

const createDataPath = require('./helpers/createDataPath');
const sanitizePath = require('./helpers/sanitizePath');

const cacheMode = argv['cache-mode'] || 'read';
const testUsers = require('./users');

let {
    SELENIUM_USER,
    SELENIUM_SECRET,
} = process.env;

// TODO DEVTOOLSSUPPORT-2001
SELENIUM_USER = 'selenium';
SELENIUM_SECRET = 'selenium';

function assertWarn(expr, message) {
    if (!expr) {
        console.warn(message); // eslint-disable-line no-console
    }
}

assertWarn(SELENIUM_USER,
    'Define SELENIUM_USER npm run get-config');

assertWarn(SELENIUM_SECRET,
    'Define SELENIUM_SECRET npm run get-config');

global.testUsers = testUsers;

module.exports = {
    gridUrl: `http://${SELENIUM_USER}:${SELENIUM_SECRET}@sg.yandex-team.ru:4444/wd/hub`,
    baseUrl: 'http://localhost',

    httpTimeout: 90000,
    waitTimeout: 10000,
    screenshotOnRejectTimeout: 5000,
    screenshotDelay: 1000,

    sessionsPerBrowser: cacheMode === 'read' ? 5 : 1,
    testsPerSession: cacheMode === 'read' ? 5 : 1,
    retry: isCI ? 5 : 0,

    compositeImage: true,
    calibrate: true,

    system: {
        mochaOpts: {
            slow: 60 * 1000, // 1m
            timeout: 15 * 60 * 1000, // 15m
        },
        ctx: {
            cacheMode,
            testUsers,
        },
    },

    compareOpts: {
        shouldCluster: true,
        stopOnFirstFail: false,
    },

    sets: {
        desktop: {
            files: [
                'tests/hermione/suites/**/*.js',
            ],
        },
    },
    browsers: {
        // Blink
        chrome: {
            windowSize: '1280x1024',
            sessionsPerBrowser: cacheMode === 'read' ? 8 : 1,
            desiredCapabilities: {
                browserName: 'chrome',
                version: '77.0',
                chromeOptions: {
                    prefs: {
                        browser: {
                            enable_spellchecking: false,
                        },
                    },
                },
            },
            meta: { platform: 'desktop' },
        },
        // Gecko
        firefox: {
            windowSize: '1280x1024',
            sessionsPerBrowser: cacheMode === 'read' ? 5 : 1,
            desiredCapabilities: {
                browserName: 'firefox',
                version: '67.0',
                acceptInsecureCerts: true,
            },
            meta: { platform: 'desktop' },
        },
    },
    plugins: {
        '@yandex-int/hermione-testpalm-steps': {
            enabled: true,
        },
        'html-reporter/hermione': {
            enabled: true,
            path: 'tests/hermione/report',
            defaultView: 'failed',
        },
        'stat-reporter/hermione': {
            enabled: true,
        },
        '@yandex-int/tunneler/hermione': {
            enabled: true,
            tunnelerOpts: {
                localport,
                user: process.env.TUNNELER_USERNAME || process.env.USER,
                sshRetries: 5,
                yandexDomainSuffix: 'yandex',
                ports: {
                    min: 1001,
                    max: 65535,
                },
            },
        },
        '@yandex-int/hermione-muted-tests': {
            project: 'connect',
            // настройки для индекса стабильности тестов
            stabilityIndex: {
                output: {
                    enabled: false,
                },
                input: {
                    path: null,
                },
            },
            autoMute: {
                enabled: false,
            },
            sortTestsByStability: {
                enabled: false,
            },
        },
        'json-reporter/hermione': {
            enabled: isCI, // Включаем только для CI
            path: 'tests/hermione/report/hermione-report.json',
        },
        '@yandex-int/wdio-polyfill': {
            enabled: true,
            browsers: {
                firefox: [
                    'moveTo',
                    'buttonUp',
                    'buttonDown',
                    'buttonPress',
                    'getValue',
                    'getAttribute',
                    'timeouts',
                ],
            },
        },
    },

    screenshotsDir: test => sanitizePath(createDataPath('screens', test, true)),

    prepareBrowser(browser) {
        fs.readdirSync(commandsDir)
            .filter(name => path.extname(name) === '.js' && fs.statSync(path.resolve(commandsDir, name)).isFile())
            .forEach(filename => {
                browser.addCommand(path.basename(filename, '.js'), require(path.resolve(commandsDir, filename)));
            });
    },
};
