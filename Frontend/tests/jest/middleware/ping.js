'use strict';

/* eslint-disable max-nested-callbacks */

const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/ping');

describe('middleware/ping', () => {
    it('Should response with OK', done => {
        const app = express();

        app.use(mw());

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect(200)
                .expect('ok')
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });
});
