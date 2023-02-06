'use strict';

const request = require('supertest');
const express = require('express');

require('./../../../express-patching');

describe('rejection should be handled automatically', () => {
    test('middleware without rejection', (done) => {
        const app = express();
        const errorMessage = 'Promise without rejection';

        async function middlewareWithoutRejection(req, res, next) {
            throw Error(errorMessage);
            next();
        }

        function errorHandlerMiddleware(err, req, res, next) {
            res.json({ error: err.message });
        }

        app.get('/test', [middlewareWithoutRejection, errorHandlerMiddleware]);

        request(app)
            .get('/test')
            .expect(200)
            .expect('Content-Type', /json/)
            .end((err, res) => {
                expect(res.body.error).toEqual(errorMessage);
                done();
            });
    });
});
