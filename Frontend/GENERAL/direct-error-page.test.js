const assert = require('assert');
const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/direct-error-page', () => {
    const DirectErrorPage = require('./direct-error-page');

    it('Should provide an error with proper status', done => {
        const app = express();

        app.use('/error/:status', DirectErrorPage.create());

        // error-handling functions have four arguments
        app.use((err, req, res, _next) => {
            assert.strictEqual(err.message, 'Direct request for error page');

            res.sendStatus(err.status);
        });

        supertest(app)
            .get('/error/555')
            .expect(555)
            .end(done);
    });
});
