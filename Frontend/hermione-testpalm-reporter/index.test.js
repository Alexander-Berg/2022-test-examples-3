const path = require('path');

const nock = require('nock');
const Hermione = require('hermione');

const {
    testpalmApi,

    mockTestRun,
    mockTestStatus,
    mockTestComment,
    mockTestAttachment,

    mockSessionCreate,
    mockSessionDelete,
} = require('./mocks');

const pluginPath = path.join(__dirname, 'index.js');

const tests = {
    idInDescribe: path.join(__dirname, 'test/tests/id-in-describe.js'),
    idInTest: path.join(__dirname, 'test/tests/id-in-test.js'),
    idInContext: path.join(__dirname, 'test/tests/id-in-context.js'),
    multipleIds: path.join(__dirname, 'test/tests/multiple-ids.js'),
    noId: path.join(__dirname, 'test/tests/no-id.js'),
    duplicateNames: path.join(__dirname, 'test/tests/duplicate-names.js'),
    disabledTest: path.join(__dirname, 'test/tests/disabled-test.js'),
    skippedHermione: path.join(__dirname, 'test/tests/skipped-hermione.js'),
    skippedMocha: path.join(__dirname, 'test/tests/skipped-mocha.js'),
    failedOneBrowser: path.join(__dirname, 'test/tests/failed-one.js'),
    failedAllBrowsers: path.join(__dirname, 'test/tests/failed-all.js'),
    passedAfterRetry: path.join(__dirname, 'test/tests/passed-after-retry.js'),
    ffOnly: path.join(__dirname, 'test/tests-firefox/ff.js'),
};

jest.setTimeout(60 * 1000);

jest.mock('clear-require', () => {
    return () => {
        jest.resetModules();
    };
});

