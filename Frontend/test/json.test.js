'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');

const fsStub = {};
const rawjson = proxyquire('../linters/json.js', { fs: fsStub });
const json = (collection, runner) => rawjson(collection, { ...runner, skipTypingsCheck: true });

describe('githooks / json', () => {
    let runner;

    beforeEach(() => {
        fsStub.readFileSync = sinon.stub();

        runner = {
            tool: sinon.stub(),
            log: sinon.stub()
        };
    });

    it('should skip if none of json files changed', () => {
        const collection = new Collection(['link.css', 'README.md']);

        assert.doesNotThrow(
            () => json(collection, runner)
        );

        assert.notCalled(runner.tool);
    });

    it('should run expflags-lint for json exp-flags files changed', () => {
        const collection = new Collection(['expflags/foo.json']);

        assert.doesNotThrow(
            () => json(collection, runner)
        );

        assert.calledWith(runner.tool, 'expflags-lint.js "expflags/foo.json"');
    });

    it('should fail on invalid json', () => {
        const collection = new Collection(['invalid.json']);

        fsStub.readFileSync.withArgs('invalid.json', 'utf-8').returns('{');

        assert.throws(
            () => json(collection, runner)
        );
    });

    it('should not fail on valid json', () => {
        const collection = new Collection(['valid.json']);

        fsStub.readFileSync.withArgs('valid.json', 'utf-8').returns('{}');

        assert.doesNotThrow(
            () => json(collection, runner)
        );
    });
});
