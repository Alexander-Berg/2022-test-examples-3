const debuglog = require('util').debuglog('hermione-testpalm-reporter');

const TestpalmApi = require('./lib/testpalm-api');
const RunningSuites = require('./lib/running-suites');
const utils = require('./lib/utils');

module.exports = (hermione, config) => {
    let runningSuites;
    let testCollection;
    let api;

    /**
     * Перед инициализацией раннеров и чтением тестов - составляем Map:
     * key - полный путь до файла теста
     * value - Set<browser_name> список браузеров для выполнения файла
     * Используется при записи значений (комментарии и статусы кейсов) в TestPalm
     */
    hook(hermione.events.INIT, () => {
        debuglog('init Hermione plugin', config);

        if (!config.project) {
            throw new Error('hermione-testpalm-reporter: project option is required');
        }

        if (!config.token) {
            throw new Error('hermione-testpalm-reporter: token option is required');
        }

        if (!config.runId) {
            throw new Error('hermione-testpalm-reporter: runId option is required');
        }

        api = new TestpalmApi(config);
    });

    hook(hermione.events.AFTER_TESTS_READ, tc => {
        // Сохраняем ссылку на объект с тестами,
        // чтобы при старте построить актуальный список тестов,
        // которые точно будут запущены
        testCollection = tc;
    });

    hook(hermione.events.RUNNER_START, async() => {
        runningSuites = new RunningSuites(api, config.runId);
        await runningSuites.fetchTestCases();

        testCollection.eachTest((test, browser) => {
            if (test.disabled || test.pending) {
                return;
            }

            const testCaseId = utils.getTestCaseId(test, config.project);
            if (!testCaseId) {
                debuglog(`RUNNER_START: ignore suite without testCaseId: ${test.fullTitle()}`);
                return;
            }

            runningSuites.registerTest(testCaseId, test, browser);
        });
    });

    hook(hermione.events.RUNNER_END, async() => {
        await runningSuites.getPendingTasks();

        debuglog('all pending tasks are done');
    });

    hook(hermione.events.TEST_BEGIN, test => {
        const testCaseId = utils.getTestCaseId(test, config.project);

        if (!testCaseId) {
            return;
        }

        runningSuites.startTest(testCaseId, test);
    });

    hook(hermione.events.TEST_PASS, test => {
        const testCaseId = utils.getTestCaseId(test, config.project);

        if (!testCaseId) {
            return;
        }

        runningSuites.passTest(testCaseId, test);
    });

    hook(hermione.events.TEST_FAIL, test => {
        const testCaseId = utils.getTestCaseId(test, config.project);

        if (!testCaseId) {
            return;
        }

        runningSuites.failTest(testCaseId, test);
    });

    hook(hermione.events.RETRY, test => {
        const testCaseId = utils.getTestCaseId(test, config.project);

        if (!testCaseId) {
            return;
        }

        runningSuites.retryTest(testCaseId, test);
    });

    function hook(event, fn) {
        hermione.on(event, (...args) => {
            if (hermione.isWorker()) {
                return;
            }

            if (config.enabled === false) {
                return;
            }

            return fn(...args);
        });
    }
};
