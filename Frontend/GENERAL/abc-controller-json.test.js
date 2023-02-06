const express = require('express');
const supertest = require('supertest');

describe('express/middlewares/abc-controller-json', () => {
    const AbcControllerJson = require('./abc-controller-json');

    it('Should respond with json', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.test = { foo: 'bar' };
            next();
        });

        app.use(AbcControllerJson.create({
            symbols: 'test',
        }));

        supertest(app)
            .get('/?__mode=json')
            .expect('Content-Type', /^application\/json/)
            .expect('{"foo":"bar"}')
            .expect(200)
            .end(done);
    });

    it('Should respond with formatted json', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.test = { foo: 'bar' };
            res.locals.error = { bar: 'quux' };
            next();
        });

        app.use(AbcControllerJson.create({
            symbols: {
                test: 'test',
                error: 'error',
            },
        }));

        supertest(app)
            .get('/?__mode=json')
            .expect('Content-Type', /^application\/json/)
            .expect('{"test":{"foo":"bar"},"error":{"bar":"quux"}}')
            .expect(200)
            .end(done);
    });
});
