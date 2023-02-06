'use strict';

/* eslint-disable max-nested-callbacks */

const url = require('url');
const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/plain-xml-redirect');

describe('middleware/plain-xml-redirect', () => {
    it('Should redirect to backend if query.plainxml is present', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {
                config: {
                    backend: {
                        abovemeta: {
                            ru: {
                                hostname: 'back-host',
                            },
                        },
                    },
                },
                parsedUrl: {
                    pathname: '/scope',
                },
                tld: 'ru',
            };

            res.locals = {
                sessionid: {
                    login: 'login',
                    lang: 'lang',
                },
            };

            next();
        });

        app.use(mw());

        sw(app)((test, closeServer) => {
            test.get('/?text=geirfgsirf&plainxml=1')
                .expect(302)
                .expect('Location', 'https://back-host/_abovemeta/?text=geirfgsirf&scope=scope&user=login&language=lang')
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should redirect to backend if query.plainxml is present in b2b environment', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {
                config: {
                    backend: {
                        abovemeta: {
                            ru: {
                                hostname: 'back-host',
                            },
                        },
                    },
                    isB2b: true,
                },
                tld: 'ru',
            };

            res.locals.sessionid = {
                login: 'login',
                lang: 'lang',
            };

            next();
        });

        app.get('/:scope', mw());

        sw(app)((test, closeServer) => {
            test.get('/scopename')
                .query({
                    text: 'geirfgsirf',
                    plainxml: 1,
                })
                .expect(302)
                .expect(
                    'Location',
                    'https://back-host/_abovemeta/?text=geirfgsirf&scope=scopename&user=login&language=lang'
                )
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should support facets, features, pages, sorts', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {
                config: {
                    backend: {
                        abovemeta: {
                            ru: {
                                hostname: 'back-host',
                            },
                        },
                    },
                },
                parsedUrl: {
                    pathname: '/scope',
                },
                tld: 'ru',
            };

            res.locals = {
                sessionid: {
                    login: 'login',
                    lang: 'lang',
                },
            };

            next();
        });

        app.use(mw());

        sw(app)((test, closeServer) => {
            test.get('/')
                .query({
                    text: 'geirfgsirf',
                    plainxml: 1,
                    'facet.foo': 'bar',
                    'feature.foo': 'bar',
                    p: 100500,
                    sorted: 'foo',
                })
                .expect(302)
                .expect(
                    'Location',
                    url.format({
                        protocol: 'https',
                        hostname: 'back-host',
                        pathname: '/_abovemeta/',
                        query: {
                            text: 'geirfgsirf',
                            scope: 'scope',
                            user: 'login',
                            language: 'lang',
                            'facet.foo': 'bar',
                            'feature.foo': 'bar',
                            p: '100500',
                            sorted: 'foo',
                        },
                    }))
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should do nothing if query.plainxml is not present', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {
                config: {
                    backend: {
                        abovemeta: {
                            ru: {
                                hostname: 'back-host',
                            },
                        },
                    },
                },
                parsedUrl: {
                    pathname: '/scope',
                },
                tld: 'ru',
            };

            res.locals = {
                sessionid: {
                    login: 'login',
                    lang: 'lang',
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
