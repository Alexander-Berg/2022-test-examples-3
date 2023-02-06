'use strict';

const path = require('path');
const assert = require('chai').assert;
const sinon = require('sinon');

const { getPathByPrefix, getMatchingTemplate } = require('./index');

sinon.assert.expose(assert, { prefix: '' });

describe('getPathByPrefix', () => {
    it('returns path without matching prefix', () => {
        const getTarget = getPathByPrefix('/static/web4', __dirname);

        const target = getTarget({ pathname: '/static/web4/test.js' });

        assert.strictEqual(target, path.resolve(__dirname, 'test.js'));
    });

    it('returns null when pathname does not match prefix', () => {
        const getTarget = getPathByPrefix('/static/web4', __dirname);

        const target = getTarget({ pathname: '/web4/test.js' });

        assert.strictEqual(target, null);
    });
});

describe('getMatchingTemplate', () => {
    let getTarget;
    before(() => {
        getTarget = getMatchingTemplate(path.resolve(__dirname, 'fixtures/templar-test-config.js'));
    });

    it('returns template by path match', () => {
        const target = getTarget({ pathname: '/search-path' });

        assert.strictEqual(target, path.resolve(__dirname, 'fixtures/renderer/test/path-renderer.js'));
    });

    it('returns template by json path match', () => {
        const target = getTarget({ pathname: '/search-matcher' });

        assert.strictEqual(target, path.resolve(__dirname, 'fixtures/renderer/test/matcher-renderer.js'));
    });

    it('returns template by matcher', () => {
        const target = getTarget({ pathname: '/search-json-path' });

        assert.strictEqual(target, path.resolve(__dirname, 'fixtures/renderer/test/json-path-renderer.js'));
    });

    it('returns null when no matching config is found', () => {
        const target = getTarget({ pathname: '/search-no-match' });

        assert.strictEqual(target, null);
    });
});
