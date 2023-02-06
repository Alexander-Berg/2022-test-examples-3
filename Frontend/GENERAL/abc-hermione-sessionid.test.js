const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;

const fakeLogger = (req, res, next) => {
    req.logger = {
        child() {
            return {
                info() {},
                debug() {},
            };
        },
    };
    next();
};

describe('express/middlewares/abc-hermione-sessionid', () => {
    const AbcHermioneSessionid = require('./abc-hermione-sessionid');

    const TEST_LOGIN = 'test_login';

    it('Should set sessionid.login if hermione_user cookie presented ', done => {
        const app = express();

        app.use(cookieParser());
        app.use(jsonParser());
        app.use(fakeLogger);

        app.use((req, res, next) => {
            req.cookies.hermione_user = TEST_LOGIN;
            next();
        });

        app.use(AbcHermioneSessionid.create());

        app.use((req, res) => {
            const { login } = res.locals.sessionid;
            assert.strictEqual(login, TEST_LOGIN);

            res.sendStatus(555);
        });

        app.get('/', (req, res) => {
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end((...args) => {
                done(...args);
            });
    });
});
