'use strict';

/* global describe, it, afterEach, beforeEach, require */
const nock = require('nock');
const request = require('supertest');
const middlewareMock = require('./helper/middleware-mock');
const nockTvm = require('./helper/tvm');

describe('Partners request form', () => {
    let app;

    before(nockTvm);

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    after(nock.cleanAll);

    afterEach(middlewareMock.integrationAfter);

    // ADVDEV-1729: Редиректы для partners/request
    it('should return 301 redirect', done => {
        request(app)
            .get('/adv/partners/request')
            .set('host', 'yandex.ru')
            .expect(301)
            .end(err => {
                done(err);
            });
    });
});
