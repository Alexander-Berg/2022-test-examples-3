/* eslint-env mocha */
'use strict';

const os = require('os');
const sinon = require('sinon');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Err', () => {
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
                require('../../middleware/err')(),
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

    it('должен перезаписать record.err', () => {
        let err = new TypeError('test');
        err.stack = '<stacktrace>';
        err.status = 500;

        logger.info({ err }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            err: {
                name: 'TypeError',
                message: 'test',
                status: 500,
                stack: '<stacktrace>',
            },
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен обрабатывать record.err без стектрейса', () => {
        let err = { message: 'test' };

        logger.info({ err }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            err: {
                message: 'test',
            },
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен падать без record.err', () => {
        logger.info('message');

        sinon.assert.calledWithExactly(stdout.write, baseRecord);
        sinon.assert.calledOnce(stdout.write);
    });
});
