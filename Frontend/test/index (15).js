/* eslint-env mocha */
'use strict';

const os = require('os');
const EventEmitter = require('events').EventEmitter;
const assert = require('assert');
const sinon = require('sinon');

const yandexLogger = require('..');

const levels = Object.freeze({
    trace: 10,
    debug: 20,
    info: 30,
    warn: 40,
    error: 50,
    fatal: 60,
});
const levelsList = Object.freeze(['trace', 'debug', 'info', 'warn', 'error', 'fatal']);

describe('YandexLogger', () => {
    for (let index = 0; index < levelsList.length; index++) {
        let currentLevel = levelsList[index];
        let levelsToLog = levelsList.slice(0, index + 1);

        describe(`(${currentLevel})`, () => {
            it('должен писать в переданные стримы', () => {
                let streams = levelsList.map(level => createStream(level));
                let logger = yandexLogger({ streams });

                let message = `Test message for ${currentLevel}`;
                logger[currentLevel](message);

                let expectedRecord = getStreamCallResult(currentLevel, {
                    msg: message,
                });

                for (let stream of streams) {
                    if (levelsToLog.indexOf(stream.level) > -1) {
                        sinon.assert.calledWithExactly(stream.stream.write, expectedRecord);
                        sinon.assert.calledOnce(stream.stream.write);
                    } else {
                        sinon.assert.notCalled(stream.stream.write);
                    }
                }
            });

            it('должен писать в стримы после обработки мидделвар', () => {
                let stream = createStream(currentLevel);
                let logger = yandexLogger({
                    streams: [stream],
                    middleware: [
                        record => {
                            record.a = 10;
                            record.b = 20;
                            record.c = 30;
                        },
                        [
                            record => { record.b++ },
                            record => { delete record.c },
                        ],
                    ],
                });

                let message = `Message for ${currentLevel}`;
                logger[currentLevel](message);

                sinon.assert.calledWithExactly(
                    stream.stream.write,
                    getStreamCallResult(currentLevel, {
                        a: 10,
                        b: 21,
                        msg: message,
                    })
                );
                sinon.assert.calledOnce(stream.stream.write);
            });

            it('должен писать в стримы после обработки асинхронных мидделвар', done => {
                let ee = new EventEmitter();
                let stream = createStream(currentLevel);
                let logger = yandexLogger({
                    streams: [stream],
                    middleware: [
                        (record, next) => {
                            setTimeout(() => {
                                record.a = 10;
                                record.b = 20;
                                record.c = 30;

                                next();
                            }, 10);
                        },
                        record => { record.b++ },
                        (record, next) => {
                            setTimeout(() => {
                                delete record.c;

                                next();
                            }, 0);
                        },
                        () => { ee.emit('done') },
                    ],
                });

                let message = `Message for ${currentLevel}`;
                logger[currentLevel](message);

                ee.on('done', () => {
                    sinon.assert.calledWithExactly(
                        stream.stream.write,
                        getStreamCallResult(currentLevel, {
                            a: 10,
                            b: 21,
                            msg: message,
                            msgFormat: message,
                            msgArgs: [],
                        })
                    );
                    sinon.assert.calledOnce(stream.stream.write);

                    done();
                });
            });

            it('должен логировать статичные поля', () => {
                let stream = createStream(currentLevel);
                let logger = yandexLogger({
                    streams: [stream],
                    fields: {
                        a: 1,
                        b: 1,
                        c: 1,
                    },
                    middleware: [
                        record => {
                            record.a++;
                            delete record.c;
                        },
                    ],
                });

                let message = `Message for ${currentLevel}`;
                logger[currentLevel](message);

                sinon.assert.calledWithExactly(
                    stream.stream.write,
                    getStreamCallResult(currentLevel, {
                        a: 2,
                        b: 1,
                        msg: message,
                    })
                );
                sinon.assert.calledOnce(stream.stream.write);
            });

            it('должен переопределять статичные поля', () => {
                let stream = createStream(currentLevel);
                let logger = yandexLogger({
                    streams: [stream],
                    fields: {
                        a: 1,
                        b: 1,
                        c: 1,
                    },
                    middleware: [
                        record => {
                            record.a++;
                            delete record.c;
                        },
                    ],
                });

                let message = `Message for ${currentLevel}`;
                logger[currentLevel]({ a: 10, d: 1 }, message);

                sinon.assert.calledWithExactly(
                    stream.stream.write,
                    getStreamCallResult(currentLevel, {
                        a: 11,
                        b: 1,
                        d: 1,
                        msg: message,
                    })
                );
                sinon.assert.calledOnce(stream.stream.write);
            });

            describe('Child-логгер', () => {
                it('должен клонировать родителя и расширять его статичные поля', () => {
                    let stream = createStream(currentLevel);
                    let logger = yandexLogger({
                        streams: [stream],
                        fields: {
                            type: 'base',
                            a: 1,
                            b: 1,
                        },
                        middleware: [record => { record.a++ }],
                    });
                    let logger1 = logger.child({ type: 'from base', a: 20, b: 20, c: 20 });
                    let logger2 = logger.child({ type: 'from base', a: 30, b: 30, c: 30 });
                    let logger3 = logger.child({ type: 'from one', a: 25, b: 25, c: 25, d: 25 });

                    let message = `Message for ${currentLevel}`;
                    let args = { b: 100, e: 100 };
                    let logData = {
                        type: 'base',
                        a: 2,
                        b: 100,
                        e: 100,
                        msg: message,
                    };

                    logger[currentLevel](args, message);
                    logger1[currentLevel](args, message);
                    logger2[currentLevel](args, message);
                    logger3[currentLevel](args, message);

                    sinon.assert.calledWithExactly(
                        stream.stream.write.getCall(0),
                        getStreamCallResult(currentLevel, logData)
                    );

                    sinon.assert.calledWithExactly(
                        stream.stream.write.getCall(1),
                        getStreamCallResult(currentLevel, Object.assign({}, logData, {
                            type: 'from base', a: 21, c: 20,
                        }))
                    );

                    sinon.assert.calledWithExactly(
                        stream.stream.write.getCall(2),
                        getStreamCallResult(currentLevel, Object.assign({}, logData, {
                            type: 'from base', a: 31, c: 30,
                        }))
                    );

                    sinon.assert.calledWithExactly(
                        stream.stream.write.getCall(3),
                        getStreamCallResult(currentLevel, Object.assign({}, logData, {
                            type: 'from one', a: 26, c: 25, d: 25,
                        }))
                    );

                    sinon.assert.callCount(stream.stream.write, 4);
                });
            });

            describe('getFields', () => {
                it('должен вернуть текущий набор полей', () => {
                    let logger = yandexLogger({
                        fields: {
                            name: 'yandex-logger',
                            a: 1,
                            b: 1,
                            nested: { c: 1 },
                        },
                    });

                    assert.deepStrictEqual(logger.getFields(), {
                        name: 'yandex-logger',
                        a: 1,
                        b: 1,
                        nested: { c: 1 },
                    });
                });
            });

            describe('extendFields', () => {
                it('должен расширить текущий набор полей', () => {
                    let stream = createStream(currentLevel);
                    let logger = yandexLogger({
                        streams: [stream],
                        fields: {
                            a: 1,
                            b: 1,
                            nested: { c: 1 },
                        },
                        middleware: [record => { record.a++ }],
                    });

                    logger.extendFields({ a: 20, nested: { d: 1 } });

                    let message = `Message for ${currentLevel}`;
                    let args = { e: 100 };
                    let logData = {
                        a: 21,
                        b: 1,
                        nested: { d: 1 },
                        e: 100,
                        msg: message,
                    };

                    logger[currentLevel](args, message);

                    sinon.assert.calledWithExactly(
                        stream.stream.write.getCall(0),
                        getStreamCallResult(currentLevel, logData)
                    );

                    sinon.assert.calledOnce(stream.stream.write);
                });
            });
        });
    }
});

function createStream(level) {
    return {
        level,
        stream: { write: sinon.spy() },
    };
}

function getStreamCallResult(level, data) {
    return Object.assign(
        {
            name: 'yandex-logger',
            pid: process.pid,
            hostname: os.hostname(),
            level: levels[level],
            levelName: level.toUpperCase(),
            date: sinon.match.date,
        },
        { msg: data.msg, msgFormat: data.msg, msgArgs: [] },
        data
    );
}
