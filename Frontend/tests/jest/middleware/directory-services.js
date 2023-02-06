'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const express = require('express');
const fakeBack = require('tools-access-express/tools/fake-back');
const {
    supertestWrapper: sw,
    mockAddLoggerToRequest,
} = require('../helpers');
const mw = require('../../../middleware/directory-services');

const servicesData = {
    services: [
        {
            url: '//connect.yandex.ru/portal/home',
            icon: 'https://yastatic.net/q/connect/portal-header-icons/dashboard.svg',
            slug: 'dashboard',
            name: 'Главная',
        },
    ],
};

let app;

describe('middleware/directory-services', () => {
    beforeEach(() => {
        app = express();

        app.use(mockAddLoggerToRequest);
    });

    it('Should provide `res.locals.directoryServices`', done => {
        fakeBack(function(back) {
            back.get('/ui/header/', (req, res) => {
                assert.deepEqual(req.query, {
                    tld: 'tld',
                    language: 'lang',
                });

                res.json(servicesData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = { tld: 'tld' };
                req.headers = { 'x-real-ip': 'real-ip' };
                req.cookies = {};
                res.locals.sessionid = {
                    uid: 'uid',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };
                req.tvm = {
                    tickets: {
                        directory: {
                            ticket: 'directory-service-ticket',
                        },
                    },
                };

                next();
            });

            app.use(mw({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname,
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.directoryServices, servicesData.services);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should pass TVM-tickets through http headers', done => {
        fakeBack(function(back) {
            const ticket = 'tvm-tiket';

            back.get('/ui/header/', (req, res) => {
                assert.strictEqual(req.headers['x-ya-service-ticket'], 'directory-service-ticket');
                assert.strictEqual(req.headers['x-ya-user-ticket'], ticket);

                res.json(servicesData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = { tld: 'tld' };
                req.headers = { 'x-real-ip': 'real-ip' };
                req.cookies = {};
                res.locals.sessionid = {
                    uid: 'uid',
                    lang: 'lang',
                    userTicket: ticket,
                };

                req.tvm = {
                    tickets: {
                        directory: {
                            ticket: 'directory-service-ticket',
                        },
                    },
                };

                next();
            });

            app.use(mw({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname,
                timeout: 100,
            }));

            app.use(function(req, res) {
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should pass the "X-Org-ID" header if a certain query parameter exists', done => {
        fakeBack(function(back) {
            back.get('/ui/header/', (req, res) => {
                assert.strictEqual(req.headers['x-org-id'], 'org-id');

                res.json(servicesData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = { tld: 'tld' };
                req.headers = { 'x-real-ip': 'real-ip' };
                req.cookies = {};
                res.locals.sessionid = {
                    uid: 'uid',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                req.tvm = {
                    tickets: {
                        directory: {
                            ticket: 'directory-service-ticket',
                        },
                    },
                };

                next();
            });

            app.use(mw({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname,
                timeout: 100,
            }));

            app.use(function(req, res) {
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .query({ org_id: 'org-id' })
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should handle a request error', done => {
        fakeBack(function(back) {
            back.get('/ui/header/', (req, res) => {
                setTimeout(() => {
                    res.json(servicesData);
                }, 10);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = { tld: 'tld' };
                req.headers = { 'x-real-ip': 'real-ip' };
                req.cookies = {};
                res.locals.sessionid = {
                    uid: 'uid',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };
                req.tvm = {
                    tickets: {
                        directory: {
                            ticket: 'directory-service-ticket',
                        },
                    },
                };

                next();
            });

            app.use(mw({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname,
                timeout: 9,
            }));

            // eslint-disable-next-line @typescript-eslint/no-unused-vars, no-unused-vars
            app.use(function(err, req, res, next) {
                assert(err instanceof Error);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    /*
        В этом случае мидлварь должна возвращать пустой [] вместо бросания ошибки.
        Дело в том, что эта мидлварь срабатывает раньше, чем запрос в бекенд.
        И если текущего пользователя нет в базе Директории, она вернет ошибку вместо списка сервисов.
        И запрос в бекенд уже не пойдет. А сами мы только из этого запроса мы можем узнать,
        что пользователя нет в директории и средиректить на заглушку.
    */
    it('Should handle an error from backend', done => {
        fakeBack(function(back) {
            back.get('/ui/header/', (req, res) => {
                res
                    .status(500)
                    .json({ message: 'Some error text' });
            });

            back.start();

            app.use((req, res, next) => {
                req.env = { tld: 'tld' };
                req.headers = { 'x-real-ip': 'real-ip' };
                req.cookies = {};
                res.locals.sessionid = {
                    uid: 'uid',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };
                req.tvm = {
                    tickets: {
                        directory: {
                            ticket: 'directory-service-ticket',
                        },
                    },
                };

                next();
            });

            app.use(mw({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname,
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert(Array.isArray(res.locals.directoryServices));
                assert.strictEqual(res.locals.directoryServices.length, 0);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should handle bad json from backend', done => {
        fakeBack(function(back) {
            back.get('/ui/header/', (req, res) => {
                res
                    .status(500)
                    .send('Some string');
            });

            back.start();

            app.use((req, res, next) => {
                req.env = { tld: 'tld' };
                req.headers = { 'x-real-ip': 'real-ip' };
                req.cookies = {};
                res.locals.sessionid = {
                    uid: 'uid',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };
                req.tvm = {
                    tickets: {
                        directory: {
                            ticket: 'directory-service-ticket',
                        },
                    },
                };

                next();
            });

            app.use(mw({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname,
                timeout: 100,
            }));

            // eslint-disable-next-line @typescript-eslint/no-unused-vars, no-unused-vars
            app.use(function(err, req, res, next) {
                assert.strictEqual(err.message, 'Unexpected token S in JSON at position 0');
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });
});
