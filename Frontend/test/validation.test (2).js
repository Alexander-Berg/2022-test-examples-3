'use strict';

var request = require('supertest'),
    common = require('./common.js'),
    getApp = common.getApp,
    shouldNotRedirect = common.shouldNotRedirect,
    shouldNotRedirectOrSetCookie = common.shouldNotRedirectOrSetCookie;

describe('validation', function() {
    it('valid cookies should be accepted', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .set('Cookie', 'yandexuid=123456789' + String(Date.now()).substr(0, 10))
            .end(shouldNotRedirectOrSetCookie(done));
    });

    it('invalid cookies should be denied', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .set('Cookie', 'yandexuid=123456789')
            .expect('Set-Cookie', (/((?!yandexuid=123456789).)*/))
            .end(shouldNotRedirect(done));
    });

    it('cookies with timestamp from future should be denied', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .set('Cookie', 'yandexuid=1234567899999999999')
            .expect('Set-Cookie', (/((?!yandexuid=123456789).)*/))
            .end(shouldNotRedirect(done));
    });

    it('cookies starting with zeroes should be denied', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .set('Cookie', 'yandexuid=023456789' + String(Date.now()).substr(0, 10))
            .expect('Set-Cookie', (/((?!yandexuid=123456789).)*/))
            .end(shouldNotRedirect(done));
    });
});
