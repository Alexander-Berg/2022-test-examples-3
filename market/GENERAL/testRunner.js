'use strict';

const _ = require('lodash');

const AllureAdapter = require('./AllureAdapter');
const RuntimeAdapter = require('./RuntimeAdapter');
const AllureController = require('./AllureController');

module.exports = {
    /**
     *
     * @param {Hermione} hermione
     * @param {AllureReporterOptions} options
     * @param tidTableParsed
     * @param hostName
     */
    init(hermione, options, tidTableParsed, hostName) {
        const {events} = hermione;

        if (!hermione.isWorker()) {
            const allureAdapter = new AllureAdapter(options);
            const allureController = new AllureController(allureAdapter, options);

            hermione
                .on(events.SUITE_BEGIN, this._makeListener(allureController, 'suiteBegin'))
                .on(events.SUITE_END, this._makeListener(allureController, 'suiteEnd'))

                .on(events.TEST_BEGIN, this._makeListener(allureController, 'testBegin'))
                .on(events.TEST_PENDING, this._makeListener(allureController, 'testPending'))
                .on(events.TEST_PASS, this._makeListener(allureController, 'testPass'))
                .on(events.TEST_FAIL, this._makeListener(allureController, 'testFail'))

                .on(events.RETRY, this._makeListener(allureController, 'testRetry'))

                .on(events.RUNNER_END, AllureController.runnerEnd.bind(null, options, hermione.config));
        } else {
            hermione
                .on(events.NEW_BROWSER, (browser) => {
                    const allureAdapter = new AllureAdapter(options, tidTableParsed, hostName);

                    Object.defineProperty(browser.getPrototype(), 'allure', {
                        configurable: false,
                        enumerable: false,
                        writable: false,
                        value: new RuntimeAdapter(allureAdapter, browser, options),
                    });
                });
        }
    },

    /**
     *
     * @param {AllureController} allure
     * @param {String} method
     * @returns {function(*=)}
     */
    _makeListener(allure, method) {
        return (entity) => {
            return allure[method](entity);
        };
    }
};
