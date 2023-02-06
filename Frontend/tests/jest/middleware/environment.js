'use strict';

/* eslint-disable max-nested-callbacks */

const assert = require('assert');
const express = require('express');
const cookieParser = require('cookie-parser');
const { supertestWrapper: sw } = require('../helpers');
const mw = require('../../../middleware/environment');

describe('middleware/environment', () => {
    it('Should provide req.env', done => {
        const app = express();

        app.use(cookieParser());

        app.use(mw({ config: 'config' }));

        app.use((req, res) => {
            assert.strictEqual(req.env.config, 'config');
            assert.strictEqual(req.env.query.text, 'text');
            assert.strictEqual(req.env.parsedUrl.protocol, 'http:');
            assert.strictEqual(req.env.parsedUrl.hostname, 'foo.bar');
            assert.strictEqual(req.env.parsedUrl.query, 'text=text');
            assert.equal(req.env.tld, 'bar');
            assert.strictEqual(req.env.cookies.yandexuid, 'yandexuid');
            assert(/^\d{1,2}\.\d{1,3}\.\d{1,2}$/.test(req.env.appVersion));

            res.sendStatus(200);
        });

        sw(app)((test, closeServer) => {
            test.get('/')
                .query({ text: 'text' })
                .set('cookie', 'yandexuid=yandexuid')
                .set('host', 'foo.bar')
                .expect(200)
                .end((...args) => {
                    closeServer();
                    done(...args);
                });
        });
    });
});
