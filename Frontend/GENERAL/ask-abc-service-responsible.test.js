const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-responsible,', () => {
    const AskAbcServiceResponsible = require('./ask-abc-service-responsible');

    it('Should get service from backend with serviceId from req.query', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.get('/api/frontend/services/responsibles/', (req, res) => {
                    assert.strictEqual(req.query.service, '123');
                    res.json([]);
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());

                app.use((req, res, next) => {
                    req.logger = {
                        child() {
                            return {
                                info() {},
                                debug() {},
                            };
                        },
                    };
                    next();
                });

                app.get('/services/:serviceId/', AskAbcServiceResponsible.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/services/123/')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should get service from backend with serviceId from query', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.get('/api/frontend/services/responsibles/', (req, res) => {
                    assert.strictEqual(req.query.service, '456');
                    res.json([]);
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());

                app.use((req, res, next) => {
                    req.logger = {
                        child() {
                            return {
                                info() {},
                                debug() {},
                            };
                        },
                    };
                    next();
                });

                app.get('/services/:serviceId', AskAbcServiceResponsible.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/services/123?target=456')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });
});
