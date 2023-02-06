'use strict';

/* eslint-disable max-nested-callbacks */

const express = require('express');
const fakeBack = require('tools-access-express/tools/fake-back');
const {
    supertestWrapper: sw,
    mockAddLoggerToRequest,
} = require('../helpers');
const mw = require('../../../middleware/ping-backend');

let app;

describe('middleware/ping-backend', () => {
    beforeEach(() => {
        app = express();

        app.use(mockAddLoggerToRequest);
    });

    it('Should response with status code from backend', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/ping', (req, res) => {
                res.sendStatus(200);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = { tld: 'ru' };
                res.locals = {
                    id: 'requestid',
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

    it('Should response with status code 500 if a request error occured', done => {
        fakeBack(function(back) {
            back.get('/_abovemeta/ping', (req, res) => {
                setTimeout(() => {
                    res.sendStatus(200);
                }, 10);
            });

            back.start();

            app.use((req, res, next) => {
                req.env = { tld: 'ru' };

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

            sw(app)((test, closeServer) => {
                test.get('/')
                    .expect(500)
                    .end(function() {
                        back.close();
                        closeServer();
                        done.apply(this, arguments);
                    });
            });
        });
    });
});
