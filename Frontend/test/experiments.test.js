'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const experiments = require('../linters/experiments.js');

describe('githooks / experiments', () => {
    let runner;

    beforeEach(() => {
        runner = { log: sinon.stub() };
    });

    it('should not fail if allowed files added to exp-level', () => {
        const collection = new Collection(['experiments/foo/link.js']);

        assert.doesNotThrow(
            () => experiments(collection, runner)
        );
    });

    it('should fail if bemhtml.js files added to exp-level', () => {
        const collection = new Collection(['experiments/foo/link.bemhtml.js']);

        assert.throws(
            () => experiments(collection, runner)
        );
    });

    it('should fail if test.js files added to exp-level', () => {
        const collection = new Collection(['experiments/foo/link.test.js']);

        assert.throws(
            () => experiments(collection, runner)
        );
    });
});
