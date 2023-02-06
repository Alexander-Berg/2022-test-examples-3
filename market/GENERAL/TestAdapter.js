'use strict';

const _ = require('lodash');
const Test = require('allure-js-commons/beans/test');
const utils = require('./utils/mocha');

const TEST_PALM_STATUS = {
    passed: 'PASSED',
    failed: 'FAILED',
    pending: 'SKIPPED',
    broken: 'BROKEN'
};

/**
 *
 */
class TestAdapter extends Test {
    /**
     *
     * @returns {string}
     */
    getName() {
        return this.name;
    }

    /**
     *
     * @param {string} name - новое полное название теста
     * @returns {TestAdapter}
     */
    setName(name) {
        this.name = name;
        return this;
    }

    /**
     *
     * @param {string} name
     * @param {string} value
     * @returns {TestAdapter}
     */
    addLabel(name, value) {
        if (value) {
            super.addLabel(name, value);
        }

        return this;
    }

    /**
     *
     * @param {string} kind
     * @param {string} name
     * @param {string} value
     * @returns {TestAdapter}
     */
    addParameter(kind, name, value) {
        super.addParameter(kind, name, value);

        return this;
    }

    /**
     *
     * @param {string} name
     * @returns {string}
     */
    getLabel(name) {
        return _.get(_.find(this.labels, {name}), 'value');
    }

    /**
     *
     * @param {string} description
     * @param {string} [type]
     * @returns {TestAdapter}
     */
    setDescription(description, type) {
        if (description) {
            super.setDescription(description, type);
        }

        return this;
    }

    /**
     *
     * @returns {Object}
     */
    toTestPalm() {
        const testId = this.getLabel('testId');

        if (!testId) {
            return;
        }

        const cuttedPath = [
            this.getLabel('feature'),
            this.getLabel('story')
        ];
        const status = TEST_PALM_STATUS[this.status];
        const parameters = _.map(this.parameters, param => _.pick(param, ['name', 'value']));

        return {
            testCase: {
                id: testId.replace(/^.+-(\d+)$/, '$1')
            },
            status,
            path: cuttedPath,
            startedTime: this.start,
            finishedTime: this.stop,
            parametersList: parameters.length ? [{status, parameters}] : []
        }
    }
}

module.exports = TestAdapter;
