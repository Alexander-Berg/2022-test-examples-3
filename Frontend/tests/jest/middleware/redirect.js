'use strict';

/* eslint-disable max-nested-callbacks */

const express = require('express');
const {
    supertestWrapper: sw,
    mockAddLoggerToRequest,
} = require('../helpers');
const mw = require('../../../middleware/redirect');

let app;

describe('middleware/redirect', () => {
    beforeEach(() => {
        app = express();

        app.use(mockAddLoggerToRequest);
    });

    it('Should redirect to /search if REDIRECT_UNKNOWN_SCOPE', done => {
        app.use((req, res, next) => {
            res.locals.searchResults = {
                errors: [
                    {
                        code: 'REDIRECT_UNKNOWN_SCOPE',
                        type: 'redirect',
                        message: 'Редиректик',
                    },
                ],
            };

            req.env = {
                config: {
                    backend: {
                        connect: {},
                    },
                },
            };

            next();
        });

        app.use(mw());

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect(302)
                .expect('Location', 'https://127.0.0.1/search')
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should redirect to /search/directory if REDIRECT_UNKNOWN_SCOPE and b2b environment', done => {
        app.use((req, res, next) => {
            res.locals.searchResults = {
                errors: [
                    {
                        code: 'REDIRECT_UNKNOWN_SCOPE',
                        type: 'redirect',
                        message: 'Редиректик',
                    },
                ],
            };

            req.env = {
                config: {
                    isB2b: true,
                    backend: {
                        connect: {},
                    },
                },
            };

            next();
        });

        app.use(mw());

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect(302)
                .expect('Location', 'https://127.0.0.1/search/directory')
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should redirect to connect.tld/forbidden/ if REDIRECT_USER_ORGANIZATION', done => {
        app.use((req, res, next) => {
            res.locals.searchResults = {
                errors: [
                    {
                        code: 'REDIRECT_USER_ORGANIZATION',
                        type: 'redirect',
                        message: 'Редиректик',
                    },
                ],
            };

            req.env = {
                config: {
                    isB2b: true,
                    backend: {
                        connect: {
                            ru: {
                                hostname: 'connect.ru',
                            },
                        },
                    },
                },
                tld: 'ru',
            };

            next();
        });

        app.use(mw());

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect(302)
                .expect(
                    'Location',
                    'https://connect.ru/forbidden/?reason=not_activated&rethpath=https%3A%2F%2F127.0.0.1%2F'
                )
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should skip itself if no any redirect error', done => {
        app.use((req, res, next) => {
            res.locals.searchResults = {
                errors: [
                    {
                        code: 'ERROR_FOO',
                        type: 'error',
                        message: 'Ошибонька',
                    },
                ],
            };

            req.env = {
                config: {
                    backend: {
                        connect: {},
                    },
                },
            };

            next();
        });

        app.use(mw());

        app.use((req, res) => {
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

    it('Should skip itself if no any error at all', done => {
        app.use((req, res, next) => {
            res.locals.searchResults = {
                errors: [],
            };

            req.env = {
                config: {
                    backend: {
                        connect: {},
                    },
                },
            };

            next();
        });

        app.use(mw());

        app.use((req, res) => {
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
});
