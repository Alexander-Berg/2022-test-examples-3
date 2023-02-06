'use strict';

const path = require('path');
const projectDir = path.resolve(__dirname, '../../..');

const repoPath = (...args) => path.resolve(projectDir, ...args);

require('@babel/register')({
    cwd: projectDir,
});

require('ignore-styles').default(['.css', '.styl']);

const {kadavrHost, kadavrPort} = require('./node');

module.exports = {
    baseUrl: 'https://testing.bringly.ru/',
    httpTimeout: 30000,
    sessionRequestTimeout: 60000,
    sessionQuitTimeout: 5000,

    hermione: {
        waitTimeout: 10000,
        retry: 3,
        sessionsPerBrowser: 10,
        testsPerSession: 10,
        sets: {
            desktop: {
                files: repoPath('src/spec/hermione/test-suites/pages.desktop/**/*.hermione.js'),
            },
        },
        browsers: {
            chrome: {
                desiredCapabilities: {
                    browserName: 'chrome',
                    version: '59.0',
                    unexpectedAlertBehaviour: 'accept',
                },
            },
        },
        system: {
            mochaOpts: {
                ui: 'exports',
                timeout: 180000,
            },
            debug: true,
        },
        plugins: {
            '@yandex-market/hermione-file-writer': {
                fileName: repoPath('src/spec/hermione/logs/uid_scenario.log'),
            },
        },
    },

    kadavr: {
        host: kadavrHost,
        port: kadavrPort,
    },

    gemini: {
        windowSize: '1920x1080',
        screenshotsDir: 'spec/gemini/screens', // на выходе должно быть platform.desktop/spec/gemini/screens
        sets: {
            chrome: {
                files: repoPath('src/spec/gemini/test-suites/*.gemini.js'),
                browsers: ['chrome'],
            },
        },
        browsers: {
            chrome: {
                screenshotsDir: 'spec/gemini/screens', // на выходе должно быть platform.desktop/spec/gemini/screens
                compositeImage: true,
                calibrate: false,
                desiredCapabilities: {
                    browserName: 'chrome',
                    version: '59.0',
                    unexpectedAlertBehaviour: 'accept',
                },
            },
        },
        system: {
            projectRoot: '.',
        },
    },

    allure: {
        targetDir: repoPath('src/spec/hermione/allure/results'),
        reportDir: process.env.REPORTS_DIR || repoPath('src/spec/hermione/allure/report'),
        testsManagementPattern: 'https://testpalm.yandex-team.ru/testcase/%s',
        issuesTrackerPattern: 'https://st.yandex-team.ru/%s',
    },

    pageObjects: {
        targetDir: repoPath('src/spec/page-objects'),
    },

    commands: {
        targetDir: repoPath('src/spec/hermione/commands'),
    },

    suiteManager: {
        suitesDir: repoPath('src/spec/hermione/test-suites/blocks.desktop'),
        caseFilter: {
            environment: 'testing|all',
        },
    },

    statReporter: {
        reporters: {
            json: {
                path: repoPath('stat-reporter.json'),
            },
        },
    },
};
