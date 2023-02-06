/* eslint-env mocha */
'use strict';

const os = require('os');
const sinon = require('sinon');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Blackbox', () => {
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
                require('../../middleware/blackbox')(),
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

    it('должен взять пользователя из req.blackbox', () => {
        let req = {
            requestId: 'req-1',
            blackbox: {
                uid: 'user-id',
                login: 'user-login',
                email: 'user-email',
                avatar: 'user-avatar',
            },
        };
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            user: {
                uid: 'user-id',
                login: 'user-login',
                email: 'user-email',
            },
            req: {
                requestId: 'req-1',
                blackbox: {
                    uid: 'user-id',
                    login: 'user-login',
                    email: 'user-email',
                    avatar: 'user-avatar',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен брать пользователя без uid', () => {
        let req = {
            requestId: 'req-1',
            blackbox: {
                login: 'user-login',
            },
        };
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                requestId: 'req-1',
                blackbox: {
                    login: 'user-login',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен падать без req.blackbox', () => {
        let req = {};
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, { req }));
        sinon.assert.calledOnce(stdout.write);
    });

    it('не должен падать без record.req', () => {
        logger.info('message');

        sinon.assert.calledWithExactly(stdout.write, baseRecord);
        sinon.assert.calledOnce(stdout.write);
    });
});
