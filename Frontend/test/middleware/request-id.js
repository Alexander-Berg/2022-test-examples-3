/* eslint-env mocha */
'use strict';

const os = require('os');
const sinon = require('sinon');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Request ID', () => {
    let logger;
    let stdout;
    let clock;
    let baseRecord;

    beforeEach(() => {
        stdout = { write: sinon.spy() };

        logger = yandexLogger({
            streams: [
                { stream: stdout },
            ],
            middleware: [
                require('../../middleware/request-id')(),
            ],
        });

        clock = sinon.useFakeTimers(1000);

        baseRecord = {
            pid: process.pid,
            date: sinon.match.date,
            name: 'yandex-logger',
            hostname: os.hostname(),
            level: 30,
            levelName: 'INFO',
            msg: 'message',
            msgFormat: 'message',
            msgArgs: [],
        };
    });

    afterEach(() => {
        clock.restore();
    });

    it('должен взять requestId из req.id', () => {
        let req = {
            id: 'req-1',
            requestId: 'req-2',
            headers: {
                'x-request-id': 'req-3',
                'x-req-id': 'req-4',
            },
        };
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req,
            requestId: 'req-1',
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('должен взять requestId из req.requestId', () => {
        let req = {
            id: null,
            requestId: 'req-2',
            headers: {
                'x-request-id': 'req-3',
                'x-req-id': 'req-4',
            },
        };
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req,
            requestId: 'req-2',
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('должен взять requestId из заголовка X-Request-Id', () => {
        let req = {
            id: null,
            requestId: null,
            headers: {
                'x-request-id': 'req-3',
                'x-req-id': 'req-4',
            },
        };
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req,
            requestId: 'req-3',
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('должен взять requestId из заголовка X-Req-Id', () => {
        let req = {
            id: null,
            requestId: null,
            headers: {
                'x-req-id': 'req-4',
            },
        };
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req,
            requestId: 'req-4',
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('должен взять существующий requestId', () => {
        let req = {
            id: 'id-2',
        };
        logger.info({ req, requestId: 'id-1' }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req,
            requestId: 'id-1',
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен падать без requestId', () => {
        let req = {};
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req,
            requestId: undefined,
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен падать без record.req', () => {
        logger.info('message');

        sinon.assert.calledWithExactly(stdout.write, baseRecord);
        sinon.assert.calledOnce(stdout.write);
    });
});
