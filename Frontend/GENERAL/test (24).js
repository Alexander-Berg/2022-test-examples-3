/* eslint-env node, mocha */

const assert = require('assert');
const requireUncached = require('require-uncached');
const clearModule = require('clear-module');

afterEach('remove NODE_ENV', done => {
    delete process.env.NODE_ENV;
    clearModule('./configs/env.js');
    done();
});

it('should extended with env.js on set environment', () => {
    process.env.NODE_ENV = 'production';
    const cfg = requireUncached('./');
    assert.strictEqual(cfg.unicorns, '💩');
    assert.strictEqual(cfg.deep.value, 2);
    clearModule('./configs/env.js');

    process.env.UNICORNS = '🤘';
    const cfg2 = requireUncached('./');
    assert.strictEqual(cfg2.unicorns, '🤘');
    assert.strictEqual(cfg2.deep.value, 2);
    delete process.env.UNICORNS;
    clearModule('./configs/env.js');

    process.env.DEEP_VALUE = '🤘';
    const cfg3 = requireUncached('./');
    assert.strictEqual(cfg3.unicorns, '💩');
    assert.strictEqual(cfg3.deep.value, '🤘');
    delete process.env.DEEP_VALUE;
    clearModule('./configs/env.js');
});
