'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');

const gitStub = {};
const fsStub = {};
const chalkStub = {
    red: () => '# error',
    yellow: () => '# warning',
    green: () => '# text'
};
const defaultFlagsTest = proxyquire('../linters/default-flags.js', {
    '../lib/vcs': gitStub,
    fs: fsStub,
    chalk: chalkStub
});

describe('githooks / defaultFlags', () => {
    let runner;

    beforeEach(() => {
        gitStub.diffIn = sinon.stub();
        fsStub.readFileSync = () => ({ platforms: ['desktop', 'touch-phone'] });

        runner = { log: sinon.stub() };
    });

    it('should do nothing if not have been changed any js files in src\/lib\/flags\/common\/ or src\/lib\/flags\/testing\/', () => {
        const collection = new Collection(['foo.js']);

        defaultFlagsTest(collection, runner);

        assert.notCalled(runner.log);
    });

    it('should not throw if flags not touched', () => {
        const collection = new Collection(['src/lib/flags/common/foo.js']);

        gitStub.diffIn.returns('+ some code');

        assert.doesNotThrow(
            () => defaultFlagsTest(collection, runner)
        );
    });

    it('should not throw if flags touched with comment', () => {
        const collection = new Collection(['src/lib/flags/common/foo.js']);

        gitStub.diffIn.returns('+ // SERP-0000\n+ flags[\'flag\'] = 1;');

        assert.doesNotThrow(
            () => defaultFlagsTest(collection, runner)
        );
    });

    it('should throw if flags touched without comment', () => {
        const collection = new Collection(['src/lib/flags/common/foo.js']);

        gitStub.diffIn.returns('+ flags[\'flag\'] = 1;');

        assert.throws(
            () => defaultFlagsTest(collection, runner)
        );
    });

    it('should throw if flags renamed', () => {
        const collection = new Collection(['src/lib/flags/common/foo.js']);

        gitStub.diffIn.returns('- const flags = base(data);');

        assert.throws(
            () => defaultFlagsTest(collection, runner)
        );
    });

    it('should throw if declaration for expflag not exists', () => {
        const collection = new Collection(['src/lib/flags/testing/index@touch-phone.js']);
        fsStub.readFileSync = () => {};

        gitStub.diffIn.returns('+++ @touch-phone.js\n+ // SERP-0000\n+ flags[\'flag\'] = 1;');

        assert.throws(
            () => defaultFlagsTest(collection, runner)
        );
    });

    it('should not throw if declaration for expflag exists', () => {
        const collection = new Collection(['src/lib/flags/testing/index@touch-phone.js']);
        fsStub.readFileSync = () => ({ platforms: ['touch-phone'] });

        gitStub.diffIn.returns('+++ @touch-phone.js\n+ // SERP-0000\n+ flags[\'flag\'] = 1;');

        assert.doesNotThrow(
            () => defaultFlagsTest(collection, runner)
        );
    });
});
