'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const noSvg = require('../linters/no-svg.js');

describe('githooks / noSvg', () => {
    let runner;

    beforeEach(() => {
        runner = { log: sinon.stub() };
    });

    it('should pass silently if no svg files added', () => {
        const collection = new Collection(['newtest.hermione.js', 'some.js', 'some.styl']);

        assert.doesNotThrow(
            () => noSvg(collection, runner)
        );
    });

    it('should fail if general file added', () => {
        const collection = new Collection(['my-custom-icon.svg']);

        assert.throws(
            () => noSvg(collection, runner)
        );
    });

    it('should pass silently if added svg files in experiments folder', () => {
        const collection = new Collection(['newtest.hermione.js', 'experiments/beauty_experiment/blocks-common/verified/verified_type_favorite.svg', 'some.styl']);

        assert.doesNotThrow(
            () => noSvg(collection, runner)
        );
    });
});
