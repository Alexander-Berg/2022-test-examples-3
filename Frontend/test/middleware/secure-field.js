/* eslint-env mocha */
'use strict';

const os = require('os');
const assert = require('assert');
const sinon = require('sinon');
const _ = require('lodash');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Secure Field', () => {
    let logger;
    let stdout;
    let clock;
    let baseRecord;

    beforeEach(() => {
        stdout = { write: sinon.spy() };

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

    it('должен обфусцировать переданные поля', () => {
        logger = getLogger({
            fields: [
                'data.a',
                'data.b.c',
                'data.obj',
                'data.empty',
                'data.number',
                'foo',
            ],
        });

        let data = {
            a: 'value-123',
            b: {
                c: 'd',
            },
            obj: {},
            empty: '',
            number: 123,
            bypass: 'value',
        };
        let dataClone = _.cloneDeep(data);

        logger.info({ data, foo: 'bar' }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            data: {
                a: 'valuXXXXX',
                b: {
                    c: 'X',
                },
                obj: {},
                empty: '',
                number: 123,
                bypass: 'value',
            },
            foo: 'bXX',
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(data, dataClone, 'Оригинальный объект не должен мутировать');
    });

    it('должен использовать функцию обфускации из конфига', () => {
        logger = getLogger({
            fields: ['data.foo'],
            secureValue(value) {
                return `${value}-secured!`;
            },
        });

        let data = {
            foo: 'value',
        };
        let dataClone = _.cloneDeep(data);

        logger.info({ data }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            data: {
                foo: 'value-secured!',
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(data, dataClone, 'Оригинальный объект не должен мутировать');
    });

    it('не должен копировать объекты, если значение не изменилось', () => {
        logger = getLogger({
            fields: [
                'data.foo',
            ],
        });

        let data = {
            foo: '',
        };
        let dataClone = _.cloneDeep(data);

        logger.info({ data }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            data: {
                foo: '',
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.strictEqual(stdout.write.args[0][0].data, data, 'data не должен быть склонирован');
        assert.deepStrictEqual(data, dataClone, 'Оригинальный объект не должен мутировать');
    });

    it('должен работать без конфига', () => {
        logger = getLogger();

        logger.info({ foo: 'bar' }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            foo: 'bar',
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    function getLogger(config) {
        return yandexLogger({
            streams: [
                { stream: stdout },
            ],
            middleware: [
                require('../../middleware/secure-field')(config),
            ],
        });
    }
});
