'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');

const fsStub = {};
const gitStub = {};
const flags = proxyquire('../linters/flags.js', {
    fs: fsStub,
    '../lib/vcs': gitStub
});

describe('githooks / flags', () => {
    let runner;

    beforeEach(() => {
        fsStub.existsSync = sinon.stub();
        gitStub.diffIn = sinon.stub();

        runner = { log: sinon.stub() };
    });

    it('should fail if source file added to exp-level without flag-json', () => {
        const collection = new Collection(['experiments/foo/block-common/link/link.js']);

        gitStub.diffIn.returns('// some code');

        assert.throws(
            () => flags(collection, runner)
        );
    });

    it('should fail if js-source file modified without flag-json', () => {
        const collection = new Collection(['link.priv.js']);

        gitStub.diffIn.returns(`
+ _.prop(data, 'reqdata.flags.foo')
+ data.expFlags.bar
+ context.expFlags[\'baz\']
        `);

        assert.throws(
            () => flags(collection, runner)
        );
    });

    it('should fail if ts-source file modified without flag-json', () => {
        const collection = new Collection(['link.ts']);

        gitStub.diffIn.returns(`
+ context.expFlags.bar
        `);

        assert.throws(
            () => flags(collection, runner)
        );
    });

    it('should not fail if source file added to exp-level with flag-json', () => {
        const collection = new Collection(['experiments/foo/block-common/link/link.js']);

        fsStub.existsSync.withArgs('expflags/foo.json').returns(true);
        gitStub.diffIn.returns('// some code');

        assert.doesNotThrow(
            () => flags(collection, runner)
        );
    });

    it('should not fail if common source file modified with flag-json', () => {
        const collection = new Collection(['link.ts']);

        fsStub.existsSync.withArgs('expflags/foo.json').returns(true);
        fsStub.existsSync.withArgs('expflags/bar.json').returns(true);
        fsStub.existsSync.withArgs('expflags/baz.json').returns(true);
        gitStub.diffIn.returns(`
+ _.prop(data, 'reqdata.flags.foo')
+ data.expFlags.bar
+ context.expFlags[\'baz\']
        `);

        assert.doesNotThrow(
            () => flags(collection, runner)
        );
    });

    it('should not fail if hermione test was modified', () => {
        const collection = new Collection(['hermione/commands/libs/pages/serp/utils.js']);

        fsStub.existsSync.withArgs('hermione/commands/libs/pages/serp/utils.js').returns(true);
        gitStub.diffIn.returns("+ const hasSmartbannerFlag = expFlags.some(flag => flag.startsWith('smartbanner_atom'));");

        assert.doesNotThrow(
            () => flags(collection, runner)
        );
    });
});
