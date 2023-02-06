const assert = require('assert');

const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/location', () => {
    const Location = require('./location');

    it('Should add res.locals.location with proto from x-client-protocol', done => {
        const app = express();

        app.use(Location.create());

        app.get('/', (req, res) => {
            assert.strictEqual(res.locals.location.protocol, '123:');
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .set('x-client-protocol', '123')
            .expect(555)
            .end(done);
    });

    it('Should add res.locals.location with proto from req.protocol', done => {
        const app = express();

        app.use(Location.create());

        app.get('/', (req, res) => {
            assert.strictEqual(res.locals.location.protocol, req.protocol + ':');
            assert.strictEqual(res.locals.location.hostname, req.hostname);
            assert.strictEqual(res.locals.location.pathname, req.originalUrl);
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });

    it('Should not add to res.locals.location search, host and path', done => {
        const app = express();

        app.use(Location.create());

        app.get('/', (req, res) => {
            assert.ok(!res.locals.location.search);
            assert.ok(!res.locals.location.host);
            assert.ok(!res.locals.location.path);
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });
});
