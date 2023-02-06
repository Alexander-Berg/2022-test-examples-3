const path = require('path');

const {
    prepareBrowser,
    modifyPluginsPaths,
} = require('@yandex-market/ginny/lib/commands/hermione');

const {
    allure,
    screenshotsDir,
} = require('./ginny2.conf');

const {
    kadavrHost,
    kadavrPort,
} = require('./node');

const babelrc = require('../../../../babelrc.node');
const {hermioneSystemDebug} = require('@self/root/configs/ginny/configGetters/hermioneSystemDebug');
const {suiteManagerCommon} = require('@self/root/configs/ginny/configGetters/suiteManagerCommon');

const chromeOptions = {
    prefs: {
        profile: {
            default_content_setting_values: {
                geolocation: 2,
            },
        },
    },
    mobileEmulation: {
        deviceMetrics: {
            width: 480,
            height: 800,
            pixelRatio: 1,
            mobile: false,
            touch: false,
        },
        userAgent: [
            'Mozilla/5.0',
            '(Linux; Android 4.2.1; en-us; Nexus 5 Build/JOP40D)',
            'AppleWebKit/535.19',
            '(KHTML, like Gecko)',
            'Chrome/18.0.1025.166',
            'Mobile Safari/535.19',
        ].join(' '),
    },
};

require('@babel/register')({
    presets: babelrc.presets,
    ignore: [
        filepath =>
            filepath.includes('node_modules') &&
            !filepath.includes('ginny') &&
            !filepath.includes('hermione'),
    ],
});

module.exports = modifyPluginsPaths({
    baseUrl: 'https://default.exp-touch.tst.market.yandex.ru/',
    gridUrl: 'http://market@sw.yandex-team.ru:80/v0',
    waitTimeout: 10000,
    httpTimeout: 30000,
    sessionRequestTimeout: 120000,
    retry: 2,
    sets: {
        touch: {
            files: 'spec/hermione2/test-suites/tops/**/*.hermione.js',
        },
        'touch-screens': {
            files: 'spec/hermione2/test-suites/tops/**/*.hermione.screens.js',
        },
    },

    browsers: {
        chrome: {
            sessionsPerBrowser: 15,
            testsPerSession: 15,

            desiredCapabilities: {
                browserName: 'chrome',
                version: '74.0',
                acceptInsecureCerts: true,
                chromeOptions,
            },
            screenshotsDir,
            // можно кастомизировать путь для скриншотов каждого отдельного теста,
            // например, складывать в папку "screens" рядом с тестом
            // screenshotsDir: test => path.join(path.dirname(test.file), 'screens', test.id(), test.browserId),
            compositeImage: true,
            windowSize: '1280x1024',
        },
    },

    system: {
        debug: hermioneSystemDebug(),
        mochaOpts: {
            ui: 'exports',
            // fgrep: 'скриншот',
            timeout: 180000,
        },
        patternsOnReject: [
            /ESOCKETTIMEDOUT/,
            /ECONNREFUSED/,
        ],

        workers: 10,
    },

    plugins: {
        'html-reporter/hermione': {
            enabled: true,
            defaultView: 'all',
            baseHost: 'default.exp-touch.tst.market.yandex.ru',
        },

        '@yandex-market/hermione-allure-reporter': {
            targetDir: allure.targetDir,
            reportDir: allure.reportDir,
            testsManagementPattern: 'https://testpalm.yandex-team.ru/testcase/%s',
            issuesTrackerPattern: 'https://st.yandex-team.ru/%s',
        },

        '@yandex-market/hermione-page-object': {
            targetDir: 'spec/page-objects',
        },

        '@yandex-market/hermione-suite-manager': {
            ...suiteManagerCommon,
            suitesDir: 'spec/hermione2/test-suites/blocks',
        },

        '@yandex-market/hermione-chai': {},

        '@yandex-market/kadavr/plugin/hermione2': {
            host: kadavrHost,
            port: kadavrPort,
        },

        '@yandex-market/hermione-fair-stat-reporter/hermione': {
            reporters: {
                json: {
                    enabled: true,
                    path: path.resolve(__dirname, '../../../stat-reporter.json'),
                    statusIncludesBroken: true,
                },
            },
        },

        '@self/root/src/spec/hermione/plugins/auth/hermione2-auth': {},
        '@self/root/src/spec/hermione/plugins/muid/hermione2-muid': {},
        '@self/root/src/spec/hermione/plugins/cookie/hermione2-cookie': {},
    },

    prepareBrowser,
});
