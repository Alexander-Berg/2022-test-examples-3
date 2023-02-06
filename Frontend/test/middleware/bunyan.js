/* eslint-env mocha */
'use strict';

const os = require('os');
const assert = require('assert');
const sinon = require('sinon');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Bunyan', () => {
    let logger;
    let stdout;
    let clock;

    beforeEach(() => {
        stdout = { write: sinon.spy() };

        logger = yandexLogger({
            streams: [
                { stream: stdout },
            ],
            middleware: [
                require('../../middleware/bunyan')(),
            ],
        });

        clock = sinon.useFakeTimers(1000);
    });

    afterEach(() => {
        clock.restore();
    });

    it('должен добавить bunyan-специфичные поля в record', () => {
        logger.info('message');

        sinon.assert.calledWithExactly(stdout.write, {
            pid: process.pid,
            date: sinon.match.date,
            name: 'yandex-logger',
            hostname: os.hostname(),
            level: 30,
            levelName: 'INFO',
            msg: 'message',
            msgFormat: 'message',
            msgArgs: [],
            time: sinon.match.date,
            v: 0,
        });

        assert.strictEqual(stdout.write.args[0][0].time.toISOString(), '1970-01-01T00:00:01.000Z');

        sinon.assert.calledOnce(stdout.write);
    });
});
