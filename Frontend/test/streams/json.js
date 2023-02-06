/* eslint-env mocha */
'use strict';

const os = require('os');
const sinon = require('sinon');

const yandexLogger = require('../..');
const jsonStream = require('../../streams/json');

describe('YandexLogger. Streams. Json', () => {
    let logger;
    let stream;
    let clock;

    beforeEach(() => {
        stream = { write: sinon.spy() };

        logger = yandexLogger({
            streams: [
                {
                    stream: jsonStream({ stream }),
                },
            ],
        });

        clock = sinon.useFakeTimers(1000);
    });

    afterEach(() => {
        clock.restore();
    });

    it('должен логировать записи в формате json в переданный стрим', () => {
        logger.info('message');

        sinon.assert.calledWithExactly(stream.write, JSON.stringify({
            pid: process.pid,
            date: '1970-01-01T00:00:01.000Z',
            hostname: os.hostname(),
            level: 30,
            levelName: 'INFO',
            msgFormat: 'message',
            msgArgs: [],
            msg: 'message',
            name: 'yandex-logger',
        }) + '\n');
        sinon.assert.calledOnce(stream.write);
    });

    it('должен логировать циклические записи', () => {
        let circularObj = {};
        circularObj.circularRef = circularObj;
        circularObj.list = [circularObj, circularObj];

        logger.info({ circularObj }, 'message');

        sinon.assert.calledWithExactly(stream.write, JSON.stringify({
            pid: process.pid,
            date: '1970-01-01T00:00:01.000Z',
            hostname: os.hostname(),
            level: 30,
            levelName: 'INFO',
            circularObj: {
                circularRef: '[Circular ~.circularObj]',
                list: [
                    '[Circular ~.circularObj]',
                    '[Circular ~.circularObj]',
                ],
            },
            msgFormat: 'message',
            msgArgs: [],
            msg: 'message',
            name: 'yandex-logger',
        }) + '\n');
        sinon.assert.calledOnce(stream.write);
    });
});
