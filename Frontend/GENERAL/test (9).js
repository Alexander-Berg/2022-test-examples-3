/* global it */

let assert = require('assert');
let request = require('supertest');
let express = require('express');
let expressRenewBbSession = require('./');

it('redirects to passport.yandex.TLD/auth/update/ on NEED_RESET', function(done) {
    let app = express()
        .use(function(req, res, next) {
            req.tld = 'TLD';
            req.blackbox = { status: 'NEED_RESET' };
            next();
        })
        .use(expressRenewBbSession());

    request(app)
        .get('/some?url=1')
        .expect('Location', 'https://passport.yandex.TLD/auth/update/?retpath=http%3A%2F%2F127.0.0.1%2Fsome%3Furl%3D1')
        .expect(302)
        .end(done);
});

it('redirects according to x-original-url header if set', function(done) {
    let app = express()
        .use(function(req, res, next) {
            req.tld = 'ru';
            req.blackbox = { status: 'NEED_RESET' };
            next();
        })
        .use(expressRenewBbSession());

    request(app)
        .get('/some?url=1')
        .set('x-original-url', 'http://127.0.0.1/app/some?url=1')
        .expect('Location', 'https://passport.yandex.ru/auth/update/?retpath=http%3A%2F%2F127.0.0.1%2Fapp%2Fsome%3Furl%3D1')
        .expect(302)
        .end(done);
});

it('requires express-tld', function(done) {
    let app = express()
        .use(expressRenewBbSession())
        .use((err, req, res, next) => {
            assert(err.name, 'MissingMiddleware');
            assert(err.message.match(/express-tld/));
            next(err, req, res);
        });

    request(app)
        .get('/')
        .expect(500)
        .end(done);
});

it('requires express-blackbox', function(done) {
    let app = express()
        .use(function(req, res, next) {
            req.tld = 'ru';
            next();
        })
        .use(expressRenewBbSession())
        .use((err, req, res, next) => {
            assert(err.name, 'MissingMiddleware');
            assert(err.message.match(/express-blackbox/));
            next(err, req, res);
        });

    request(app)
        .get('/')
        .expect(500)
        .end(done);
});
