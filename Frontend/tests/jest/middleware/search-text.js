'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/search-text');

describe('middleware/search-text', () => {
    it('Should get the value from errata', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                searchResults: {
                    errata: {
                        applied: true,
                        fixed_pure: 'errata-value',
                    },
                },
            };

            next();
        });

        app.use(mw());

        app.use((req, res) => {
            assert.strictEqual(res.locals.searchText, 'errata-value');

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

    it('Should get the value from query.text', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                searchResults: {
                    errata: null,
                },
            };
            req.query = {
                text: 'query-text-value',
            };

            next();
        });

        app.use(mw());

        app.use((req, res) => {
            assert.strictEqual(res.locals.searchText, 'query-text-value');

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
