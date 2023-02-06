'use strict';

/* global it */

var assert = require('assert');
var request = require('supertest');
var app = require('express')();

var domainAccess = require('./')('ru, ua, com.tr');

app.use(require('express-tld')());
app.use(domainAccess);
app.get('/', function(req, res) {
    return res.end('Ok');
});

it('should accept string and return function', function() {
    assert.equal(typeof domainAccess, 'function');
});

it('should return status 404 if domain not available', function(done) {
    request(app)
        .get('/')
        .set('Host', 'yandex.net')
        .expect(404)
        .end(done);
});

it('should call next if domain available', function(done) {
    request(app)
        .get('/')
        .set('Host', 'yandex.ru')
        .expect(200, 'Ok')
        .end(done);
});
