'use strict';

const request = require('supertest');

const app = require('../..').expressApp;

test('ping returns 200', () => {
    return new Promise(done => {
        request(app).get('/ping').expect(200, done);
    });
});

test('ping with query name=name returns "pong name"', () => {
    return new Promise(done => {
        request(app)
            .get('/ping?name=name')
            .expect(200)
            .then(response => {
                expect(response.body.result).toBe('pong name');
                done();
            });
    });
});
