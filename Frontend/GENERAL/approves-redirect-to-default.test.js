const assert = require('assert');

const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/approves-redirect-to-default', () => {
    const mw = require('./approves-redirect-to-default');

    it('Should redirect to default type', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                location: {},
            };
            next();
        });

        app.get('/approves/', mw.create());

        supertest(app)
            .get('/approves/')
            .expect(302)
            .expect(res => {
                assert(res.header.location.includes('/services-movement'));
            })
            .end(done);
    });
});
