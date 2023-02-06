const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;

const redirects = require('./redirects');

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

const app = express();

app.use(cookieParser());
app.use(jsonParser());
app.use(fakeLogger);

app.use(redirects);

app.get(/.*hardware/, (req, res) => {
    res.ok();
});

describe('express/dispenser/redirects', () => {
    it('should redirect from old "?edit=id" link', done => {
        supertest(app)
            .get('/service/service_slug/hardware?edit=42')
            .expect(302)
            .end((err, res) => {
                assert.equal(
                    res.headers.location,
                    '/hardware/42',
                    'Ошибка в редиректе старой страницы "?edit=id"',
                );
                done();
            });
    });

    it('should redirect from old "?view=consuming" link', done => {
        supertest(app)
            .get('/service/service_slug/hardware?view=consuming&rest=test')
            .expect(302)
            .end((err, res) => {
                assert.equal(
                    res.headers.location,
                    '?rest=test',
                    'Ошибка в редиректе старой страницы "view=consuming"',
                );
                done();
            });
    });

    it('should redirect from old "?view=supplying" link', done => {
        supertest(app)
            .get('/service/service_slug/hardware?view=supplying&rest=test')
            .expect(302)
            .end((err, res) => {
                assert.equal(
                    res.headers.location,
                    '?rest=test&showNested=true',
                    'Ошибка в редиректе старой страницы "view=supplying"',
                );
                done();
            });
    });

    it('should redirect from old "?view=all" link', done => {
        supertest(app)
            .get('/service/service_slug/hardware?view=all&rest=test')
            .expect(302)
            .end((err, res) => {
                assert.equal(
                    res.headers.location,
                    '/hardware/?rest=test',
                    'Ошибка в редиректе старой страницы "view=all"',
                );
                done();
            });
    });

    it('should redirect from old "?view=all" (without other params) link', done => {
        supertest(app)
            .get('/service/service_slug/hardware?view=all')
            .expect(302)
            .end((err, res) => {
                assert.equal(
                    res.headers.location,
                    '/hardware/',
                    'Ошибка в редиректе старой страницы "view=all" без других параметров',
                );
                done();
            });
    });

    it('should redirect from old "?resourcePreorderReasonType=type" link', done => {
        supertest(app)
            .get('/service/service_slug/hardware?resourcePreorderReasonType=type1&resourcePreorderReasonType=type2&rest=test')
            .expect(302)
            .end((err, res) => {
                assert.equal(
                    res.headers.location,
                    '?rest=test&reason=type1&reason=type2',
                    'Ошибка при переименовани параметра "resourcePreorderReasonType"',
                );
                done();
            });
    });

    it('should not redirect link without old params', done => {
        supertest(app)
            .get('/service/service_slug/hardware?rest=test&reason=type')
            .expect(200)
            .end(() => {
                done();
            });
    });
});
