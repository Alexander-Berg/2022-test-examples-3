const debuglog = require('util').debuglog('hermione-testpalm-reporter');
const utils = require('./utils');
const Queue = require('./queue');

class RunningSuites {
    /**
     * @param {TestpalmApi} api
     * @param {String} runId
     * @constructor
     */
    constructor(api, runId) {
        this._api = api;
        this._runId = runId;
        this._runTestCases = [];
        this._testCases = [];
        this._queue = new Queue();
    }

    /**
     * @returns {Promise}
     */
    async fetchTestCases() {
        const testRun = await this._api.getTestRun(this._runId);

        this._runTestCases = utils.getRunCases(testRun);

        debuglog(`found ${this._runTestCases.length} test cases`);
    }

    /**
     * @param {Number} testCaseId
     * @param {Test} test
     * @param {String} browser
     * @returns {undefined}
     */
    registerTest(testCaseId, test, browser) {
        const testCase = this._runTestCases.find(item => String(item.testCase.id) === String(testCaseId));

        if (!testCase) {
            debuglog(`ignore test[${test.fullTitle()}] in suite[${testCaseId}] – not found in Test Run`);
            return;
        }

        debuglog(`register test[${test.fullTitle()}] in suite[${testCaseId}] for browser[${browser}]`);

        let item = this.getTestCase(testCaseId);

        if (!item) {
            item = {
                id: testCaseId,
                uuid: testCase.uuid,
                tests: [],
                testCount: 0,
                testDone: 0,
                started: false,
                stat: {
                    passed: 0,
                    failed: 0,
                },
            };

            this._testCases.push(item);
        }

        item.testCount++;
        item.tests.push(test);
    }

    /**
     * @param {Number} testCaseId
     * @returns {Object|undefined}
     */
    getTestCase(testCaseId) {
        return this._testCases.find(testCase => testCase.id === testCaseId);
    }

    /**
     * @param {Number} testCaseId
     * @returns {undefined}
     */
    startTestCase(testCaseId) {
        debuglog(`start suite[${testCaseId}]`);

        const testCase = this.getTestCase(testCaseId);

        testCase.started = true;

        this._addComment(testCaseId, null, [
            'Запускаю авто-тесты:',
            ...testCase.tests.map(test => {
                return `- [${test.browserId}] ${test.fullTitle()}`;
            }),
        ].join('\n'));

        this._setStatus(testCaseId, 'STARTED');
    }

    /**
     * @param {Number} testCaseId
     * @returns {undefined}
     */
    finishTestCase(testCaseId) {
        debuglog(`finish suite[${testCaseId}]`);

        const testCase = this.getTestCase(testCaseId);
        let status;

        // Проверяем что был запущен хотя бы один тест,
        // на случай когда все тесты в статусе ignored
        if (testCase.stat.passed > 0) {
            status = 'PASSED';
        }

        // Если упал хоть один тест, то весь тесткейс не пройден
        if (testCase.stat.failed > 0) {
            status = 'FAILED';
        }

        if (status) {
            this._addComment(testCaseId, null, `Авто-тесты завершены. Статус: ${status}.`);
            this._setStatus(testCaseId, status);
        }
    }

    /**
     * @param {Number} testCaseId
     * @param {Object} test
     * @returns {undefined}
     */
    startTest(testCaseId, test) {
        const testCase = this.getTestCase(testCaseId);

        if (!testCase) {
            return;
        }

        debuglog(`start test[${test.fullTitle()}] in suite[${testCaseId}] in browser[${test.browserId}]`);

        if (!testCase.started) {
            this.startTestCase(testCaseId);
        }
    }

    /**
     * @param {Number} testCaseId
     * @param {Object} test
     * @returns {undefined}
     */
    passTest(testCaseId, test) {
        const testCase = this.getTestCase(testCaseId);

        if (!testCase) {
            return;
        }

        debuglog(`passed test[${test.title}] in suite[${testCaseId}] in browser[${test.browserId}]`);

        testCase.testDone++;
        testCase.stat.passed++;

        if (testCase.testDone === testCase.testCount) {
            this.finishTestCase(testCaseId);
        }
    }

    /**
     * @param {Number} testCaseId
     * @param {Object} test
     * @returns {undefined}
     */
    failTest(testCaseId, test) {
        const testCase = this.getTestCase(testCaseId);

        if (!testCase) {
            return;
        }

        debuglog(`failed test[${test.title}] in suite[${testCaseId}] in browser[${test.browserId}]`);

        testCase.testDone++;
        testCase.stat.failed++;

        this._addComment(testCaseId, test, `Тест _${test.fullTitle()}_ не пройден в браузере _${test.browserId}_`);

        if (testCase.testDone === testCase.testCount) {
            this.finishTestCase(testCaseId);
        }
    }

    /**
     * @param {Number} testCaseId
     * @param {Object} test
     * @returns {undefined}
     */
    retryTest(testCaseId, test) {
        const testCase = this.getTestCase(testCaseId);

        if (!testCase) {
            return;
        }

        debuglog(`retry test[${test.title}] in suite[${testCaseId}] in browser[${test.browserId}]`);

        this._addComment(testCaseId, test, `Тест _${test.fullTitle()}_ будет перезапущен в браузере _${test.browserId}_`);
    }

    /**
     * @returns {Promise}
     */
    getPendingTasks() {
        return this._queue.promise();
    }

    /**
     * @param {Number} testCaseId
     * @param {Object|null} test
     * @param {String} text
     * @returns {undefined}
     * @private
     */
    _addComment(testCaseId, test, text) {
        const testCase = this.getTestCase(testCaseId);
        let err = test && test.err;

        this._queue.add(() => Promise.resolve()
            .then(() => {
                if (err && err.screenshot) {
                    return this._api
                        .uploadScreenshot(testCaseId, test.err.screenshot.base64)
                        .catch(err => {
                            console.error('Failed to upload screenshot', err);
                        });
                }
            })
            .then(screenshot => {
                if (err) {
                    const stack = err.testStack || err.stack;
                    if (stack) {
                        err = stack.toString();
                    } else {
                        err = err.toString();
                    }
                }

                const comment = [
                    text,
                    test && test.sessionId && `**Сессия в Selenium**: ${test.sessionId}`,
                    test && test.meta && test.meta.url && `**URL**: ${test.meta.url}`,
                    screenshot && `**Скриншот**: ![image.png](${screenshot.url})`,
                    err && `**Ошибка**:\n\n${utils.ident(err)}`,
                ].filter(Boolean).join('\n');

                return this._api.addTestCaseComment(this._runId, testCase.uuid, comment);
            })
            .then(() => {
                debuglog(`comment added to suite[${testCaseId}]: ${text}`);
            })
            .catch(err => {
                console.error(`Failed to add comment for test case #${testCaseId}`, err);
            }),
        );
    }

    /**
     * @param {Number} testCaseId
     * @param {String} status
     * @returns {undefined}
     * @private
     */
    _setStatus(testCaseId, status) {
        const testCase = this.getTestCase(testCaseId);

        this._queue.add(() => this._api
            .setTestCaseStatus(this._runId, testCase.uuid, status)
            .then(() => {
                debuglog(`suite[${testCaseId}] marked as ${status}`);
            })
            .catch(err => {
                console.error(`Failed to status for test case #${testCaseId}`, err);
            }),
        );
    }
}

module.exports = RunningSuites;
