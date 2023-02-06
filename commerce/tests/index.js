'use strict';
/* eslint-env mocha */
const request = require('supertest');
const app = require('../server/app');

it('static works', done => {
    request(app)
        .get('/static/desktop.bundles/index/_index.css')
        .expect(200, done);
});
