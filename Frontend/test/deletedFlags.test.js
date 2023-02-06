'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');

const fsStub = {};
const gitStub = {};
const chalkStub = {
    red: () => '# error',
    yellow: () => '# warning',
    green: () => '# text'
};
const deletedFlagsTest = proxyquire('../linters/deleted-flags.js', {
    fs: fsStub,
    chalk: chalkStub
});

describe('githooks / deletedFlags', () => {
    let runner;

    beforeEach(() => {
        gitStub.diffIn = sinon.stub();

        runner = { log: sinon.stub() };
    });

    it('should do nothing if not have been changed any files in expflags\/', () => {
        const collection = new Collection(['foo.js']);

        deletedFlagsTest(collection, runner);

        assert.notCalled(runner.log);
    });

    it('should not throw if flags not exist in src\/lib\/flags\/common\/ or src\/lib\/flags\/testing\/', () => {
        const collection = new Collection(['expflags/foo.json']);

        assert.doesNotThrow(
            () => deletedFlagsTest(collection, runner)
        );
    });

    it('should throw if flags exist in src\/lib\/flags\/common\/', () => {
        const collection = new Collection(['expflags/serp3_baseline.json']);

        assert.throws(
            () => deletedFlagsTest(collection, runner)
        );
    });

    it('should throw if flags exist in src\/lib\/flags\/testing\/', () => {
        const collection = new Collection(['expflags/typo-serp-adv-sitelinks.json']);

        assert.throws(
            () => deletedFlagsTest(collection, runner)
        );
    });
});
