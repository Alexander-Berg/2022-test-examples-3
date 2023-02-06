const assert = require('assert');

const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/approves-type', () => {
    const mw = require('./approves-type');

    it('Should provide type', done => {
        const app = express();

        app.get('/approves/:type/', mw.create());

        app.use((req, res) => {
            assert.strictEqual(res.locals.type, '777');
            res.sendStatus(555);
        });

        supertest(app)
            .get('/approves/777')
            .end(done);
    });
});