describe('HermioneTestpalmReporter', () => {
    let hermione;
    let testRun;
    let chromeSessionCreate;
    let chromeSessionDelete;
    let firefoxSessionCreate;
    let firefoxSessionDelete;

    beforeEach(() => {
        jest.resetModules();
        nock.disableNetConnect();

        const config = path.join(__dirname, 'test/.hermione.conf.js');
        hermione = new Hermione(config);

        Object.assign(hermione.config.plugins[pluginPath], {
            enabled: true,
            project: 'project-id',
            token: 'oauth-token',
            runId: 'run-id',
            apiHost: testpalmApi,
        });

        chromeSessionCreate = mockSessionCreate('chrome');
        chromeSessionDelete = mockSessionDelete('chrome');
        firefoxSessionCreate = mockSessionCreate('firefox');
        firefoxSessionDelete = mockSessionDelete('firefox');
    });

    afterEach(() => {
        nock.cleanAll();
        nock.enableNetConnect();
    });

    it('should report with id in test title', async() => {
        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-2', 'STARTED'),
            mockTestComment('uuid-2', [
                'Запускаю авто-тесты:',
                '- [chrome] test two project-id-2: awesome test',
                '- [firefox] test two project-id-2: awesome test',
            ].join('\n')),
            mockTestComment('uuid-2', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-2', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.idInTest,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should report with id in describe title', async() => {
        testRun = createTestRun([
            createTestCase({ id: 1 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-1', 'STARTED'),
            mockTestComment('uuid-1', [
                'Запускаю авто-тесты:',
                '- [chrome] project-id-1: test one awesome test',
                '- [firefox] project-id-1: test one awesome test',
            ].join('\n')),
            mockTestComment('uuid-1', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-1', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.idInDescribe,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should report with id in context', async() => {
        testRun = createTestRun([
            createTestCase({ id: 5 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-5', 'STARTED'),
            mockTestComment('uuid-5', [
                'Запускаю авто-тесты:',
                '- [chrome] test five awesome test',
                '- [firefox] test five awesome test',
            ].join('\n')),
            mockTestComment('uuid-5', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-5', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.idInContext,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should report with multiple ids in one test file', async() => {
        testRun = createTestRun([
            createTestCase({ id: 3 }),
            createTestCase({ id: 4 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-3', 'STARTED'),
            mockTestComment('uuid-3', [
                'Запускаю авто-тесты:',
                '- [chrome] group project-id-3: test three',
                '- [firefox] group project-id-3: test three',
            ].join('\n')),
            mockTestComment('uuid-3', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-3', 'PASSED'),

            mockTestStatus('uuid-4', 'STARTED'),
            mockTestComment('uuid-4', [
                'Запускаю авто-тесты:',
                '- [chrome] group project-id-4: test four awesome test',
                '- [firefox] group project-id-4: test four awesome test',
            ].join('\n')),
            mockTestComment('uuid-4', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-4', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.multipleIds,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should works with single browser', async() => {
        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const requests = [
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-2', 'STARTED'),
            mockTestComment('uuid-2', [
                'Запускаю авто-тесты:',
                '- [firefox] project-id-2: firefox test awesome test',
            ].join('\n')),
            mockTestComment('uuid-2', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-2', 'PASSED'),

            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.ffOnly,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should works if no tests mapped to test cases', async() => {
        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.noId,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should works with duplicate test case in different files', async() => {
        testRun = createTestRun([
            createTestCase({ id: 5 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-5', 'STARTED'),
            mockTestComment('uuid-5', [
                'Запускаю авто-тесты:',
                '- [chrome]  project-id-5: test one',
                '- [chrome]  project-id-5: test two',
                '- [firefox]  project-id-5: test one',
                '- [firefox]  project-id-5: test two',
            ].join('\n')),
            mockTestComment('uuid-5', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-5', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.duplicateNames,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should works with duplicate test names', async() => {
        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-2', 'STARTED'),
            mockTestComment('uuid-2', [
                'Запускаю авто-тесты:',
                '- [chrome] test two project-id-2: awesome test',
                '- [firefox] test two project-id-2: awesome test',
                '- [firefox] project-id-2: firefox test awesome test',
            ].join('\n')),
            mockTestComment('uuid-2', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-2', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.idInTest,
            tests.ffOnly,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should ignore disabled tests', async() => {
        testRun = createTestRun([
            createTestCase({ id: 1 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-1', 'STARTED'),
            mockTestComment('uuid-1', [
                'Запускаю авто-тесты:',
                '- [chrome]  project-id-1: test one',
                '- [firefox]  project-id-1: test one',
            ].join('\n')),
            mockTestComment('uuid-1', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-1', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.disabledTest,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should ignore skipped tests by hermione', async() => {
        testRun = createTestRun([
            createTestCase({ id: 1 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-1', 'STARTED'),
            mockTestComment('uuid-1', [
                'Запускаю авто-тесты:',
                '- [chrome]  project-id-1: test six',
                '- [firefox]  project-id-1: test six',
            ].join('\n')),
            mockTestComment('uuid-1', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-1', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.skippedHermione,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should ignore skipped tests by mocha', async() => {
        testRun = createTestRun([
            createTestCase({ id: 1 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-1', 'STARTED'),
            mockTestComment('uuid-1', [
                'Запускаю авто-тесты:',
                '- [chrome]  project-id-1: test four',
                '- [firefox]  project-id-1: test four',
            ].join('\n')),
            mockTestComment('uuid-1', 'Авто-тесты завершены. Статус: PASSED.'),
            mockTestStatus('uuid-1', 'PASSED'),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.skippedMocha,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should ignore test missing in test run', async() => {
        testRun = createTestRun([
            createTestCase({ id: 1 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.idInTest,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should works with fail test in one browser', async() => {
        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-2', 'STARTED'),
            mockTestComment('uuid-2', [
                'Запускаю авто-тесты:',
                '- [chrome] project-id-2: failed firefox test awesome test',
                '- [firefox] project-id-2: failed firefox test awesome test',
            ].join('\n')),

            mockTestAttachment(2),
            mockTestComment('uuid-2', [
                'Тест _project-id-2: failed firefox test awesome test_ будет перезапущен в браузере _chrome_',
                '**Сессия в Selenium**: session-id-chrome-1',
                '**Скриншот**: ![image.png](/static/screenshot-url)',
                '**Ошибка**:',
                '',
                '    error stacktrace',
            ].join('\n')),

            chromeSessionDelete,
            firefoxSessionDelete,
            mockSessionCreate('chrome', 2),

            mockTestAttachment(2),
            mockTestComment('uuid-2', [
                'Тест _project-id-2: failed firefox test awesome test_ не пройден в браузере _chrome_',
                '**Сессия в Selenium**: session-id-chrome-2',
                '**Скриншот**: ![image.png](/static/screenshot-url)',
                '**Ошибка**:',
                '',
                '    error stacktrace',
            ].join('\n')),

            mockTestComment('uuid-2', 'Авто-тесты завершены. Статус: FAILED.'),
            mockTestStatus('uuid-2', 'FAILED'),

            mockSessionDelete('chrome', 2),
        ];

        const result = await hermione.run([
            tests.failedOneBrowser,
        ]);

        expect(result).toBe(false);

        requests.forEach(mock => mock.done());
    });

    it('should works with fail test in two browsers', async() => {
        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-2', 'STARTED'),
            mockTestComment('uuid-2', [
                'Запускаю авто-тесты:',
                '- [chrome] project-id-2: failed test awesome test',
                '- [firefox] project-id-2: failed test awesome test',
            ].join('\n')),

            mockTestAttachment(2),
            mockTestComment('uuid-2', [
                'Тест _project-id-2: failed test awesome test_ будет перезапущен в браузере _chrome_',
                '**Сессия в Selenium**: session-id-chrome-1',
                '**Скриншот**: ![image.png](/static/screenshot-url)',
                '**Ошибка**:',
                '',
                '    error stacktrace',
            ].join('\n')),

            mockTestAttachment(2),
            mockTestComment('uuid-2', [
                'Тест _project-id-2: failed test awesome test_ будет перезапущен в браузере _firefox_',
                '**Сессия в Selenium**: session-id-firefox-1',
                '**Скриншот**: ![image.png](/static/screenshot-url)',
                '**Ошибка**:',
                '',
                '    error stacktrace',
            ].join('\n')),

            chromeSessionDelete,
            firefoxSessionDelete,
            mockSessionCreate('chrome', 2),
            mockSessionCreate('firefox', 2),

            mockTestAttachment(2),
            mockTestComment('uuid-2', [
                'Тест _project-id-2: failed test awesome test_ не пройден в браузере _chrome_',
                '**Сессия в Selenium**: session-id-chrome-2',
                '**Скриншот**: ![image.png](/static/screenshot-url)',
                '**Ошибка**:',
                '',
                '    error stacktrace',
            ].join('\n')),

            mockTestAttachment(2),
            mockTestComment('uuid-2', [
                'Тест _project-id-2: failed test awesome test_ не пройден в браузере _firefox_',
                '**Сессия в Selenium**: session-id-firefox-2',
                '**Скриншот**: ![image.png](/static/screenshot-url)',
                '**Ошибка**:',
                '',
                '    error stacktrace',
            ].join('\n')),

            mockTestComment('uuid-2', 'Авто-тесты завершены. Статус: FAILED.'),
            mockTestStatus('uuid-2', 'FAILED'),

            mockSessionDelete('chrome', 2),
            mockSessionDelete('firefox', 2),
        ];

        const result = await hermione.run([
            tests.failedAllBrowsers,
        ]);

        expect(result).toBe(false);

        requests.forEach(mock => mock.done());
    });

    it('should pass test after retry', async() => {
        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,

            mockTestRun(testRun),

            mockTestStatus('uuid-2', 'STARTED'),
            mockTestComment('uuid-2', [
                'Запускаю авто-тесты:',
                '- [chrome] project-id-2: passed after retry awesome test',
                '- [firefox] project-id-2: passed after retry awesome test',
            ].join('\n')),

            mockTestAttachment(2),
            mockTestComment('uuid-2', [
                'Тест _project-id-2: passed after retry awesome test_ будет перезапущен в браузере _chrome_',
                '**Сессия в Selenium**: session-id-chrome-1',
                '**Скриншот**: ![image.png](/static/screenshot-url)',
                '**Ошибка**:',
                '',
                '    error stacktrace',
            ].join('\n')),

            mockTestAttachment(2),
            mockTestComment('uuid-2', [
                'Тест _project-id-2: passed after retry awesome test_ будет перезапущен в браузере _firefox_',
                '**Сессия в Selenium**: session-id-firefox-1',
                '**Скриншот**: ![image.png](/static/screenshot-url)',
                '**Ошибка**:',
                '',
                '    error stacktrace',
            ].join('\n')),

            chromeSessionDelete,
            firefoxSessionDelete,
            mockSessionCreate('chrome', 2),
            mockSessionCreate('firefox', 2),

            mockTestStatus('uuid-2', 'PASSED'),
            mockTestComment('uuid-2', 'Авто-тесты завершены. Статус: PASSED.'),

            mockSessionDelete('chrome', 2),
            mockSessionDelete('firefox', 2),
        ];

        const result = await hermione.run([
            tests.passedAfterRetry,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should not start plugin if enabled=false', async() => {
        hermione.config.plugins[pluginPath].enabled = false;

        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const requests = [
            chromeSessionCreate,
            firefoxSessionCreate,
            chromeSessionDelete,
            firefoxSessionDelete,
        ];

        const result = await hermione.run([
            tests.idInTest,
        ]);

        expect(result).toBe(true);

        requests.forEach(mock => mock.done());
    });

    it('should throw error if no project in config', async() => {
        hermione.config.plugins[pluginPath].project = undefined;

        await expect(hermione.run()).rejects.toThrowError('hermione-testpalm-reporter: project option is required');
    });

    it('should throw error if no token in config', async() => {
        hermione.config.plugins[pluginPath].token = undefined;

        await expect(hermione.run()).rejects.toThrowError('hermione-testpalm-reporter: token option is required');
    });

    it('should throw error if no runId in config', async() => {
        hermione.config.plugins[pluginPath].runId = undefined;

        await expect(hermione.run()).rejects.toThrowError('hermione-testpalm-reporter: runId option is required');
    });
});

function createTestRun(testCases) {
    return {
        testGroups: [
            {
                testCases: [],
            },
            {
                testCases,
            },
            {
                testCases: [],
            },
        ],
    };
}

function createTestCase({ id, ...rest }) {
    return {
        uuid: `uuid-${id}`,
        status: 'CREATED',
        ...rest,
        testCase: {
            id,
            name: `Test case #${id}`,
            status: 'actual',
            isAutotest: true,
            removed: false,
            ...(rest && rest.testCase),
        },
    };
}
