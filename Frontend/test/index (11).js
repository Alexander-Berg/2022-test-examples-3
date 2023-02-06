/* global describe, it */
/* eslint-disable no-new */

let assert = require('assert');
let Bundle = require('..');

it('should return a self-invoking constructor', function() {
    assert.equal(typeof(Bundle), 'function');

    assert.throws(function() { new Bundle() }, /bundlePath is required/);

    let bundle = new Bundle('test/fixtures/desktop.bundles/index');
    assert.ok(bundle instanceof Bundle);
    bundle = new Bundle('test/fixtures/desktop.bundles/index');
    assert.ok(bundle instanceof Bundle);
    bundle = new Bundle(__dirname + '/fixtures/desktop.bundles/index');
    assert.ok(bundle instanceof Bundle);
});

it('should return object properties', function() {
    let bundle = new Bundle('test/fixtures/desktop.bundles/index');
    assert.ok(typeof(bundle.priv), 'object');
    assert.ok(typeof(bundle.privPath), 'string');
    assert.ok(typeof(bundle.template), 'object');
    assert.ok(typeof(bundle.templatePath), 'string');
    assert.equal(bundle.name, 'index');
});

it('should preserve context in priv and bemhtml files', function() {
    let bundle = new Bundle('test/fixtures/desktop.bundles/index');
    assert.ok(bundle.priv.exec(), 'right');
    assert.ok(bundle.template.apply(), 'right');
});

it('should return undefined if priv is missing', function() {
    let bundle = new Bundle('test/fixtures/desktop.bundles/empty');
    assert.equal(bundle.priv, undefined);
    assert.equal(bundle.template, undefined);
    assert.equal(bundle.name, 'empty');
});

it('should return undefined on invalid bundle path', function() {
    let bundle = new Bundle('test/fixtures/desktop.bundles/wrong');
    assert.equal(bundle.priv, undefined);
    assert.equal(bundle.template, undefined);
    assert.equal(bundle.name, 'wrong');
});

it('should cache requires', function(done) {
    let foo = new Bundle('test/fixtures/desktop.bundles/index', { lang: 'date' });
    setTimeout(function() {
        let bar = new Bundle('test/fixtures/desktop.bundles/index', { lang: 'date' });

        assert.ok(bar.priv.exec() === foo.priv.exec());
        done();
    }, 10);
});

// https://st.yandex-team.ru/FRONTEND-663
it.skip('should throw on syntax errors', function() {
    assert.throws(function() {
        new Bundle('test/fixtures/desktop.bundles/corrupted');
    });

    assert.throws(function() {
        new Bundle('test/fixtures/desktop.bundles/nomodule');
    });
});

describe('options', function() {
    it('should have cwd option', function() {
        let bundle = new Bundle('desktop.bundles/index', { cwd: 'test/fixtures' });
        assert.ok(typeof(bundle.priv), 'object');
        assert.ok(typeof(bundle.template), 'object');
        assert.equal(bundle.name, 'index');
    });

    it('should have lang option', function() {
        let bundle = new Bundle('test/fixtures/desktop.bundles/index', { lang: 'ru' });
        assert.ok(typeof(bundle.priv), 'object');
        assert.ok(bundle.priv.exec(), 'ru');
        assert.ok(typeof(bundle.template), 'object');
        assert.equal(bundle.name, 'index');
    });

    it('should have dev option', function(done) {
        let foo = new Bundle('test/fixtures/desktop.bundles/index', { lang: 'date', dev: true });
        setTimeout(function() {
            let bar = new Bundle('test/fixtures/desktop.bundles/index', { lang: 'date', dev: true });
            assert.ok(bar.priv.exec() > foo.priv.exec());
            done();
        }, 10);
    });

    // TODO: Restore test
    it.skip('should have i18n option', function() {
        let bundle = new Bundle('test/fixtures/desktop.bundles/bevisTemplate', { lang: 'ru', templateExt: 'bt.js' });
        assert.ok(typeof(bundle.priv), 'object');
        assert.ok(bundle.priv.i18n(), 'i18n');
        assert.ok(bundle.template.lib.i18n(), 'i18n');
        assert.ok(typeof(bundle.template), 'object');
        assert.equal(bundle.name, 'bevisTemplate');
    });
});
