/* global it */

var assert = require('assert');
var express = require('express');
var request = require('supertest');
var bundleResponse = require('./index');

it('should render simple page', function(done) {
    var app = express();

    app.use(bundleResponse({ cwd: 'fixtures' }));
    app.get('/', function(req, res) {
        res.bundle.render('index');
    });

    request(app)
        .get('/')
        .expect(/Privet from page block of index priv/)
        .expect(200)
        .end(done);
});

it('should apply lang overrides', function(done) {
    var app = express();

    app.use(bundleResponse({ cwd: 'fixtures' }));
    app.get('/', function(req, res) {
        res.bundle
            .lang('en')
            .render('index');
    });

    request(app)
        .get('/')
        .expect(/Hello from page block of index priv/)
        .expect(200)
        .end(done);
});

it('should apply type overrides', function(done) {
    var app = express();

    app.use(bundleResponse({ cwd: 'fixtures' }));
    app.get('/', function(req, res) {
        res.bundle
            .type('touch')
            .render('index');
    });

    request(app)
        .get('/')
        .expect(/Hello from touch page block of index priv/)
        .expect(200)
        .end(done);
});

it('should forward errors to handler', function(done) {
    var app = express();

    app.use(bundleResponse({ cwd: 'fixtures' }));
    app.get('/', function(req, res) {
        res.bundle.render('wat');
    });

    app.use(function(err, req, res, next) {
        assert.ok(/Priv file not found in /.test(err.message));
        done();
    });

    request(app)
        .get('/')
        .end();
});

it('should forward syntax errors to handler', function(done) {
    var app = express();

    app.use(bundleResponse({ cwd: 'fixtures' }));
    app.get('/', function(req, res) {
        res.bundle.render('error');
    });

    app.use(function(err, req, res, next) {
        assert.ok(/Unexpected token/.test(err.message));
        done();
    });

    request(app)
        .get('/')
        .end();
});
