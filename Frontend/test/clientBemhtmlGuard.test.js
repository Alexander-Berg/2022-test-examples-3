'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const clientBemhtmlGuard = require('../linters/client-bemhtml-guard.js');

describe('githooks / clientBemhtmlGuard', () => {
    let runner;

    beforeEach(() => {
        runner = { tool: sinon.stub() };
    });

    it('should skip if none of deps.js files changed', () => {
        const collection = new Collection(['link.css', 'link.js']);

        clientBemhtmlGuard(collection, runner);

        assert.notCalled(runner.tool);
    });

    it('should run client-bemhtml-guard if enb-config changed', () => {
        const collection = new Collection(['link.css', '.enb/config/index.js']);

        clientBemhtmlGuard(collection, runner);

        assert.calledWith(runner.tool, 'client-bemhtml-guard.js ".enb/config/index.js"');
    });

    it('should run client-bemhtml-guard if deps.js files changed', () => {
        const collection = new Collection(['link.css', 'link.deps.js']);

        clientBemhtmlGuard(collection, runner);

        assert.calledWith(runner.tool, 'client-bemhtml-guard.js "link.deps.js" ".enb/config/index.js"');
    });
});
