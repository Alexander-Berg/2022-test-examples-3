const assert = require('assert');
const express = require('express');
const supertest = require('supertest');

describe('express/middlewares/error-page', () => {
    const errorPage = require('./error-page');

    it('Should handle error', done => {
        const app = express();
        const error = new Error('EROR');

        app.use((req, res, next) => {
            req.logger = {
                error() {},
            };

            next(error);
        });

        app.use(errorPage());

        app.use((req, res) => {
            assert.strictEqual(res.locals.error, error);
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });

    it('Should send err.data if req.isAjax', done => {
        const app = express();
        const error = new Error('EROR');

        error.status = 400;
        error.data = { foo: 42 };

        app.use((req, res, next) => {
            req.logger = {
                error() {},
            };

            req.isAjax = true;

            next(error);
        });

        app.use(errorPage());

        supertest(app)
            .get('/')
            .expect(400)
            .expect('{"foo":42}')
            .end(done);
    });

    it('Should send 500 on incorrect status', done => {
        const app = express();
        const error = new Error('EROR');

        error.status = 0;
        error.data = { foo: 42 };

        app.use((req, res, next) => {
            req.logger = {
                error() {},
            };

            req.isAjax = true;

            next(error);
        });

        app.use(errorPage());

        supertest(app)
            .get('/')
            .expect(500)
            .end(done);
    });
});
