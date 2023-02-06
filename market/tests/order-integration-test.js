'use strict';

/* global describe, it, afterEach, beforeEach, require */
const request = require('supertest');
const middlewareMock = require('./helper/middleware-mock');

describe('Order page', () => {
    let app;

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    it('should return 404 when page doesn`t exist', done => {
        request(app)
            .get('/adv/order/dfgdfhfghgfhfg')
            .set('host', 'yandex.ru')
            .expect(404, done);
    });

    afterEach(middlewareMock.integrationAfter);
});
