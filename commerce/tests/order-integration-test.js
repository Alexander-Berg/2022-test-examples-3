'use strict';

const nock = require('nock');
const request = require('supertest');
const middlewareMock = require('./helper/middleware-mock');
const nockTvm = require('./helper/tvm');

describe('Order page', () => {
    let app;

    before(nockTvm);

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    after(nock.cleanAll);

    afterEach(middlewareMock.integrationAfter);

    it('should return 404 when page doesn`t exist', done => {
        request(app)
            .get('/adv/order/dfgdfhfghgfhfg')
            .set('host', 'yandex.ru')
            .expect(404, done);
    });
});
