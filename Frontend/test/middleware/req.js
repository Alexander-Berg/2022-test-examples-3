/* eslint-env mocha */
'use strict';

const os = require('os');
const assert = require('assert');
const sinon = require('sinon');
const _ = require('lodash');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Req', () => {
    let logger;
    let stdout;
    let clock;
    let req;
    let baseRecord;

    beforeEach(() => {
        stdout = { write: sinon.spy() };

        logger = yandexLogger({
            streams: [
                { stream: stdout },
            ],
            middleware: [
                require('../../middleware/req')(),
            ],
        });

        clock = sinon.useFakeTimers(1000);

        req = {
            method: 'POST',
            protocol: 'https',
            hostname: 'yandex.ru',
            url: '/afisha?from=yamain',
            headers: {
                host: 'yandex.ru',
                cookie: 'yandexuid=123',
            },
            body: '<data>',
            connection: {
                remoteAddress: '127.0.0.1',
                remotePort: 43212,
            },
            blackbox: {
                uid: 123,
            },
            requestId: 'req123',
            cookies: {
                yandexuid: 'yandexuid123',
            },
        };

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

    it('должен перезаписать record.req', () => {
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                method: 'POST',
                url: 'https://yandex.ru/afisha?from=yamain',
                headers: {
                    host: 'yandex.ru',
                    cookie: 'yandexuid=123',
                },
                body: '<data>',
                remoteAddress: '127.0.0.1',
                remotePort: 43212,
                yandexuid: 'yandexuid123',
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный record.req не долен мутировать');
    });

    it('должен построить url из свойства originalUrl', () => {
        req.originalUrl = '/original-url';
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                method: 'POST',
                url: 'https://yandex.ru/original-url',
                headers: {
                    host: 'yandex.ru',
                    cookie: 'yandexuid=123',
                },
                body: '<data>',
                remoteAddress: '127.0.0.1',
                remotePort: 43212,
                yandexuid: 'yandexuid123',
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный record.req не долен мутировать');
    });

    it('должен обработать record.req без свойства connection', () => {
        delete req.connection;
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                method: 'POST',
                url: 'https://yandex.ru/afisha?from=yamain',
                headers: {
                    host: 'yandex.ru',
                    cookie: 'yandexuid=123',
                },
                body: '<data>',
                remoteAddress: undefined,
                remotePort: undefined,
                yandexuid: 'yandexuid123',
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный record.req не долен мутировать');
    });

    it('не должен падать с пустым объектом в record.req', () => {
        req = {};

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                method: 'GET',
                url: '',
                headers: undefined,
                body: undefined,
                remoteAddress: undefined,
                remotePort: undefined,
                yandexuid: undefined,
            },
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен падать с объектом в req.url', () => {
        req.url = {};

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: '[logger: failed to parse record.req]',
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен падать без record.req', () => {
        logger.info('message');

        sinon.assert.calledWithExactly(stdout.write, baseRecord);
        sinon.assert.calledOnce(stdout.write);
    });
});
