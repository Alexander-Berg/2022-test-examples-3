'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');

const GlobalContextPropsStub = ['foo', 'bar'];

const gitStub = {};
const chalkStub = {
    red: () => '# error',
    yellow: () => '# warning',
    green: () => '# text'
};
const globalContext = proxyquire('../linters/global-context.js', {
    '../lib/vcs': gitStub,
    '../helpers/global-context-props': () => GlobalContextPropsStub,
    chalk: chalkStub
});

describe('githooks / globalContext', () => {
    let runner;

    beforeEach(() => {
        gitStub.diffIn = sinon.stub();

        runner = { log: sinon.stub() };
    });

    describe('i-global__variables', () => {
        it('should warn if new prop added', () => {
            const collection = new Collection(['i-global__variables.priv.js']);

            gitStub.diffIn.returns('+ data.baz');

            globalContext(collection, runner);

            assert.calledOnce(runner.log);
            assert.calledWith(runner.log, chalkStub.yellow());
        });

        it('should not warn if nothing added', () => {
            const collection = new Collection(['i-global__variables.priv.js']);

            gitStub.diffIn.returns('- data.baz');

            globalContext(collection, runner);

            assert.notCalled(runner.log);
        });
    });

    describe('priv', () => {
        it('should do nothing if no priv changed', () => {
            const collection = new Collection(['foo.css']);

            globalContext(collection, runner);

            assert.notCalled(runner.log);
        });

        it('should not throw if unknown prop was read', () => {
            const collection = new Collection(['foo.priv.js']);

            gitStub.diffIn.returns('+ data.baz');

            assert.doesNotThrow(
                () => globalContext(collection, runner)
            );
        });

        it('should throw if known prop was read from data', () => {
            const collection = new Collection(['foo.priv.js']);

            gitStub.diffIn.returns('+ data.foo');

            assert.throws(
                () => globalContext(collection, runner)
            );
        });

        it('should throw if known prop was read from reportData', () => {
            const collection = new Collection(['foo.priv.js']);

            gitStub.diffIn.returns('+ context.reportData.foo');

            assert.throws(
                () => globalContext(collection, runner)
            );
        });
    });
});
