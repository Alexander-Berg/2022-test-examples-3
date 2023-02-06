'use strict';

const config = require('yandex-cfg');
const mockery = require('mockery');
const nock = require('nock');
const request = require('supertest');
const url = require('url');

const middlewareMock = require('../helper/middleware-mock');
const mockBlackbox = require('../helper/blackbox');
const nockTvm = require('../helper/tvm');

describe('Partners access middleware', () => {
    const directApi = url.format(config.direct);
    const uid = 'userIdentifier';
    let app;

    beforeEach(() => {
        mockBlackbox({
            status: 'VALID',
            raw: {
                uid: {
                    value: uid
                }
            }
        });
        nockTvm();
    });

    afterEach(() => {
        mockery.deregisterAll();
        mockery.disable();
        nock.cleanAll();
    });

    it('should allow access if user has access', done => {
        middlewareMock.integrationBefore();

        mockery.enable({
            warnOnReplace: false,
            warnOnUnregistered: false,
            useCleanCache: true
        });

        app = require('../../server/app');

        nock(directApi)
            .get('')
            .query({ uid })
            .times(Infinity)
            .reply(200, { role: config.partnersAccessRoles[0] });

        request(app)
            .get('/adv/partners/materials')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                data.body.access.should.be.equal('allowed');
                done(err);
            });
    });

    it('should deny access when Direct API is unavailable', done => {
        [
            'uatraits',
            'yandexuid',
            'secretkey',
            'render',
            'realBunker',
            'secretkeyCheck'
        ]
            .forEach(middleware => middlewareMock[middleware]());

        mockery.registerMock('./middleware/partners-access', (req, _res, next) => {
            req.partnersAccess = 'forbidden';
            next();
        });

        mockery.enable({
            warnOnReplace: false,
            warnOnUnregistered: false,
            useCleanCache: true
        });

        app = require('../../server/app');

        nock(directApi)
            .get('')
            .query({ uid })
            .times(Infinity)
            .reply(418, '<p>&nbsp;</p>');

        request(app)
            .get('/adv/partners/materials')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                data.body.access.should.be.equal('forbidden');
                done(err);
            });
    });
});
