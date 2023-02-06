/* global describe, it */
/* jshint expr:true */
/* eslint-disable @typescript-eslint/no-unused-vars */

// eslint-disable-next-line no-unused-vars
let should = require('should');
let request = require('supertest');
let common = require('./common');
let getApp = common.getApp;
let mw = common.mw;
let BELORUSSIAN_YANDEX_GID = common.BELORUSSIAN_YANDEX_GID;
let KHARKOVS_IP_ADDRESS = common.KHARKOVS_IP_ADDRESS;
let YEKATERINBURGS_IP_ADDRESS = common.YEKATERINBURGS_IP_ADDRESS;

describe('National redirects', function() {
    it('Should not redirect with russian IP-address from www.yandex.ru', function(done) {
        let app = getApp();

        app.get('/', mw());

        request(app)
            .get('/')
            .set('Host', 'www.yandex.ru')
            .set('X-Real-IP', YEKATERINBURGS_IP_ADDRESS)
            .set('X-Forwarded-For', YEKATERINBURGS_IP_ADDRESS)
            .expect(200)
            .end(done);
    });

    it('Should redirect with ukraine IP-address from www.yandex.ru to www.yandex.ua', function(done) {
        let app = getApp();

        app.get('/', mw());

        request(app)
            .get('/')
            .set('Host', 'www.yandex.ru')
            .set('X-Real-IP', KHARKOVS_IP_ADDRESS)
            .set('X-Forwarded-For', KHARKOVS_IP_ADDRESS)
            .expect(302)
            .end(function(err, res) {
                if (err) return done(err);

                res.statusCode.should.be.eql(302);
                res.status.should.be.eql(302);

                res.redirect.should.be.true;

                res.headers.location.should.startWith('http://www.yandex.ua');
                done();
            });
    });

    it('Should redirect with belorussian yandex_gid cookie from www.yandex.ru to www.yandex.by', function(done) {
        let app = getApp();

        app.get('/', mw());

        request(app)
            .get('/')
            .set('Host', 'www.yandex.ru')
            .set('Cookie', 'yandex_gid=' + BELORUSSIAN_YANDEX_GID)
            .set('X-Real-IP', KHARKOVS_IP_ADDRESS)
            .set('X-Forwarded-For', KHARKOVS_IP_ADDRESS)
            .expect(302)
            .end(function(err, res) {
                if (err) return done(err);

                res.statusCode.should.be.eql(302);
                res.status.should.be.eql(302);

                res.redirect.should.be.true;

                res.headers.location.should.startWith('http://www.yandex.by');
                done();
            });
    });

    it('Should not redirect with belorussian yandex_gid cookie from www.yandex.ua', function(done) {
        let app = getApp();

        app.get('/', mw());

        request(app)
            .get('/')
            .set('Host', 'www.yandex.ua')
            .set('Cookie', 'yandex_gid=' + BELORUSSIAN_YANDEX_GID)
            .set('X-Real-IP', KHARKOVS_IP_ADDRESS)
            .set('X-Forwarded-For', KHARKOVS_IP_ADDRESS)
            .expect(200)
            .end(done);
    });

    it('Should redirect with path and url prefix if X-Original-URL header set', function(done) {
        let app = getApp();

        app.get('/*', mw());

        request(app)
            .get('/download/')
            .set('Host', 'www.yandex.ru')
            .set('X-Original-URL', 'https://www.yandex.ru/myapp/download/')
            .set('X-Real-IP', KHARKOVS_IP_ADDRESS)
            .set('X-Forwarded-For', KHARKOVS_IP_ADDRESS)
            .expect(302)
            .end(function(err, res) {
                if (err) return done(err);

                res.statusCode.should.be.eql(302);
                res.status.should.be.eql(302);

                res.redirect.should.be.true;

                res.headers.location.should.startWith('http://www.yandex.ua/myapp/download/');
                done();
            });
    });
});
