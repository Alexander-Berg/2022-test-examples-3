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

describe('generation', function() {
    it('request should have secretkey property', function(done) {
        let app = express();

        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(expressSecretKey())
            .get('/', function(req, res) {
                should.exist(req.secretkey);
                res.status(200).end();
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

    it('request should have secretkey property for version 2', function(done) {
        let app = express();

        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(expressSecretKey({ version: 2, salt: 'abc' }))
            .get('/', function(req, res) {
                should.exist(req.secretkey);
                res.status(200).end();
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

    it('salt property must be provided if version 2 is used', function(done) {
        let app = express();

        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(require('express-blackbox')())
            .use(expressSecretKey({ version: 2 }))
            .use(function(err, req, res, next) {
                should.equal(err.message, 'options.salt is required for v2 keys');
                done();
            })
            .get('/', function(req, res) {
                res.status(200).end();
            });

        request(app)
            .get('/')
            .set('Host', 'tests.yandex.ru')
            .end();
    });

    it('should throw an error if blackbox middleware is not configured', function(done) {
        let app = express();

        app
            .use(cookieParser())
            .use(uatraits())
            .use(require('express-yandexuid')())
            .use(expressSecretKey())
            .use(function(err, req, res, next) {
                should.equal(err.message, 'Misconfigured express-secretkey: blackbox middleware is not installed');
                done();
            });

        request(app)
            .get('/').end();
    });
});
