'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/counters');

describe('middleware/counters', () => {
    it('Should provide res.locals.counters with sorted updated', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {
                config: {
                    env: 'dev',
                },
                page: 'search',
            };

            req.query = {
                dump_counters: '1',
                sorted: 'updated',
            };

            res.locals = {
                id: 1,
                searchText: 'search text',
                searchResults: {
                    meta: {
                        page: 0,
                        count: 10,
                        ab_info: {
                            buckets: null,
                            crypted_buckets: null,
                        },
                    },
                },
            };

            next();
        });

        app.use(mw());

        app.use((req, res) => {
            assert.deepStrictEqual(res.locals.counters, {
                env: 'dev',
                page: 'search',
                text: 'search text',
                reqid: 1,
                currentPage: 0,
                count: 10,
                dumpCounters: true,
                expBuckets: null,
                sorted: 'updated',
                exp: '',
            });

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

    it('Should provide res.locals.counters with sorted rlv', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {
                config: {
                    env: 'dev',
                },
                page: 'search',
            };

            req.query = {
                dump_counters: '1',
                sorted: 'rlv',
            };

            res.locals = {
                id: 1,
                searchText: 'search text',
                searchResults: {
                    meta: {
                        page: 0,
                        count: 10,
                        ab_info: {
                            buckets: null,
                            crypted_buckets: null,
                        },
                    },
                },
            };

            next();
        });

        app.use(mw());

        app.use((req, res) => {
            assert.deepStrictEqual(res.locals.counters, {
                env: 'dev',
                page: 'search',
                text: 'search text',
                reqid: 1,
                currentPage: 0,
                count: 10,
                dumpCounters: true,
                expBuckets: null,
                sorted: 'rlv',
                exp: '',
            });

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

    it('Should provide res.locals.counters with sorted', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {
                config: {
                    env: 'dev',
                },
                page: 'search',
            };

            req.query = {
                dump_counters: '1',
            };

            res.locals = {
                id: 1,
                searchText: 'search text',
                searchResults: {
                    meta: {
                        page: 0,
                        count: 10,
                        ab_info: {
                            buckets: null,
                            crypted_buckets: null,
                        },
                    },
                },
            };

            next();
        });

        app.use(mw());

        app.use((req, res) => {
            assert.deepStrictEqual(res.locals.counters, {
                env: 'dev',
                page: 'search',
                text: 'search text',
                reqid: 1,
                currentPage: 0,
                count: 10,
                dumpCounters: true,
                expBuckets: null,
                sorted: 'rlv',
                exp: '',
            });

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

    it('Should provide res.locals.counters with expirement wizards_to_right', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {
                config: {
                    env: 'dev',
                },
                page: 'search',
            };

            req.query = {
                dump_counters: '1',
            };

            res.locals = {
                id: 1,
                searchText: 'search text',
                searchResults: {
                    meta: {
                        page: 0,
                        count: 10,
                        ab_info: {
                            buckets: null,
                            crypted_buckets: null,
                        },
                        features: {
                            wizards_to_right: 1,
                        },
                    },
                },
                wizardsToRight: true,
            };

            next();
        });

        app.use(mw());

        app.use((req, res) => {
            assert.deepStrictEqual(res.locals.counters, {
                env: 'dev',
                page: 'search',
                text: 'search text',
                reqid: 1,
                currentPage: 0,
                count: 10,
                dumpCounters: true,
                expBuckets: null,
                sorted: 'rlv',
                exp: 'wizards_to_right',
            });

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
