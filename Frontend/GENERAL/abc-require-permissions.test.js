const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

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

describe('express/middlewares/abc-require-permissions', () => {
    let AbcRequirePermissions = require('./abc-require-permissions');
    let AskAbcPermissions;

    beforeEach(() => {
        // переимпортируем каждый раз, чтобы очистить кеш пермишенов
        AskAbcPermissions = require('./ask-abc-permissions');
    });

    it('Should throw 404 error without specified permissions', done => {
        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/api/frontend/permissions/', (req, res) => {
                    res.json({ results: ['can_do_something_unimportant', 'can_talk'] });
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

                app.use(AbcRequirePermissions.create({ permissions: ['can_talk', 'can_conquer_the_world'] }));

                // eslint-disable-next-line no-unused-vars,@typescript-eslint/no-unused-vars
                app.use((err, req, res, next) => {
                    res.sendStatus(err.status);
                });

                app.get('/', (req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/')
                    .expect(404)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should do nothing if client has required permissions', done => {
        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/api/frontend/permissions/', (req, res) => {
                    res.json({ results: ['can_do_something_unimportant', 'can_talk'] });
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

                app.use(AbcRequirePermissions.create({ permissions: ['can_talk'] }));

                // eslint-disable-next-line no-unused-vars,@typescript-eslint/no-unused-vars
                app.use((err, req, res, next) => {
                    res.sendStatus(err.status);
                });

                app.get('/', (req, res) => {
                    res.sendStatus(555);
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

    it('Should throw error with passed status', done => {
        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/api/frontend/permissions/', (req, res) => {
                    res.json({ results: ['can_work'] });
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

                app.use(AbcRequirePermissions.create({ permissions: ['can_dream'], errorStatus: 403 }));

                // eslint-disable-next-line no-unused-vars,@typescript-eslint/no-unused-vars
                app.use((err, req, res, next) => {
                    res.sendStatus(err.status);
                });

                app.get('/', (req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/')
                    .expect(403)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });
});
