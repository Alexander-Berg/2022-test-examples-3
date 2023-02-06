const babelrc = require('../../../../babelrc.node');
const path = require('path');

require('@babel/register')({
    presets: babelrc.presets,
    ignore: [filepath =>
        filepath.includes('node_modules') &&
        !filepath.includes('app/node_modules') &&
        !filepath.includes('beton/src') &&
        !filepath.includes('seo/esm')],
    plugins: babelrc.plugins,
});
require('ignore-styles').default(['.css', '.styl']);

const {smokeTestConfig} = require('@self/platform/spec/hermione/configs/smoke');

const {jsonReporterPath} = require('@self/root/configs/ginny/configGetters/jsonReporterPath');
const {hermioneSystemDebug} = require('@self/root/configs/ginny/configGetters/hermioneSystemDebug');

const {suiteManagerCommon} = require('@self/root/configs/ginny/configGetters/suiteManagerCommon');
const patchProjectPlatformRequire = require('@self/root/configs/ginny/patchProjectPlatformRequire');
patchProjectPlatformRequire('market', 'desktop');
global.__non_webpack_require__ = require;

// Для того, чтобы в коде проекта отличать когда он используется в автотестах
// Например, нужно для отключения TVM
process.env.IS_AUTOTEST_RUN = 'true';

module.exports = {
    baseUrl: 'https://default.exp.tst.market.yandex.ru/',
    gridUrl: 'http://market@sw.yandex-team.ru:80/v0',
    httpTimeout: 30000,
    sessionRequestTimeout: 60000,
    sessionQuitTimeout: 5000,

    hermione: {
        waitTimeout: 10000,
        retry: 1,
        sessionsPerBrowser: 10,
        testsPerSession: 10,
        sets: {
            desktop: {
                files: 'spec/hermione/test-suites/tops/**/*.hermione.js',
            },
        },
        browsers: {
            chrome: {
                desiredCapabilities: {
                    browserName: 'chrome',
                    version: '74.0',
                },
            },
        },
        system: {
            mochaOpts: {
                ui: 'exports',
                timeout: 180000,
            },
            patternsOnReject: [
                /ESOCKETTIMEDOUT/,
                /ECONNREFUSED/,
            ],
            debug: hermioneSystemDebug(),
        },
        plugins: {
            '@yandex-market/hermione-json-reporter/hermione': {
                path: jsonReporterPath(),
            },
            '@self/root/src/spec/hermione/plugins/yandexHelp/hermione-yandexHelp': {},
            ...(Boolean(process.env.SMOKE_TEST) && {
                '@yandex-market/hermione-url-rewriter-plugin': {
                    config: smokeTestConfig,
                    filter: true,
                },
            }),
            '@self/root/src/spec/hermione/plugins/auth/hermione-auth': {},
            '@self/root/src/spec/hermione/plugins/muid/hermione-muid': {},
            '@self/root/src/spec/hermione/plugins/cookie/hermione-cookie': {},
        },
    },

    /**
     * Значения из конфигов проекта могут быть переопределены в genisys
     * https://genisys.yandex-team.ru/rules/sandbox-ci-market/sandbox_clients
     */
    gemini: {
        windowSize: '1280x780',
        screenshotsDir: 'spec/gemini/screens',
        grep: /^.*KADAVR.*$/, // Вырезаем тесты без кадавра
        sets: {
            chrome: {
                files: 'spec/gemini/test-suites/pages/**/*.gemini.js',
                browsers: ['chrome'],
            },
        },
        browsers: {
            chrome: {
                screenshotsDir: 'spec/gemini/screens',
                compositeImage: true,
                calibrate: false,
                suitesPerSession: 1,
                retry: 2,
                desiredCapabilities: {
                    browserName: 'chrome',
                    version: '74.0',
                },
            },
        },
        system: {
            projectRoot: '.',
        },
    },

    allure: {
        targetDir: 'spec/hermione/allure/results',
        reportDir: process.env.REPORTS_DIR || 'spec/hermione/allure/report',
        testsManagementPattern: 'https://testpalm.yandex-team.ru/testcase/%s',
        issuesTrackerPattern: 'https://st.yandex-team.ru/%s',
        tidTableInfo: {
            htmlTidTableDir: '../tid_table_info',
            htmlTidTableJsonFile: 'tidTable.json',
            hostName: 'https://a.yandex-team.ru/arc/trunk/arcadia/market/front/apps/marketfront',
        },
        debugInfo: {
            wdCommands: true,
            kadavrLog: true,
        },
    },

    pageObjects: {
        targetDir: 'spec/page-objects',
    },

    commands: {
        targetDir: 'spec/hermione/commands',
    },

    suiteManager: {
        ...suiteManagerCommon,
        caseFilter: {
            environment: 'testing|kadavr',
        },
    },

    statReporter: {
        reporters: {
            json: {
                enabled: true,
                path: path.resolve(__dirname, '../../../stat-reporter.json'),
                statusIncludesBroken: true,
            },
        },
    },

    brokenTests: {
        enabled: true,
        mode: 'collect',
        reportDir: path.resolve(__dirname, '../../../broken_tests_report'),
    },
    // https://st.yandex-team.ru/MARKETFRONTECH-1530
    // Грязный хак чтобы указать gemini-плагину, что не нужно выставлять дефолтные хост и порт (:::8089), а вместо этого
    // взять их из KADAVR_HOSTS
    kadavr: {
        host: null,
        port: null,
    },
};
