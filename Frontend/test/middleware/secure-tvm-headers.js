/* eslint-env mocha */
'use strict';

const os = require('os');
const assert = require('assert');
const sinon = require('sinon');
const _ = require('lodash');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Secure TVM Headers', () => {
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

    it('должен обфусцировать TVM тикеты в req.headers', () => {
        logger = getLogger();

        let req = {
            headers: {
                ticket: 'TVM-1 Ticket',
                'x-ya-service-ticket': 'TVM 2 Service Ticket',
                'x-ya-user-ticket': 'TVM 2 User Ticket',
                authorization: 'Unknown Ticket',
            },
        };
        let reqClone = _.cloneDeep(req);
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                headers: {
                    ticket: 'TVM-1 XXXXXX',
                    'x-ya-service-ticket': 'TVM 2 ServXXXXXXXXXX',
                    'x-ya-user-ticket': 'TVM 2 UsXXXXXXXXX',
                    authorization: 'Unknown Ticket',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    it('должен принимать путь до заголовков', () => {
        logger = getLogger({ field: 'api.headers' });

        let api = {
            headers: {
                ticket: 'TVM-1 Ticket',
                'x-ya-service-ticket': 'TVM 2 Service Ticket',
                'x-ya-user-ticket': 'TVM 2 User Ticket',
                authorization: 'Unknown Ticket',
            },
        };
        let apiClone = _.cloneDeep(api);
        logger.info({ api }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            api: {
                headers: {
                    ticket: 'TVM-1 XXXXXX',
                    'x-ya-service-ticket': 'TVM 2 ServXXXXXXXXXX',
                    'x-ya-user-ticket': 'TVM 2 UsXXXXXXXXX',
                    authorization: 'Unknown Ticket',
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(api, apiClone, 'Оригинальный объект не должен мутировать');
    });

    it('не должен падать без заголовков в req.headers', () => {
        logger = getLogger();

        let req = {
            headers: {},
        };
        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                headers: {},
            },
        }));
        sinon.assert.calledOnce(stdout.write);
    });

    function getLogger(config) {
        return yandexLogger({
            streams: [
                { stream: stdout },
            ],
            middleware: [
                require('../../middleware/secure-tvm-headers')(config),
            ],
        });
    }
});
