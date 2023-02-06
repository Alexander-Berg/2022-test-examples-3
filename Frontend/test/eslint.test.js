'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const eslint = require('../linters/eslint.js');

describe('githooks / eslint', () => {
    let runner;

    beforeEach(() => {
        runner = { npx: sinon.stub() };
    });

    it('should skip if none of js/ts/tsx files changed', () => {
        const collection = new Collection(['link.css', 'README.md']);

        eslint(collection, runner);

        assert.notCalled(runner.npx);
    });

    it('should run eslint for js/tsx? files changed', () => {
        const collection = new Collection(['link.css', 'link.js', 'link.ts', 'link.tsx']);

        eslint(collection, runner);

        assert.calledWith(runner.npx, 'eslint "link.js" "link.ts" "link.tsx"');
    });
});
