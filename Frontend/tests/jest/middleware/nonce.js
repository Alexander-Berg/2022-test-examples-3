'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/nonce');

describe('middleware/nonce', () => {
    it('Should provide `res.locals.nonce`', done => {
        const app = express();

        app.use(mw());

        app.use((req, res) => {
            assert(res.locals.nonce);

            res.send('ok');
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
