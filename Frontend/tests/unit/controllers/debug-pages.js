'use strict';

require('should-http');

const proxyquire = require('proxyquire');

const meta = require('../fixtures/models/meta');
const experiment = require('../fixtures/models/experiment');
const express = require('../fixtures/express');
const mongoose = { connection: { readyState: 0 } };

const DebugPages = proxyquire.load('../../../src/server/controllers/debug-pages', {
    '../models/experiment': experiment,
    '../models/meta': meta,
    '../models/user': {},
    mongoose,
});

describe('experiments-api', () => {
    let debug, req, res, sandbox;

    beforeEach(() => {
        req = {};
        res = express.getRes();

        sandbox = sinon.createSandbox();
        debug = new DebugPages(req, res);
    });

    afterEach(() => sandbox.restore());

    describe('Проверка соединения с базой:', () => {
        it('если соединения с базой нет, ждём ошибку', () => {
            const STATUS_CODE_503 = 503;

            sandbox.stub(mongoose, 'connection').value({ readyState: 0 });

            return debug.dbCheck()
                .then((res) => {
                    assert.equal(res.getStatus(), STATUS_CODE_503);
                });
        });

        it('если соединение с базой есть, ждём количество экспериментов', () => {
            const STATUS_OK = 200;

            sandbox.stub(mongoose, 'connection').value({ readyState: 1 });

            return debug.dbCheck()
                .then((res) => {
                    assert.equal(res.getStatus(), STATUS_OK);
                    assert.isNumber(res.getJsonBody().count);
                });
        });
    });
});
