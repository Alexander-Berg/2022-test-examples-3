'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const path = require('path');
const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/html');

describe('middleware/html', () => {
    it('Should response with an html-page', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.sessionid = { lang: 'ru' };
            res.locals.searchResults = {
                meta: {
                    count: 1,
                    ab_info: {
                        buckets: null,
                        crypted_buckets: null,
                    },
                },
            };
            req.env = {
                config: { isB2b: false },
                query: {},
            };

            next();
        });

        app.use(mw({
            bemhtmlPathes: { ru: path.join(__dirname, '../fixtures/middleware-html-1') },
        }));

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect('all good')
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should response with an html-page (hermione)', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.sessionid = { lang: 'ru' };
            res.locals.searchResults = {
                meta: {
                    count: 1,
                    ab_info: {
                        buckets: null,
                        crypted_buckets: null,
                    },
                },
            };
            req.env = {
                config: { isB2b: false, env: 'hermione' },
                query: {},
            };

            next();
        });

        app.use(mw({
            bemhtmlPathes: { ru: path.join(__dirname, '../fixtures/middleware-html-1') },
        }));

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect('all good')
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should handle a rendering error', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.sessionid = { lang: 'ru' };
            res.locals.searchResults = {
                meta: {
                    count: 1,
                },
            };
            req.env = {
                query: {},
            };

            next();
        });

        app.use(mw({
            bemhtmlPathes: { ru: 'invalid path' },
        }));

        // eslint-disable-next-line @typescript-eslint/no-unused-vars, no-unused-vars
        app.use((err, req, res, next) => {
            assert.strictEqual(err.code, 'MODULE_NOT_FOUND');
            res.sendStatus(200);
        });

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect(200)
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should show empty results', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.sessionid = { lang: 'ru' };
            res.locals.searchResults = {
                meta: {
                    count: 0,
                    ab_info: {
                        buckets: null,
                        crypted_buckets: null,
                    },
                },
            };
            req.env = {
                config: { isB2b: false },
                query: {},
            };

            next();
        });

        app.use(mw({
            bemhtmlPathes: { ru: path.join(__dirname, '../fixtures/middleware-html-1') },
        }));

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect('all good')
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });
});
