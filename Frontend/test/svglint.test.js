'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const svglint = require('../linters/svglint.js');

describe('githooks / svglint', () => {
    let runner;

    beforeEach(() => {
        runner = { tool: sinon.stub() };
    });

    it('should skip if none of svg files changed', () => {
        const collection = new Collection(['link.css', 'link.js']);

        svglint(collection, runner);

        assert.notCalled(runner.tool);
    });

    it('should run check-svg for svg files changed', () => {
        const collection = new Collection(['icon.css', 'icon.svg']);

        svglint(collection, runner);

        assert.calledWith(runner.tool, 'check-svg.js "icon.svg"');
    });
});
