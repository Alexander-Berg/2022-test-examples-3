/* global it */
'use strict';

let assert = require('assert');
let bundle = require('./index');

let opts = {
    cwd: 'fixtures',
};

it('should render simple page', function(done) {
    bundle(opts)
        .render('index', function(err, html) {
            assert.ok(/simple/.test(html));
            done();
        });
});

it('should support types', function(done) {
    bundle(opts)
        .type('desktop')
        .render('index', function(err, html) {
            assert.ok(/desktop\.block/.test(html));
            done();
        });
});

it('should support langs', function(done) {
    bundle(opts)
        .lang('ru')
        .render('index', function(err, html) {
            assert.ok(/russian/.test(html));
            done();
        });
});

it('should not set type for undefined flag', function(done) {
    bundle(opts)
        .type('desktop', undefined)
        .render('index', function(err, html) {
            assert.ok(/simple/.test(html));
            done();
        });
});

it('should get bundle', function() {
    let b = bundle(opts)
        .type('desktop')
        .get('index');

    assert.ok(b.priv);
    assert.ok(b.template);
    assert.equal(b.name, 'index');
});

it('should work with empty typeExt', function(done) {
    bundle({ cwd: 'fixtures', typeExt: '' })
        .type('desktop')
        .render('index', function(err, html) {
            assert.ok(/Hello from unprefixed bundle/.test(html));
            done();
        });
});

it('should render page without priv', function(done) {
    bundle({ cwd: 'fixtures', ignorePriv: true })
        .render('nopriv', function(err, html) {
            assert.equal(html, '{"page":"nopriv"}');
            done();
        });
});

it('should not catch exceptions from callback', function() {
    assert.throws(function() {
        bundle(opts)
            .render('index', function(err) {
                if (err) { throw new Error('Error was passed!') }
                throw new Error('Hello!');
            });
    }, /Hello!/);
});
