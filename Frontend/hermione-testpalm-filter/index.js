const debuglog = require('util').debuglog('hermione-testpalm-filter');

const TestpalmApi = require('./lib/testpalm-api');
const utils = require('./lib/utils');

module.exports = (hermione, config) => {
    let testCases;

    hermione.on(hermione.events.INIT, () => {
        if (hermione.isWorker()) {
            return;
        }

        if (config.enabled === false) {
            return;
        }

        debuglog('init Hermione plugin', config);

        if (!config.project) {
            throw new Error('hermione-testpalm-filter: project option is required');
        }

        if (!config.token) {
            throw new Error('hermione-testpalm-filter: token option is required');
        }

        if (!config.runId) {
            throw new Error('hermione-testpalm-filter: runId option is required');
        }

        const api = new TestpalmApi(config);

        return api
            .getTestRun(config.runId)
            .then(testRun => {
                testCases = utils.getRunCases(testRun);

                debuglog(`found ${testCases.length} test cases`);
            })
            .catch(err => {
                testCases = [];

                console.error('Failed to get TestRun', err);
            });
    });

    hermione.on(hermione.events.AFTER_TESTS_READ, testCollection => {
        if (hermione.isWorker()) {
            return;
        }

        if (config.enabled === false) {
            return;
        }

        testCollection.eachTest(test => {
            const title = test.fullTitle();
            const testCaseId = utils.getTestCaseId(test, config.project);

            // Пропускаем тесты без проставленного ID из TestPalm
            if (!testCaseId) {
                debuglog(`disable test for reason[no TestCase ID]: ${title}`);

                testCollection.disableTest(title);
                return;
            }

            const item = testCases.find(item => String(item.testCase.id) === String(testCaseId));

            // Пропускаем тесты, которых нет в текущем Тест-Ране
            if (!item) {
                debuglog(`disable test[${testCaseId}] for reason[not found in TestRun]: ${title}`);

                testCollection.disableTest(title);
                return;
            }

            // Пропускаем уже запущенный тест кейс
            if (item.status !== 'CREATED') {
                debuglog(`disable test[${testCaseId}] for reason[case was ${item.status}]: ${title}`);

                testCollection.disableTest(title);
                return;
            }

            // Пропускаем удаленные кейсы
            if (item.testCase.removed) {
                debuglog(`disable test[${testCaseId}] for reason[case was removed]: ${title}`);

                testCollection.disableTest(title);
                return;
            }

            // В Ран попал неактуальный тест кейс
            if (item.testCase.status !== 'actual') {
                debuglog(`disable test[${testCaseId}] for reason[case status is ${item.testCase.status}]: ${title}`);

                testCollection.disableTest(title);
                return;
            }

            debuglog(`allow to run suite[${testCaseId}]: ${title}`);
        });
    });
};
