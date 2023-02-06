/* eslint-env mocha */
'use strict';

const os = require('os');
const assert = require('assert');
const sinon = require('sinon');
const _ = require('lodash');

const yandexLogger = require('../..');

describe('YandexLogger. Middleware. Secure Passport Cookie', () => {
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

    it('должен обфусцировать паспортные куки в req.headers.cookie', () => {
        logger = getLogger();

        let req = {
            headers: {
                host: 'yandex.ru',
                cookie: [
                    'yandexuid=123',
                    'Session_id=3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                    'Secure_session_id=3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                    'sessionid2=3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                    'Eda_id=3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                    'edaid2=3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                    'yandex_login=yandex_user',
                ].join('; '),
            },
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                headers: {
                    host: 'yandex.ru',
                    cookie: [
                        'yandexuid=123',
                        'Session_id=3%3A1516266223.5.0.1509446909355%3Ancyd_asaRhEHKAAAuAYCKg%3A2.1%7C32112343.4353114.302.2%3A4353114%7C464691359.1237818.2.2%3A1237818%7C176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                        'Secure_session_id=3%3A1516266223.5.0.1509446909355%3Ancyd_asaRhEHKAAAuAYCKg%3A2.1%7C32112343.4353114.302.2%3A4353114%7C464691359.1237818.2.2%3A1237818%7C176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                        'sessionid2=3%3A1516266223.5.0.1509446909355%3Ancyd_asaRhEHKAAAuAYCKg%3A2.1%7C32112343.4353114.302.2%3A4353114%7C464691359.1237818.2.2%3A1237818%7C176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                        'Eda_id=3%3A1516266223.5.0.1509446909355%3Ancyd_asaRhEHKAAAuAYCKg%3A2.1%7C32112343.4353114.302.2%3A4353114%7C464691359.1237818.2.2%3A1237818%7C176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                        'edaid2=3%3A1516266223.5.0.1509446909355%3Ancyd_asaRhEHKAAAuAYCKg%3A2.1%7C32112343.4353114.302.2%3A4353114%7C464691359.1237818.2.2%3A1237818%7C176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                        'yandex_login=XXXXXXXXXXX',
                    ].join('; '),
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    it('должен обфусцировать паспортные куки в req.cookies', () => {
        logger = getLogger({ field: 'req.cookies' });

        let req = {
            cookies: {
                /* eslint-disable camelcase */
                yandexuid: '123',
                Session_id: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                Secure_session_id: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                sessionid2: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                Eda_id: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                edaid2: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                yandex_login: 'yandex_user',
                /* eslint-enable camelcase */
            },
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                cookies: {
                    /* eslint-disable camelcase */
                    yandexuid: '123',
                    Session_id: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                    Secure_session_id: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                    sessionid2: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                    Eda_id: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                    edaid2: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                    yandex_login: 'XXXXXXXXXXX',
                    /* eslint-enable camelcase */
                },
            },
        }));
        sinon.assert.calledOnce(stdout.write);

        assert.deepStrictEqual(req, reqClone, 'Оригинальный объект не должен мутировать');
    });

    it('должен принимать набор кук в конфиге', () => {
        logger = getLogger({
            field: 'req.cookies',
            cookies: ['customSession'],
        });

        let req = {
            cookies: {
                /* eslint-disable camelcase */
                yandexuid: '123',
                Session_id: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                customSession: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                /* eslint-enable camelcase */
            },
        };
        let reqClone = _.cloneDeep(req);

        logger.info({ req }, 'message');

        sinon.assert.calledWithExactly(stdout.write, Object.assign({}, baseRecord, {
            req: {
                cookies: {
                    /* eslint-disable camelcase */
                    yandexuid: '123',
                    Session_id: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.I_HF25d8DGZaZvNjT0WRfqtB-sk',
                    customSession: '3:1516266223.5.0.1509446909355:ncyd_asaRhEHKAAAuAYCKg:2.1|32112343.4353114.302.2:4353114|464691359.1237818.2.2:1237818|176001.561453.XXXXXXXXXXXXXXXXXXXXXXXXXXX',
                    /* eslint-enable camelcase */
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
                require('../../middleware/secure-passport-cookie')(config),
            ],
        });
    }
});
