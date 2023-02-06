/**
*   TODO: линтеры ругаются на no-unused-vars: чаще всего в express коллбеках.
*   Убираем unused vars из параметров функции – падают тесты.
*/
/* eslint-disable */

/* global describe, it */

let expressSecretKey = require('..');
let express = require('express');
let request = require('supertest');
let should = require('should');
let uatraits = require('express-uatraits-mock');
let cookieParser = require('cookie-parser');

describe('validation', function() {
    it('request should invalidate wrong sk query param', function(done) {
        let app = express();
        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(expressSecretKey())
            .post('/', expressSecretKey().validate, function(err, req, res, next) {
                done();
            });

        request(app)
            .post('/?sk=ololo')
            .set('Host', 'tests.yandex.ru')
            .expect(403)
            .end(function(err) {
                if (err) { return done(err) }
                done();
            });
    });

    it('request should validate right sk query param', function(done) {
        let app = express();
        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(expressSecretKey())
            .get('/', function(req, res) {
                res.json({
                    secretkey: req.secretkey,
                    yandexuid: req.cookies.yandexuid,
                });
            })
            .post('/', expressSecretKey().validate, function(req, res) {
                res.end();
            });

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .expect(200)
            .end(function(err, res) {
                if (err) { return done(err) }

                request(app)
                    .post('/?sk=' + res.body.secretkey)
                    .set('cookie', 'yandexuid=' + res.body.yandexuid)
                    .expect(200)
                    .end(function(err, res) {
                        if (err) { return done(err) }
                        done();
                    });
            });
    });

    it('should validate right sk query param for version 2', function(done) {
        let app = express();
        let opts = { version: 2, salt: 'abc' };

        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(expressSecretKey(opts))
            .get('/', function(req, res) {
                res.json({
                    secretkey: req.secretkey,
                    yandexuid: req.cookies.yandexuid,
                });
            })
            .post('/', expressSecretKey(opts).validate, function(req, res) {
                res.end();
            });

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .expect(200)
            .end(function(err, res) {
                if (err) { return done(err) }

                request(app)
                    .post('/?sk=' + res.body.secretkey)
                    .set('cookie', 'yandexuid=' + res.body.yandexuid)
                    .expect(200)
                    .end(function(err, res) {
                        if (err) { return done(err) }
                        done();
                    });
            });
    });

    it('request should not check ignored methods', function(done) {
        let app = express();
        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(expressSecretKey())
            .get('/', expressSecretKey().validate, function(req, res) {
                res.end();
            });

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .expect(200)
            .end(function(err) {
                if (err) { return done(err) }
                done();
            });
    });

    it('request should not validate invalid blackbox statuses', function(done) {
        let app = express();
        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(function(req, res, next) {
                req.cookies.yandexuid = 100500;
                req.blackbox.uid = 100500;
                req.blackbox.status = 'VALID';
                next();
            })
            .use(expressSecretKey())
            .get(
                '/',
                function(req, res) {
                    res.json({
                        secretkey: req.secretkey,
                        yandexuid: req.cookies.yandexuid,
                    });
                }
            )
            .post(
                '/',
                function(req, res, next) {
                    delete req.blackbox.uid;
                    req.blackbox.status = 'INVALID';
                    next();
                },
                expressSecretKey().validate,
                function(req, res) {
                    res.end();
                }
            );

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .expect(200)
            .end(function(err, res) {
                if (err) { return done(err) }

                request(app)
                    .post('/?sk=' + res.body.secretkey + '&')
                    .set('cookie', 'yandexuid=' + res.body.yandexuid)
                    .expect(403)
                    .end(function(err, res) {
                        if (err) { return done(err) }
                        done();
                    });
            });
    });

    it('request should validate with NEED_RESET status', function(done) {
        let app = express();
        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(function(req, res, next) {
                req.cookies.yandexuid = 100500;
                req.blackbox.uid = 100500;
                req.blackbox.status = 'VALID';
                next();
            })
            .use(expressSecretKey())
            .get(
                '/',
                function(req, res) {
                    res.json({
                        secretkey: req.secretkey,
                        yandexuid: req.cookies.yandexuid,
                    });
                }
            )
            .post(
                '/',
                function(req, res, next) {
                    req.blackbox.status = 'NEED_RESET';
                    next();
                },
                expressSecretKey().validate,
                function(req, res) {
                    res.end();
                }
            );

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .expect(200)
            .end(function(err, res) {
                if (err) { return done(err) }

                request(app)
                    .post('/?sk=' + res.body.secretkey + '&')
                    .set('cookie', 'yandexuid=' + res.body.yandexuid)
                    .expect(200)
                    .end(function(err, res) {
                        if (err) { return done(err) }
                        done();
                    });
            });
    });

    it('request should not fail with empty token in v2', function(done) {
        let opts = { version: 2, salt: 'abc' };

        let app = express();
        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(function(req, res, next) {
                req.cookies.yandexuid = 100500;
                next();
            })
            .use(expressSecretKey(opts))
            .post('/', expressSecretKey(opts).validate, function(err, req, res, next) {
                let status = err && err.status;
                res.status(status);

                if (status !== 403) {
                    next(err);
                } else {
                    res.end();
                }
            });

        request(app)
            .post('/')
            .set('Host', 'tests.yandex.ru')
            .expect(403)
            .end(function(err) {
                if (err) { return done(err) }
                done();
            });
    });
});
