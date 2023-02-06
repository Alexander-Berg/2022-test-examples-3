'use strict';

var request = require('supertest'),
    common = require('./common.js'),
    getApp = common.getApp,
    shouldNotRedirect = common.shouldNotRedirect,
    shouldNotRedirectOrSetCookie = common.shouldNotRedirectOrSetCookie;

describe('middleware', function() {
    it('should set yandexuid, if request was from yandex.ru domain', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.ru/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if request was from yandex.by domain', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.by')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.by/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if request was from yandex.kz domain', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.kz')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.kz/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if request was from yandex.ua domain', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ua')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.ua/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if request was from ya.ru', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'p.ya.ru')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.ya\.ru/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if request was from yandex.com', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'yandex.com')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.com/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if request was from yandex-ad.cn', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'yandex-ad.cn')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex-ad\.cn/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if request was from yandex-team.ru', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'yandex-team.ru')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex-team\.ru/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if request was from yandex.eu', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.eu')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.eu/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should set yandexuid, if there is mda=0 cookie', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ua')
            .set('Cookie', 'mda=0')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.ua/)
            .expect(200)
            .end(shouldNotRedirect(done));
    });

    it('should preserve cookie, if it was in cookies before', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ua')
            .set('Cookie', 'yandexuid=123456789' + String(Date.now()).substr(0, 10))
            .end(shouldNotRedirectOrSetCookie(done));
    });

    it('should do nothing if the request came from an user agent that isn\'t a browser', function(done) {
        var app = getApp({ isBrowser: false });

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .end(shouldNotRedirectOrSetCookie(done));
    });

    it('should do nothing if the request came from a bot', function(done) {
        var app = getApp({ isRobot: true });

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .end(shouldNotRedirectOrSetCookie(done));
    });

    it('should do nothing if the request came from a browser that doesn\'t support cookies', function(done) {
        var app = getApp();

        request(app)
            .get('/?nocookiesupport=yes')
            .set('Host', 'tests.yandex.ru')
            .end(shouldNotRedirectOrSetCookie(done));
    });

    it('should do nothing if the request came from localhost', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'localhost')
            .end(shouldNotRedirectOrSetCookie(done));
    });

    it('should do nothing if the request came from non root domain', function(done) {
        var app = getApp();

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.net')
            .end(shouldNotRedirectOrSetCookie(done));
    });
});
