'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const stylelint = require('../linters/stylelint.js');

describe('githooks / stylelint', () => {
    let runner;

    beforeEach(() => {
        runner = { npx: sinon.stub() };
    });

    it('should skip if none of scss files changed', () => {
        const collection = new Collection(['link.css', 'README.md']);

        stylelint(collection, runner);

        assert.notCalled(runner.npx);
    });

    it('should run stylelint for scss files changed', () => {
        const collection = new Collection(['README.md', 'link.scss', 'button.scss']);

        stylelint(collection, runner);

        assert.calledWith(runner.npx, 'stylelint "link.scss" "button.scss"');
    });
});
