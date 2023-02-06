const assert = require('assert');

const supertest = require('supertest');
const express = require('express');

const cookieParser = require('cookie-parser');

describe('express/middlewares/abc-service-slug-redirect', () => {
    const AbcServiceSlugRedirect = require('./abc-service-slug-redirect');

    it('Should redirect to service/slug', done => {
        const app = express();

        app.use(cookieParser());

        app.get('/services/:serviceId/',
            (req, res, next) => {
                req.logger = {
                    child() {
                        return {
                            info() {},
                            debug() {},
                        };
                    },
                };
                res.locals = {
                    location: { pathname: req.path },
                    service: {
                        slug: '145',
                    },
                };
                next();
            },
            AbcServiceSlugRedirect.create());

        app.use((req, res) => {
            res.sendStatus(555);
        });

        supertest(app)
            .get('/services/666/')
            .set('cookie', 'Session_id=Sessid')
            .expect(302)
            .expect(res => {
                assert(res.header.location.includes('/145'));
            })
            .end(done);
    });

    it('Should not redirect if id not number', done => {
        const app = express();

        app.use(cookieParser());

        app.use((req, res, next) => {
            req.logger = {
                child() {
                    return {
                        info() {},
                        debug() {},
                    };
                },
            };
            res.locals = {
                location: { pathname: '/foo' },
                service: {
                    slug: '145',
                },
            };
            next();
        });

        app.get('/services/:serviceId/', AbcServiceSlugRedirect.create());

        app.use((req, res) => {
            res.sendStatus(555);
        });

        supertest(app)
            .get('/services/qwerty/')
            .set('cookie', 'Session_id=Sessid')
            .expect(555)
            .end(done);
    });
});
