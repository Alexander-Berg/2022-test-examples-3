'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const bemhtml = require('../linters/bemhtml.js');

describe('githooks / bemhtml', () => {
    let runner;

    beforeEach(() => {
        runner = { tool: sinon.stub() };
    });

    it('should skip if none of bemhtml.js files changed', () => {
        const collection = new Collection(['link.css', 'link.js']);

        bemhtml(collection, runner);

        assert.notCalled(runner.tool);
    });

    it('should run check-bemhint for js files changed', () => {
        const collection = new Collection(['link.css', 'link.bemhtml.js', 'foo.bemhtml.js']);

        bemhtml(collection, runner);

        assert.calledWith(runner.tool, 'check-bemhtml.js "link.bemhtml.js" "foo.bemhtml.js"');
    });
});
