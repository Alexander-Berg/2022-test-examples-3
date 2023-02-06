const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

const TEST_PERMISSIONS = [
    'view_kpi', 'view_resources', 'view_profile', 'view_tags', 'view_traffic_light',
    'can_edit', 'can_export', 'view_hardware', 'view_hierarchy', 'view_duty', 'can_view',
    'view_team', 'view_own_services', 'view_all_services', 'view_details',
];

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

describe('express/middlewares/ask-abc-permissions', () => {
    it('Should set empty array to locals.permissions without sessionid', done => {
        const AskAbcPermissions = require('./ask-abc-permissions');

        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/api/frontend/permissions/', (req, res) => {
                    res.json({ results: TEST_PERMISSIONS });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());
                app.use(fakeLogger);

                app.use(AskAbcPermissions.create({
                    port,
                    hostname,
                }));

                app.get('/', (req, res) => {
                    assert.deepEqual(res.locals.permissions, []);
                    res.send(555);
                });

                supertest(app)
                    .get('/')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should set locals permissions from backend', done => {
        const AskAbcPermissions = require('./ask-abc-permissions');

        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/api/frontend/permissions/', (req, res) => {
                    res.json({ results: TEST_PERMISSIONS });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());
                app.use(fakeLogger);
                app.use((req, res, next) => {
                    res.locals.sessionid = { login: 'usertest' };
                    next();
                });

                app.use(AskAbcPermissions.create({
                    port,
                    hostname,
                }));

                app.get('/', (req, res) => {
                    assert.deepEqual(res.locals.permissions, TEST_PERMISSIONS);
                    res.send(555);
                });

                supertest(app)
                    .get('/')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should cache backend response', done => {
        const AskAbcPermissions = require('./ask-abc-permissions');

        let backendRequests = 0;

        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/api/frontend/permissions/', (req, res) => {
                    backendRequests++;
                    res.json({ results: TEST_PERMISSIONS });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());
                app.use(fakeLogger);
                app.use((req, res, next) => {
                    res.locals.sessionid = { login: 'usertest-another' };
                    next();
                });

                app.use(AskAbcPermissions.create({
                    port,
                    hostname,
                }));

                app.get('/', (req, res) => {
                    assert.deepEqual(res.locals.permissions, TEST_PERMISSIONS);
                    res.send(555);
                });

                const request = supertest(app);

                request
                    .get('/')
                    .expect(555)
                    .end(() => {
                        assert.equal(backendRequests, 1);
                        request
                            .get('/')
                            .expect(555)
                            .end((...args) => {
                                assert.equal(backendRequests, 1);
                                close();
                                done(...args);
                            });
                    });
            },
        );
    });

    it('Should not cache backend response in hermione tests', done => {
        process.env.NODE_ENV = 'hermione';
        const AskAbcPermissions = require('./ask-abc-permissions');

        let backendRequests = 0;

        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/api/frontend/permissions/', (req, res) => {
                    backendRequests++;
                    res.json({ results: TEST_PERMISSIONS });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());
                app.use(fakeLogger);
                app.use((req, res, next) => {
                    res.locals.sessionid = { login: 'usertest-another' };
                    next();
                });

                app.use(AskAbcPermissions.create({
                    port,
                    hostname,
                }));

                app.get('/', (req, res) => {
                    assert.deepEqual(res.locals.permissions, TEST_PERMISSIONS);
                    res.send(555);
                });

                const request = supertest(app);

                request
                    .get('/')
                    .expect(555)
                    .end(() => {
                        assert.equal(backendRequests, 1);
                        request
                            .get('/')
                            .expect(555)
                            .end((...args) => {
                                assert.equal(backendRequests, 2);
                                close();
                                done(...args);
                            });
                    });
            },
        );
    });
});
