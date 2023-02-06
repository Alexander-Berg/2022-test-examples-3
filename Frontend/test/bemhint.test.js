'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const bemhint = require('../linters/bemhint.js');

describe('githooks / bemhint', () => {
    let runner;

    beforeEach(() => {
        runner = { npx: sinon.stub() };
    });

    it('should skip if none of js files changed', () => {
        const collection = new Collection(['link.css', 'README.md']);

        bemhint(collection, runner);

        assert.notCalled(runner.npx);
    });

    it('should run bemhint for js files changed', () => {
        const collection = new Collection(['link.css', 'link.js', 'button.js']);

        bemhint(collection, runner);

        assert.calledWith(runner.npx, 'bemhint "link.js" "button.js"');
    });
});
