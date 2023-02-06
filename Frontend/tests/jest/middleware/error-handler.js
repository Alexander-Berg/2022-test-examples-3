'use strict';

/* eslint-disable max-nested-callbacks */

const express = require('express');
const {
    supertestWrapper: sw,
    mockAddLoggerToRequest,
} = require('../helpers');
const mw = require('../../../middleware/error-handler');

let app;

describe('middleware/error-handler', () => {
    beforeEach(() => {
        app = express();

        app.use(mockAddLoggerToRequest);
    });

    it('Should rewrite url to /500', done => {
        app.use((req, res, next) => {
            next(new Error('foo'));
        });

        app.use(mw());

        app.get('/500', (req, res) => {
            res.sendStatus('200');
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
