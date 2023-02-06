'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/page');

describe('middleware/page', () => {
    it('Should provide req.env.page', done => {
        const app = express();

        app.use((req, res, next) => {
            req.env = {};

            next();
        });

        app.use(mw());

        app.use((req, res) => {
            assert.strictEqual(req.env.page, 'search');

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
