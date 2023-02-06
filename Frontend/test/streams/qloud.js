/* eslint-env mocha */
'use strict';

const os = require('os');
const sinon = require('sinon');

const yandexLogger = require('../..');
const qloudStream = require('../../streams/qloud');

describe('YandexLogger. Streams. Qloud', () => {
    let stream;
    let clock;

    beforeEach(() => {
        stream = { write: sinon.spy() };

        clock = sinon.useFakeTimers(1000);
    });

    afterEach(() => {
        clock.restore();
    });

    it('должен логировать записи в формате qloud в переданный стрим', () => {
        let logger = createLogger({ stream });

        let err = {
            message: 'Error message',
            stack: 'Error stack',
        };
        logger.info({ err, customField: 'foo' }, 'message');

        sinon.assert.calledWithExactly(stream.write, JSON.stringify({
            msg: 'message',
            stackTrace: 'Error stack',
            level: 'INFO',
            levelStr: 'INFO',
            loggerName: 'yandex-logger',
            '@fields': {
                pid: process.pid,
                date: '1970-01-01T00:00:01.000Z',
                hostname: os.hostname(),
                level: 30,
                levelName: 'INFO',
                err: {
                    name: 'Object',
                    message: 'Error message',
                    stack: 'Error stack',
                },
                customField: 'foo',
                msgFormat: 'message',
                msgArgs: [],
                msg: 'message',
                name: 'yandex-logger',
            },
        }) + '\n');
        sinon.assert.calledOnce(stream.write);
    });

    it('должен логировать циклические записи', () => {
        let logger = createLogger({ stream });

        let circularObj = {};
        circularObj.circularRef = circularObj;
        circularObj.list = [circularObj, circularObj];

        logger.info({ circularObj }, 'message');

        sinon.assert.calledWithExactly(stream.write, JSON.stringify({
            msg: 'message',
            level: 'INFO',
            levelStr: 'INFO',
            loggerName: 'yandex-logger',
            '@fields': {
                pid: process.pid,
                date: '1970-01-01T00:00:01.000Z',
                hostname: os.hostname(),
                level: 30,
                levelName: 'INFO',
                circularObj: {
                    circularRef: '[Circular ~.@fields.circularObj]',
                    list: [
                        '[Circular ~.@fields.circularObj]',
                        '[Circular ~.@fields.circularObj]',
                    ],
                },
                msgFormat: 'message',
                msgArgs: [],
                msg: 'message',
                name: 'yandex-logger',
            },
        }) + '\n');
        sinon.assert.calledOnce(stream.write);
    });

    it('должен убирать поля из excludeFields', () => {
        let logger = createLogger({ stream, excludeFields: ['field1', 'msg', 'msgArgs', 'msgFormat'] });

        logger.info({ field1: 'foo', field2: 'bar' }, 'message');

        sinon.assert.calledWithExactly(stream.write, JSON.stringify({
            msg: 'message',
            level: 'INFO',
            levelStr: 'INFO',
            loggerName: 'yandex-logger',
            '@fields': {
                pid: process.pid,
                date: '1970-01-01T00:00:01.000Z',
                hostname: os.hostname(),
                level: 30,
                levelName: 'INFO',
                field2: 'bar',
                name: 'yandex-logger',
            },
        }) + '\n');
        sinon.assert.calledOnce(stream.write);
    });
});

function createLogger(config) {
    return yandexLogger({
        streams: [
            {
                stream: qloudStream(config),
            },
        ],
    });
}
