const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;

const ExpFlagsCookies = require('./abc-add-exp-cookie');

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

describe('express/middlewares/abc-add-exp-cookie', () => {
    const expFlag = 'exp_flags=test';
    const app = express();

    app.use(cookieParser());
    app.use(jsonParser());
    app.use(fakeLogger);

    app.use(ExpFlagsCookies.create());

    app.use((_, res) => {
        res.sendStatus(555);
    });

    it('Should not set cookies for regular queries', done => {
        supertest(app)
            .get('/?page=1')
            .expect(555)
            .end((err, result) => {
                if (err) done(err);

                expect(result.res.headers['set-cookie']).toBeUndefined();
                done();
            });
    });

    it('Should set exp flags cookies', done => {
        supertest(app)
            .get(`/?${expFlag}`)
            .expect(555)
            .end((err, result) => {
                if (err) done(err);

                const cookieHeaders = result.res.headers['set-cookie'];
                expect(cookieHeaders.some(c => c.includes(expFlag))).toBeTruthy();
                done();
            });
    });
});
