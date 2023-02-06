const express = require('express');
const supertest = require('supertest');
const gzip = require('./gzip');

describe('express/middlewares/gzip', () => {
    it('Should gzip response', () => {
        const app = express();

        app.use(gzip({ threshold: 0 }));
        app.use((req, res) => {
            res.json({ foo: 'bar' });
        });

        return supertest(app)
            .get('/')
            .set('Accept-Encoding', 'gzip')
            .expect(200)
            .expect('Vary', 'Accept-Encoding')
            .expect('Content-Encoding', 'gzip');
    });
});
