'use strict';

/* global describe, it, afterEach, before, require */
const request = require('supertest');
const { expect } = require('chai');
const middlewareMock = require('./helper/middleware-mock');
const config = require('yandex-cfg');
const nock = require('nock');
const url = require('url');
const mockery = require('mockery');

describe('Partners', () => {
    let app;
    const direct = {
        host: url.format({
            protocol: config.direct.protocol,
            hostname: config.direct.hostname,
            port: config.direct.port || null
        }),
        path: `/${config.direct.pathname}`
    };

    before(() => {
        nock(direct.host)
            .get(direct.path)
            .query({
                uid: '10001'
            })
            .times(Infinity)
            .reply(200, {
                role: 'client'
            });

        nock(direct.host)
            .get(direct.path)
            .query({
                uid: '10002'
            })
            .times(Infinity)
            .reply(200, {
                role: 'agency'
            });

        nock(direct.host)
            .get(direct.path)
            .query({
                uid: '10003'
            })
            .times(Infinity)
            .reply(200, {
                role: 'support'
            });
    });

    it('should set access to "forbidden" for unauthorized user', done => {
        mockery.registerMock('express-blackbox', () => {
            return (req, res, next) => {
                req.blackbox = { status: 'INVALID' };
                next();
            };
        });
        middlewareMock.integrationBefore();
        app = require('../server/app');

        request(app)
            .get('/adv/partners')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                expect(data.body.access).to.equal('forbidden');
                done(err);
            });
    });

    it('should set access to "invalid" for not-agency', done => {
        mockery.registerMock('express-blackbox', () => {
            return (req, res, next) => {
                req.blackbox = {
                    status: 'VALID',
                    raw: { uid: { value: '10001' } }
                };
                next();
            };
        });
        middlewareMock.integrationBefore();
        app = require('../server/app');

        request(app)
            .get('/adv/partners')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                expect(data.body.role).to.equal('client');
                expect(data.body.access).to.equal('invalid');
                done(err);
            });
    });

    it('should set access to "allowed" for agency', done => {
        mockery.registerMock('express-blackbox', () => {
            return (req, res, next) => {
                req.blackbox = {
                    status: 'VALID',
                    raw: { uid: { value: '10002' } }
                };
                next();
            };
        });
        middlewareMock.integrationBefore();
        app = require('../server/app');

        request(app)
            .get('/adv/partners/request')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                expect(data.body.role).to.equal('agency');
                expect(data.body.access).to.equal('allowed');
                done(err);
            });
    });

    it('should set access to "allowed" for support', done => {
        mockery.registerMock('express-blackbox', () => {
            return (req, res, next) => {
                req.blackbox = {
                    status: 'VALID',
                    raw: { uid: { value: '10003' } }
                };
                next();
            };
        });
        middlewareMock.integrationBefore();
        app = require('../server/app');

        request(app)
            .get('/adv/partners/request')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                expect(data.body.role).to.equal('support');
                expect(data.body.access).to.equal('allowed');
                done(err);
            });
    });

    afterEach(middlewareMock.integrationAfter);
});
