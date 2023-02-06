const path = require('path');

const nock = require('nock');
const Hermione = require('hermione');

const testpalmApi = 'http://testpalm-api';
const seleniumApi = 'http://sg.yandex-team.ru:4444';
const sessionId = 'ca966830ef3e2311e39e770fed48a95334ac645d-ad20-4d4d-a506-6b0283da6521';
const pluginPath = path.join(__dirname, 'index.js');

describe('HermioneTestpalmFilter', () => {
    let hermione;
    let testRun;
    let sessionCreate;
    let sessionDelete;

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

        sessionCreate = mockSessionCreate();
        sessionDelete = mockSessionDelete();
    });

    afterEach(() => {
        nock.cleanAll();
        nock.enableNetConnect();
    });

    it('should run all available tests with test case id', async() => {
        testRun = createTestRun([
            createTestCase({ id: 1 }),
            createTestCase({ id: 2 }),
            createTestCase({ id: 3 }),
            createTestCase({ id: 4 }),
            createTestCase({ id: 5 }),
        ]);

        const getTestRun = mockTestRun(testRun);

        const result = await hermione.run();

        expect(result).toBe(true);
        expect(hermione.startedTests.sort()).toEqual([
            'group project-id-3: test three',
            'group project-id-4: test four awesome test',
            'project-id-1: test one awesome test',
            'test five awesome test',
            'test two project-id-2: awesome test',
        ]);

        getTestRun.done();
        sessionCreate.done();
        sessionDelete.done();
    });

    it('should disable tests not available in test run', async() => {
        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const getTestRun = mockTestRun(testRun);

        const result = await hermione.run();

        expect(result).toBe(true);
        expect(hermione.startedTests).toEqual([
            'test two project-id-2: awesome test',
        ]);

        getTestRun.done();
        sessionCreate.done();
        sessionDelete.done();
    });

    it('should disable test if status is not CREATED', async() => {
        testRun = createTestRun([
            createTestCase({ id: 1, status: 'STARTED' }),
            createTestCase({ id: 2 }),
        ]);

        const getTestRun = mockTestRun(testRun);

        const result = await hermione.run();

        expect(result).toBe(true);
        expect(hermione.startedTests).toEqual([
            'test two project-id-2: awesome test',
        ]);

        getTestRun.done();
        sessionCreate.done();
        sessionDelete.done();
    });

    it('should disable test if test case was removed', async() => {
        testRun = createTestRun([
            createTestCase({
                id: 1,
                testCase: {
                    removed: true,
                },
            }),
            createTestCase({ id: 2 }),
        ]);

        const getTestRun = mockTestRun(testRun);

        const result = await hermione.run();

        expect(result).toBe(true);
        expect(hermione.startedTests).toEqual([
            'test two project-id-2: awesome test',
        ]);

        getTestRun.done();
        sessionCreate.done();
        sessionDelete.done();
    });

    it('should disable test if test case is not actual', async() => {
        testRun = createTestRun([
            createTestCase({
                id: 1,
                testCase: {
                    status: 'need changes',
                },
            }),
            createTestCase({ id: 2 }),
        ]);

        const getTestRun = mockTestRun(testRun);

        const result = await hermione.run();

        expect(result).toBe(true);
        expect(hermione.startedTests).toEqual([
            'test two project-id-2: awesome test',
        ]);

        getTestRun.done();
        sessionCreate.done();
        sessionDelete.done();
    });

    it('should not start plugin if enabled=false', async() => {
        hermione.config.plugins[pluginPath].enabled = false;

        testRun = createTestRun([
            createTestCase({ id: 2 }),
        ]);

        const getTestRun = mockTestRun(testRun);

        const result = await hermione.run();

        expect(result).toBe(true);
        expect(hermione.startedTests.sort()).toEqual([
            'group project-id-3: test three',
            'group project-id-4: test four awesome test',
            'project-id-1: test one awesome test',
            'test five awesome test',
            'test two project-id-2: awesome test',
            'unnamed test awesome test',
        ]);

        expect(getTestRun.isDone()).toBe(false);
        sessionCreate.done();
        sessionDelete.done();
    });

    it('should throw error if no project in config', async() => {
        hermione.config.plugins[pluginPath].project = undefined;

        await expect(hermione.run()).rejects.toThrowError('hermione-testpalm-filter: project option is required');
    });

    it('should throw error if no token in config', async() => {
        hermione.config.plugins[pluginPath].token = undefined;

        await expect(hermione.run()).rejects.toThrowError('hermione-testpalm-filter: token option is required');
    });

    it('should throw error if no runId in config', async() => {
        hermione.config.plugins[pluginPath].runId = undefined;

        await expect(hermione.run()).rejects.toThrowError('hermione-testpalm-filter: runId option is required');
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
        uuid: 'uuid-1',
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

function mockTestRun(testRun) {
    return nock(testpalmApi)
        .matchHeader('Authorization', 'OAuth oauth-token')
        .get('/api/testrun/project-id/run-id')
        .reply(200, testRun);
}

function mockSessionCreate() {
    return nock(seleniumApi, { encodedQueryParams: true })
        .post('/wd/hub/session')
        .reply(200, {
            value: {
                sessionId,
                capabilities: {
                    browserName: 'chrome',
                    browserVersion: '123.0.0',
                    platformName: 'linux',
                },
            },
        });
}

function mockSessionDelete() {
    return nock(seleniumApi, { encodedQueryParams: true })
        .delete(`/wd/hub/session/${sessionId}`)
        .reply(200, {
            value: {},
        });
}
