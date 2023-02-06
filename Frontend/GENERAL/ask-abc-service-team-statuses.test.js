const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-team,', () => {
    const AskAbcServiceTeamStatuses = require('./ask-abc-service-team-statuses');

    it('Should get service from backend with serviceId from req.params', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.get('/api/frontend/services/team_statuses/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '123');
                    res.json({
                        active: {
                            persons: [{ actions: [] }],
                            departments: [{ actions: [] }],
                        },
                        inactive: {
                            persons: [{ actions: [] }],
                            departments: [{ actions: [] }],
                        },
                    });
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

                app.get('/services/:serviceId', AskAbcServiceTeamStatuses.create({
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

                back.get('/api/frontend/services/team_statuses/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '456');
                    res.json({
                        active: {
                            persons: [{ actions: [] }],
                            departments: [{ actions: [] }],
                        },
                        inactive: {
                            persons: [{ actions: [] }],
                            departments: [{ actions: [] }],
                        },
                    });
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

                app.get('/services/:serviceId', AskAbcServiceTeamStatuses.create({
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

    describe('ask-abc-service/_formatKey_team_statuses', () => {
        it('Should format team_statuses actions', done => {
            fakeBack(
                back => {
                    back.use(jsonParser());

                    back.get('/api/frontend/services/team_statuses/:serviceId/', (req, res) => {
                        res.json({
                            active: {
                                persons: [
                                    {
                                        actions: ['foo', 'bar'],
                                    },
                                ],
                                departments: [
                                    {
                                        actions: ['bar', 'baz'],
                                    },
                                ],
                            },
                            inactive: {
                                persons: [
                                    {
                                        actions: ['foo1', 'bar1'],
                                    },
                                ],
                                departments: [
                                    {
                                        actions: ['bar1', 'baz1'],
                                    },
                                ],
                            },
                        });
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

                    app.get('/services/:serviceId', AskAbcServiceTeamStatuses.create({
                        port,
                        hostname,
                    }));

                    app.use((req, res) => {
                        assert.deepEqual(res.locals.service.team_statuses, {
                            active: {
                                persons: [
                                    {
                                        actions: {
                                            foo: {},
                                            bar: {},
                                        },
                                    },
                                ],
                                departments: [
                                    {
                                        actions: {
                                            bar: {},
                                            baz: {},
                                        },
                                    },
                                ],
                            },
                            inactive: {
                                persons: [
                                    {
                                        actions: {
                                            foo1: {},
                                            bar1: {},
                                        },
                                    },
                                ],
                                departments: [
                                    {
                                        actions: {
                                            bar1: {},
                                            baz1: {},
                                        },
                                    },
                                ],
                            },
                        });
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
