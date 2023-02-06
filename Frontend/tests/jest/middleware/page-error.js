'use strict';

/* eslint-disable max-nested-callbacks */

const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/page-error');

describe('middleware/page-error', () => {
    it('Should send status 500', done => {
        const app = express();

        app.use(mw({ isB2b: false }));

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect(500)
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should response with an error html-page', done => {
        const app = express();

        app.use(mw({ isB2b: true }));

        sw(app)((test, closeServer) => {
            test.get('/')
                .expect(/b-page__error-message/)
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });
});
