'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const express = require('express');
const fakeBack = require('tools-access-express/tools/fake-back');
const {
    supertestWrapper: sw,
    mockAddLoggerToRequest,
} = require('../helpers');
const mw = require('../../../middleware/proxy-request');

let app;

describe('middleware/proxy-request', () => {
    beforeEach(() => {
        app = express();

        app.use(mockAddLoggerToRequest);
    });

    it('Should proxy request', done => {
        fakeBack(back => {
            back.get('/back-pathname/', (req, res) => {
                assert.strictEqual(req.query.foo, 'bar');

                res.json({ a: 1 });
            });

            back.start();

            app.use((req, res, next) => {
                res.locals = {
                    sessionid: { login: 'login' },
                    id: 'requestid',
                };
                req.cookies = {};
                req.env = {
                    query: {
                        foo: 'bar',
                        req_name: 'req-name',
                    },
                    config: {
                        backend: {
                            'req-name': {
                                protocol: 'http',
                                port: back.port,
                                hostname: back.hostname,
                                pathname: '/back-pathname',
                            },
                        },
                    },
                };

                next();
            });

            app.use(mw({}));

            sw(app)((test, closeServer) => {
                test.get('/')
                    .expect({ a: 1 })
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });
});
