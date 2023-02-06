'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const testCases = proxyquire('../linters/test-cases.js', {
    fs: {
        readFileSync: () => 'content',
        '@global': true
    }
});

describe('githooks / testCases', () => {
    let runner;

    beforeEach(() => {
        runner = {
            log: sinon.stub(),
            npx: sinon.stub()
        };
    });

    it('should skip if none of yml/hermione files changed', () => {
        const collection = new Collection(['link.css', 'README.md']);

        assert.doesNotThrow(
            () => testCases(collection, runner)
        );

        assert.notCalled(runner.npx);
    });

    it('should fail on "yaml" files', () => {
        const collection = new Collection(['a-feature.yaml']);

        assert.throws(
            () => testCases(collection, runner)
        );
    });

    it('should fail on "yml" files', () => {
        const collection = new Collection(['a-feature.yml']);

        assert.throws(
            () => testCases(collection, runner)
        );
    });

    it('should run palmsync for testpalm.yml/hermione files', () => {
        const collection = new Collection(['link.css', 'foo.testpalm.yml', 'foo.hermione.js']);

        assert.doesNotThrow(
            () => testCases(collection, runner)
        );

        assert.calledWith(
            runner.npx,
            'palmsync validate "foo.testpalm.yml" "foo.hermione.js" --skip=tests'
        );
    });
});
