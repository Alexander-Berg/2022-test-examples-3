'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const express = require('express');
const fakeBack = require('tools-access-express/tools/fake-back');
const {
    supertestWrapper: sw,
    mockAddLoggerToRequest,
} = require('../helpers');
const mw = require('../../../middleware/search-results');

const searchResultsData = { foo: 'bar', meta: {} };
const searchResultsExpected = {
    foo: 'bar',
    meta: {
        ab_info: {
            buckets: null,
            crypted_buckets: null,
        },
    },
};

let app;

describe('middleware/search-results', () => {
    beforeEach(() => {
        app = express();

        app.use(mockAddLoggerToRequest);
    });

    it('Should provide `res.locals.searchResults` for intranet env', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query.text, 'text');
                assert.strictEqual(req.query.scope, 'scopenamesearch');
                assert.strictEqual(req.query.user, 'login');
                assert.strictEqual(req.query.language, 'lang');
                assert.strictEqual(req.query.format, 'json');
                assert(req.query.request_id);
                assert.strictEqual(req.headers['x-ya-user-ticket'], 'tvm-tiket');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals = {
                    sessionid: {
                        login: 'login',
                        lang: 'lang',
                        userTicket: 'tvm-tiket',
                    },
                    id: 'requestid',
                };

                next();
            });

            app.get(/^\/([a-z]*)search$/, mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ text: 'text' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should send the X-Forwarded-For header to the backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.headers['x-forwarded-for'], '127001');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.get(/^\/([a-z]*)search$/, mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ text: 'text' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should send the X-Ya-Service-Ticket header to the backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.headers['x-ya-service-ticket'], 'abovemeta-service-ticket');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.get(/^\/([a-z]*)search$/, mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ text: 'text' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should not send the X-Forwarded-For header to the backend in case of dev env', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert(!req.headers.hasOwnProperty('x-forwarded-for'));

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {
                        env: 'dev',
                    },
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.get(/^\/([a-z]*)search$/, mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ text: 'text' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should provide `res.locals.searchResults` for b2b env', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query.text, 'text');
                assert.strictEqual(req.query.scope, 'scopenamesearch');
                assert.strictEqual(req.query.user, 'login');
                assert.strictEqual(req.query.language, 'lang');
                assert.strictEqual(req.query.format, 'json');
                assert(req.query.request_id);
                assert.strictEqual(req.headers['x-ya-user-ticket'], 'tvm-tiket');
                assert.strictEqual(req.headers['x-forwarded-for'], '127001');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: { isB2b: true },
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals = {
                    sessionid: {
                        login: 'login',
                        lang: 'lang',
                        userTicket: 'tvm-tiket',
                    },
                    id: 'requestid',
                };

                next();
            });

            app.get('/:scope', mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ text: 'text' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should pass facets query params to backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query['facet.foo'], 'bar');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ 'facet.foo': 'bar' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should pass p query param to backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query.p, 'bar');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ p: 'bar' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should pass sorted query param to backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query.sorted, 'bar');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ sorted: 'bar' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should pass nomisspell query param to backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query.nomisspell, '1');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ nomisspell: 1 })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should pass features query params to backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query['feature.foo'], 'bar');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ 'feature.foo': 'bar' })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
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
            back.get('/_abovemeta/', (req, res) => {
                setTimeout(() => {
                    res.json(searchResultsData);
                }, 10);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 9,
            }));

            // eslint-disable-next-line @typescript-eslint/no-unused-vars, no-unused-vars
            app.use(function(err, req, res, next) {
                assert(err instanceof Error);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should handle an error from backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                res
                    .status(500)
                    .json({ message: 'Some error text' });
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            // eslint-disable-next-line @typescript-eslint/no-unused-vars, no-unused-vars
            app.use(function(err, req, res, next) {
                assert.strictEqual(err.message, 'Backend responded with 500: Internal Server Error');
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
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
            back.get('/_abovemeta/', (req, res) => {
                res.send('Some string');
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            // eslint-disable-next-line @typescript-eslint/no-unused-vars, no-unused-vars
            app.use(function(err, req, res, next) {
                assert.strictEqual(err.message, 'Unexpected token S in JSON at position 0');
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/')
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should get scope from proper place for format-json in b2b env', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query.scope, 'scopename');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: { isB2b: true },
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.get('/:scope', mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopename')
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should accept 4XX codes from backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                res
                    .status(400)
                    .json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: { isB2b: true },
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopename')
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
                    .expect(200)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });

    it('Should pass a debug query param to backend if plainjson=1 requested', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/', (req, res) => {
                assert.strictEqual(req.query.debug, '1');

                res.json(searchResultsData);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = {
                    parsedUrl: {
                        pathname: req.path,
                    },
                    config: {},
                    tld: 'ru',
                };
                req.tvm = {
                    tickets: {
                        abovemeta: {
                            ticket: 'abovemeta-service-ticket',
                        },
                    },
                };
                req.cookies = {};
                res.locals.sessionid = {
                    login: 'login',
                    lang: 'lang',
                    userTicket: 'tvm-tiket',
                };

                next();
            });

            app.use(mw({
                reqData: {
                    protocol: 'http',
                    port: back.port,
                    ru: {
                        hostname: back.hostname,
                    },
                },
                timeout: 100,
            }));

            app.use(function(req, res) {
                assert.deepEqual(res.locals.searchResults, searchResultsExpected);
                res.sendStatus(200);
            });

            sw(app)((test, closeServer) => {
                test.get('/scopenamesearch')
                    .query({ plainjson: 1 })
                    .set('x-real-ip', '127001')
                    .set('x-forwarded-for', '127001')
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
