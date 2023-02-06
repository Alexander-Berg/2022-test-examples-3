const assert = require('assert');
const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/not-found', function() {
    const NotFound = require('./not-found');

    it('Should provide an error "Not found" with proper status', done => {
        const app = express();

        app.use(NotFound.create());

        // error-handling functions have four arguments
        app.use((err, req, res, _next) => {
            assert.strictEqual(err.message, 'Not found');
            assert.strictEqual(err.status, '404');

            res.sendStatus(555);
        });

        supertest(app)
            .get('/not-exist')
            .expect(555)
            .end(done);
    });
});
