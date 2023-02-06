const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-team,', () => {
    const AskAbcServiceTeam = require('./ask-abc-service-team');

    it('Should get service from backend with serviceId from req.params', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.get('/api/frontend/services/team/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '123');
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

                app.get('/services/:serviceId', AskAbcServiceTeam.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/services/123')
                    .set('cookie', 'Session_id=Sessid')
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

                back.get('/api/frontend/services/team/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '456');
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

                app.get('/services/:serviceId', AskAbcServiceTeam.create({
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

    describe('ask-abc-service/_formatKey_team', () => {
        it('Should format team actions', done => {
            fakeBack(
                back => {
                    const actions = [
                        'one',
                        'two',
                        'three',
                    ];
                    const team = [{
                        actions: actions,
                    }];

                    back.use(jsonParser());

                    back.get('/api/frontend/services/team/:serviceId/', (req, res) => {
                        res.json(team);
                    });
                },
                (port, hostname, close) => {
                    const app = express();

                    const expect = { one: {}, two: {}, three: {} };

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

                    app.get('/services/:serviceId', AskAbcServiceTeam.create({
                        port,
                        hostname,
                    }));

                    app.use((req, res) => {
                        assert.deepEqual(res.locals.service.team, [{ actions: expect }]);
                        res.sendStatus(555);
                    });

                    supertest(app)
                        .get('/services/123')
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
});
