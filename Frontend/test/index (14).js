/* global beforeEach, describe, it */

let assert = require('assert');
let requireUncached = require('require-uncached');
let mock = require('mock-require');

describe('cfg', function() {
    beforeEach(function() {
        process.env.NODE_ENV = process.env.CONFIG_ENV = process.env.QLOUD_ENVIRONMENT = process.env.CFG_DIR = '';
        mock('yandex-environment', './mocks/yandex-environment');
    });

    it('should return default configs', function() {
        let cfg = requireUncached('./../');
        assert.strictEqual(cfg.unicorns, 'rainbows');
    });

    it('should extend defaults with environment config', function() {
        process.env.NODE_ENV = 'production';
        let cfg = requireUncached('./../');
        assert.ok(cfg.horse);
        assert.strictEqual(cfg.unicorns, '💩');
        assert.strictEqual(cfg.deep.value, 2);
        assert.strictEqual(cfg.fn(), 2);
        assert(Array.isArray(cfg.array));
        assert.equal(cfg.array.toString(), '3');
    });

    it('should return production configs with production NODE_ENV', function() {
        process.env.NODE_ENV = 'production';
        let cfg = requireUncached('./../');
        assert.strictEqual(cfg.unicorns, '💩');
        assert.strictEqual(cfg.deep.value, 2);
        assert.strictEqual(cfg.fn(), 2);
        assert(Array.isArray(cfg.array));
        assert.equal(cfg.array.toString(), '3');
        delete process.env.NODE_ENV;
        cfg = requireUncached('./../');
        assert.strictEqual(cfg.unicorns, 'rainbows');
        assert.strictEqual(cfg.deep.value, 1);
        assert.strictEqual(cfg.fn(), 1);
        assert(Array.isArray(cfg.array));
        assert.equal(cfg.array.toString(), '1,2');
    });

    it('should return production configs with production QLOUD_ENVIRONMENT', function() {
        process.env.QLOUD_ENVIRONMENT = 'production';
        let cfg = requireUncached('./../');
        assert.strictEqual(cfg.unicorns, '💩');
        assert.strictEqual(cfg.deep.value, 2);
        delete process.env.QLOUD_ENVIRONMENT;
        cfg = requireUncached('./../');
        assert.strictEqual(cfg.unicorns, 'rainbows');
        assert.strictEqual(cfg.deep.value, 1);
    });

    it('should return production configs with production CONFIG_ENV', function() {
        process.env.CONFIG_ENV = 'production';
        let cfg = requireUncached('./../');
        assert.strictEqual(cfg.unicorns, '💩');
        assert.strictEqual(cfg.deep.value, 2);
        delete process.env.CONFIG_ENV;
        cfg = requireUncached('./../');
        assert.strictEqual(cfg.unicorns, 'rainbows');
        assert.strictEqual(cfg.deep.value, 1);
    });

    it('should export defaults on unknown environment', function() {
        process.env.NODE_ENV = 'unknown';
        let cfg = requireUncached('./../');
        assert.ok(cfg.horse);
        assert.strictEqual(cfg.unicorns, 'rainbows');
        assert.strictEqual(cfg.deep.value, 1);
    });
});
