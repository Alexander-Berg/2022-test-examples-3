const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service', () => {
    const AskAbcService = require('./ask-abc-service');

    it('Should get service from backend with serviceId from req.params', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.get('/api/frontend/services/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '123');
                    assert.strictEqual(req.query.fields, 'fields1,fields2,id');
                    res.json({ content: { service: 'nope' } });
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
                    res.locals = {
                        service: { fields: ['fields1', 'fields2'] },
                    };
                    next();
                });

                app.get('/services/:serviceId', AskAbcService.create({
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

                back.get('/api/frontend/services/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '456');
                    assert.strictEqual(req.query.fields, 'fields1,fields2,id');
                    res.json({ content: { service: 'nope' } });
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
                    res.locals = {
                        service: { fields: ['fields1', 'fields2'] },
                    };
                    next();
                });

                app.get('/services/:serviceId', AskAbcService.create({
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

    describe('ask-abc-service/_formatKey_actions', () => {
        it('Should format actions', done => {
            fakeBack(
                back => {
                    back.use(jsonParser());

                    back.get('/api/frontend/services/:serviceId/', (req, res) => {
                        res.json({
                            actions: [
                                { action1: { v: 'one' } },
                                { action2: { v: 'two' } },
                                { action2: { v: 'three' } },
                            ],
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
                        res.locals = {
                            service: { fields: ['fields1', 'fields2'] },
                        };
                        next();
                    });

                    app.get('/services/:serviceId', AskAbcService.create({
                        port,
                        hostname,
                    }));

                    app.use((req, res) => {
                        assert.deepEqual(res.locals.service.actions, {
                            action1: { v: 'one' },
                            action2: { v: 'three' },
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
    describe('ask-abc-service/_formatKey_departments', () => {
        it('Should format departments actions with members', done => {
            fakeBack(
                back => {
                    const actions = [
                        { action1: { v: 'one' } },
                        { action2: { v: 'two' } },
                        { action2: { v: 'three' } },
                    ];
                    const departments = [{
                        membersCount: 100500,
                        actions: actions,
                    }];

                    back.use(jsonParser());

                    back.get('/api/frontend/services/:serviceId/', (req, res) => {
                        res.json({ departments: departments });
                    });
                },
                (port, hostname, close) => {
                    const app = express();

                    const expect = { action1: { v: 'one' }, action2: { v: 'three' } };

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

                    app.get('/services/:serviceId', AskAbcService.create({
                        port,
                        hostname,
                    }));

                    app.use((req, res) => {
                        assert.deepEqual(res.locals.service.departments, [{ membersCount: 100500, actions: expect }]);
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
    describe('ask-abc-service/_formatKey_requested', () => {
        it('Should format departments and members actions', done => {
            fakeBack(
                back => {
                    const actions = [
                        { action1: { v: 'one' } },
                        { action2: { v: 'two' } },
                        { action2: { v: 'three' } },
                    ];
                    const actions2 = [
                        { action3: { v: 'three' } },
                        { action4: { v: 'four' } },
                        { action4: { v: 'five' } },
                    ];
                    const departments = [{
                        actions: actions,
                    }];
                    const members = [{
                        actions: actions2,
                    }];

                    back.use(jsonParser());

                    back.get('/api/frontend/services/:serviceId/', (req, res) => {
                        res.json({
                            requested: {
                                departments: departments,
                                persons: members,
                            },
                        });
                    });
                },
                (port, hostname, close) => {
                    const app = express();
                    const expect = { action1: { v: 'one' }, action2: { v: 'three' } };
                    const expect2 = { action3: { v: 'three' }, action4: { v: 'five' } };

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

                    app.get('/services/:serviceId', AskAbcService.create({
                        port,
                        hostname,
                    }));

                    app.use((req, res) => {
                        assert.deepEqual(res.locals.service.requested, {
                            departments: [{ actions: expect }],
                            persons: [{ actions: expect2 }],
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

    describe('ask-abc-service/_formatKey_responsible', () => {
        it('Should format responsible actions', done => {
            fakeBack(
                back => {
                    const actions = [
                        'one',
                        'two',
                        'three',
                    ];
                    const responsible = [{
                        actions: actions,
                    }];

                    back.use(jsonParser());

                    back.get('/api/frontend/services/:serviceId/', (req, res) => {
                        res.json({ responsible: responsible });
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

                    app.get('/services/:serviceId', AskAbcService.create({
                        port,
                        hostname,
                    }));

                    app.use((req, res) => {
                        assert.deepEqual(res.locals.service.responsible, [{ actions: expect }]);
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
