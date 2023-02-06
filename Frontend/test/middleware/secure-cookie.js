/* eslint-env mocha */
'use strict';

const os = require('os');
const assert = require('assert');
const sinon = require('sinon');
const _ = require('lodash');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Secure Cookie', () => {
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

    it('должен обфусцировать cookie в виде строки из req.headers.cookie', () => {
        logger = getLogger({
            cookies: ['str', 'number', 'empty'],
        });

        let req = {
            headers: {
                host: 'yandex.ru',
                cookie: 'yandexuid=123; str=value; number=123; empty=; sk=asdasd',
            },
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                headers: {
                    host: 'yandex.ru',
                    cookie: 'yandexuid=123; str=vaXXX; number=1XX; empty=; sk=asdasd',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    it('не должен копировать заголовки, если значение cookie не изменилось', () => {
        logger = getLogger({
            cookies: ['empty'],
        });

        let req = {
            headers: {
                host: 'yandex.ru',
                cookie: 'yandexuid=123; str=value; number=123; empty=; sk=asdasd',
            },
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                headers: {
                    host: 'yandex.ru',
                    cookie: 'yandexuid=123; str=value; number=123; empty=; sk=asdasd',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.strictEqual(stdout.write.args[0][0].req, req, 'req не должен быть склонирован');
        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    it('должен обфусцировать cookie в виде объекта из req.cookies', () => {
        logger = getLogger({
            field: 'req.cookies',
            cookies: ['str', 'number', 'obj', 'empty'],
        });

        let req = {
            cookies: {
                yandexuid: '123',
                str: 'value',
                number: 123,
                obj: {},
                empty: '',
                sk: 'asdasd',
            },
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                cookies: {
                    yandexuid: '123',
                    str: 'vaXXX',
                    number: 123,
                    obj: {},
                    empty: '',
                    sk: 'asdasd',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    it('не должен падать с req.cookies == null', () => {
        logger = getLogger({
            field: 'req.cookies',
            cookies: ['str'],
        });

        let req = {
            cookies: null,
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                cookies: null,
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    it('должен использовать функцию обфускации из конфига', () => {
        logger = getLogger({
            field: 'req.cookies',
            cookies: ['str'],
            secureValue(value) {
                return `${value}-secured!`;
            },
        });

        let req = {
            cookies: {
                yandexuid: '123',
                str: 'value',
            },
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                cookies: {
                    yandexuid: '123',
                    str: 'value-secured!',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    it('должен работать без конфига', () => {
        logger = getLogger();

        let req = {
            headers: {
                host: 'yandex.ru',
                cookie: 'yandexuid=123; str=value',
            },
            cookies: {
                yandexuid: '123',
                str: 'value',
            },
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                headers: {
                    host: 'yandex.ru',
                    cookie: 'yandexuid=123; str=value',
                },
                cookies: {
                    yandexuid: '123',
                    str: 'value',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    function getLogger(config) {
        return yandexLogger({
            streams: [
                { stream: stdout },
            ],
            middleware: [
                require('../../middleware/secure-cookie')(config),
            ],
        });
    }
});
