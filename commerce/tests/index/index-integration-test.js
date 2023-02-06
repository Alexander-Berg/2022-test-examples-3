'use strict';

const ip = '5.45.241.138';
const request = require('supertest');
const mockery = require('mockery');

const path = require('path');

process.env.NODE_ENV = process.env.NODE_ENV || require('yandex-environment') || 'development';
process.env.CFG_DIR = path.join(__dirname, '..', '..', '..', 'configs');
const config = require('cfg');

const nocks = require('../helpers/nocks');

require('chai').should();

describe('Index integration', () => {
    var app;

    before(() => {
        mockery.registerMock('express-bunker', () => function (req, res, next) {
            req.bunker = {};
            next();
        });
        mockery.enable({
            warnOnReplace: false,
            warnOnUnregistered: false,
            useCleanCache: true
        });
        app = require('../../app');
    });

    it('should response 200 when url is /faq', done => {
        request(app)
            .get(`${config.router.namespace}/faq`)
            .set('x-forwarded-for', ip)
            .set('Host', 'yandex.ru')
            .expect(200, done);
    });

    it('should response 404 when url is invalid', done => {
        nocks.nockExamCluster('invalidUrl');

        request(app)
            .get(`${config.router.namespace}/invalidUrl`)
            .set('x-forwarded-for', ip)
            .set('Host', 'yandex.ru')
            .expect(404, done);
    });
});
