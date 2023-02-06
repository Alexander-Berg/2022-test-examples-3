/* global describe, it */
/* eslint-disable no-var */

var data = {
    'yandex.ru': 'ru',
    'yandex.net': 'net',
    'yandex.com.tr': 'com.tr',
    'yandex.co.il': 'co.il',
    'some.host.ru': 'ru',
    'yandex.ru:8080': 'ru',
    'bla.yandex.kyprizel.net': 'net',
    localhost: 'localhost',
    '.': '',
    '127.0.0.1': undefined,
    '[::1]': undefined,
    '127.0.0.1:8080': undefined,
    '[::1]:8080': undefined,
};

var assert = require('assert');
var express = require('express');
var request = require('supertest');
var expressTld = require('.')();
var noop = function() {};

function t(host, tld) {
    it('"' + host + '" tld should equal "' + tld + '"', function() {
        var req = { headers: { host: host } };
        expressTld(req, {}, noop);
        assert.equal(tld, req.tld);
    });
}

describe('undefined host', function() {
    t(undefined, undefined);
});

describe('hosts in data.js', function() {
    Object.keys(data).forEach(function(key) {
        t(key, data[key]);
    });
});

describe('express', function() {
    it('works with supertest', function(done) {
        var app = express();

        app.use(expressTld);

        app.get('/', function(req, res) {
            res.send(req.tld);
        });

        request(app)
            .get('/')
            .expect('', done);
    });
});
