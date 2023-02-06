const assert = require('assert');
const _ = require('lodash');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const fakeBack = require('../../../tools/fake-back');

describe('express/sets/service', () => {
    const Service = require('./service');

    it('Should load data if all conditions are true', done => {
        const spy = [];

        fakeBack(
            back => {
                back.get('/api/frontend/services/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '123');
                    let fields = req.query.fields.split(',').sort();

                    assert.ok(_.includes(fields, 'lang'));

                    spy.push(fields.includes('team'));

                    res.json({ content: { service: { contacts: [] } } });
                });

                back.get('/service-actions/defaults/', (req, res) => {
                    res.json({ content: {} });
                });

                back.get('/services/', (req, res) => {
                    res.json({ content: { services: 'foo' } });
                });

                back.get('/api/v4/roles/scopes/', (req, res) => {
                    res.json({ results: 'www' });
                });

                back.get('/api/frontend/services/team/:serviceId', (req, res) => {
                    res.json([]);

                    spy.push(true);
                });

                back.get('/api/frontend/scopes/counter/', (req, res) => {
                    res.json({ results: [] });
                });
            },
            (port, hostname, close) => {
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
                        service: {
                            fields: [
                                'team',
                                'scopes',
                                'lang',
                            ],
                        },
                        permissions: ['can_edit'],
                        location: {
                            query: {},
                        },
                    };
                    req.lang = 'lang';

                    next();
                });

                app.get('/services/:serviceId', Service.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    const service = res.locals.service;

                    assert.strictEqual(service.lang, 'lang');
                    assert.ok(service.fields);
                    assert.ok(service.scopes);
                    assert.ok(service.scopesCount);

                    assert.deepEqual(spy, [false, true]);

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

    it('Should not load data if all conditions false', done => {
        fakeBack(
            back => {
                back.get('/api/frontend/services/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '123');
                    res.json({ content: { service: {} } });
                });

                back.get('/service-actions/defaults/', (req, res) => {
                    res.json({ content: {} });
                });
            },
            (port, hostname, close) => {
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
                        service: { fields: [] },
                        location: { query: {} },
                        permissions: [],
                    };
                    req.lang = 'lang';

                    next();
                });

                app.get('/services/:serviceId', Service.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    const service = res.locals.service;

                    assert.ok(!service.lang);
                    assert.deepEqual(service.fields, []);
                    assert.ok(!service.scopes);

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
