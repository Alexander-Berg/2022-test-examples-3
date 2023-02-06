'use strict';

/* eslint-disable max-nested-callbacks */

const express = require('express');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/plain-json');

describe('middleware/plain-json', () => {
    it('Should response with json if query parameter passed', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.searchResults = { foo: 'bar' };

            next();
        });

        app.use(mw());

        sw(app)((test, closeServer) => {
            test.get('/')
                .query({ plainjson: true })
                .expect(200)
                .expect('{"foo":"bar"}')
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });

    it('Should skip itself if query parameter not passed', done => {
        const app = express();

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
