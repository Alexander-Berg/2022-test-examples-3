jest.mock('asker-as-promised');
const ask = require('asker-as-promised');
const getAuthMiddleware = require('../../middleware/auth');

let lastLoggerParams;
const DEFAULT_CONFIG = {
    hostCfg: {
        host: 'blackbox.yandex.net',
        path: '/blackbox'
    },
    dbFields: [
        { userField: 'lang', dbField: 'userinfo.lang.uid' },
        { userField: 'hasDisk', dbField: 'subscription.suid.59' },
        { userField: 'yandexStaffLogin', dbField: 'subscription.login.669' }
    ],
    attributes: [{ userField: 'b2b', attributeId: '1011' }],
    logger: {
        log(req, component, params) {
            lastLoggerParams = params;
        }
    }
};

const DEFAULT_REQ = {
    ip: 'fe80::1',
    cookies: {
        // сессионные куки тестового пользователя из тестинга
        // eslint-disable-next-line max-len
        Session_id: '3:1525960486.5.2.1511987398289:JQABAAAAAAARNwAAuAYCKg:9.1|4003079057.0.2|4009012048.236561.2.2:236561|4005521594.13722691.2.2:13722691|4008306858.315332.2.2:315332|1130000000205551.1634711.2.2:1634711|4004250101.5403601.2.2:5403601|4010192332.6207781.2.2:6207781|4010192358.6208313.2.2:6208313|4011587096.11887587.2.2:11887587|295172.104068.GP-sn35C8L3d3JDBSqzvzrc2MXc',
        // eslint-disable-next-line max-len
        sessionid2: '3:1525960486.5.0.1511987398289:JQABAAAAAAARNwAAuAYCKg:9.1|4011587096.-1.0|4005521594.-1.0|295172.647847.3aVLYLejQefumPmv0GP2zhalh90'
    },
    tvmTickets: {
        // тикет из тестинга
        // eslint-disable-next-line max-len
        blackbox: '3:serv:CNwdEKH109cFIgcI5Il6EOAB:BPa4Q6Ko79fVQ7pKzM5mGmkm9j9N-ubTgFuYNfKXpODkv-q-vMnWUULxLHKEKMJXRLE2XmeHpJk6j9Z6r65HUWNrynFEjdjDnjE-anccVFaVnLygjjzKU2SjjT_TLtmZECLSjKtIKw27WBdBYbxdSfthPcIVL6Rt7KNDcakHL-o'
    },
    headers: {
        host: 'disk.yandex.ru'
    }
};

const USERS = [{
    id: '4005521594',
    status: { value: 'VALID', id: 0 },
    uid: { value: '4005521594', lite: false, hosted: false },
    login: 'km256.new.1',
    have_password: true,
    have_hint: true,
    karma: { value: 0 },
    karma_status: { value: 0 },
    regname: 'km256.new.1',
    display_name: {
        name: 'km256.new.1',
        avatar: { default: '0/0-0', empty: true }
    },
    dbfields: {
        'subscription.login.669': '',
        'subscription.suid.59': '1',
        'userinfo.lang.uid': 'ru'
    },
    attributes: {},
    auth: {
        password_verification_age: 253731,
        have_password: true,
        secure: true,
        allow_plain_text: true,
        partner_pdd_token: false
    }
}, {
    id: '4008306858',
    status: {
        value: 'VALID',
        id: 0
    },
    uid: {
        value: '4008306858',
        lite: false,
        hosted: false
    },
    login: 'km256.new.2',
    have_password: true,
    have_hint: true,
    karma: {
        value: 0
    },
    karma_status: {
        value: 0
    },
    regname: 'km256.new.2',
    display_name: {
        name: 'km256.new.2',
        avatar: {
            default: '0/0-0',
            empty: true
        }
    },
    dbfields: {
        'subscription.login.669': '',
        'subscription.suid.59': '1',
        'userinfo.lang.uid': 'ru'
    },
    attributes: {},
    auth: {
        password_verification_age: 13661090,
        have_password: true,
        secure: true,
        allow_plain_text: true,
        partner_pdd_token: false
    }
}, {
    id: '40083068581',
    status: {
        value: 'EXPIRED',
        id: 2
    }
}];

describe('getAuthMiddleware', () => {
    describe('Авторизация найдена', () => {
        beforeEach(() => {
            ask.mockImplementation(() => Promise.resolve({
                data: JSON.stringify({
                    status: { value: 'VALID', id: 0 },
                    error: 'OK',
                    default_uid: USERS[0].id,
                    users: USERS,
                    // eslint-disable-next-line max-len
                    user_ticket: '3:user:CNYdENi60dcFGmUKBgiRx-j0DgoGCPWDsPUOCgYIutH99Q4KBgiq0af3DgoGCNDW0vcOCgYIzNua-A4KBgjm25r4DgoGCJjs7_gOCgkI74XvhKr3gAIQutH99Q4aDGJiOnNlc3Npb25pZCDkiXooAQ:UUNcQb4ZI_qQZEM-lkEszPh-3xphRc6HtC9RNXkhumUA5akAT99ieZq-YVynl0XjA7ffrfeHE3FvSGg_qF6b_nCYfJA_dwVFCvB7msYFUHFx4PS6n6UxkH6qxph7r8vH1rRI_QSoiRz8_NcuxwzvL4zKGJ1KEWKdZCuvyioCl8Y'
                })
            }));
        });

        it('должна обсфуцировать sessionid и tvm-тикет в логах', (done) => {
            const req = Object.assign({}, DEFAULT_REQ);
            getAuthMiddleware(DEFAULT_CONFIG)(req, {}, () => {
                expect(lastLoggerParams.serviceUrl).toBe(
                    // eslint-disable-next-line max-len
                    'https://blackbox.yandex.net/blackbox?method=sessionid&multisession=yes&host=disk.yandex.ru&sessionid=3%3A1525960486.5.2.1511987398289%3AJQABAAAAAAARNwAAuAYCKg%3A9.1%7C4003079057.0.2%7C4009012048.236561.2.2%3A236561%7C4005521594.13722691.2.2%3A13722691%7C4008306858.315332.2.2%3A315332%7C1130000000205551.1634711.2.2%3A1634711%7C4004250101.5403601.2.2%3A5403601%7C4010192332.6207781.2.2%3A6207781%7C4010192358.6208313.2.2%3A6208313%7C4011587096.11887587.2.2%3A11887587%7C295172.104068.***&sslsessionid=3%3A1525960486.5.0.1511987398289%3AJQABAAAAAAARNwAAuAYCKg%3A9.1%7C4011587096.-1.0%7C4005521594.-1.0%7C295172.647847.***&renew=no&userip=fe80%3A%3A1&tvmTicket=3%3Aserv%3ACNwdEKH109cFIgcI5Il6EOAB%3A***&dbfields=userinfo.lang.uid&dbfields=subscription.suid.59&dbfields=subscription.login.669&attributes=1011&aliases=1%2C5%2C24'
                );
                done();
            });
        });

        it('должна класть user_ticket в req.tvmTickets', (done) => {
            const req = Object.assign({}, DEFAULT_REQ);
            getAuthMiddleware(DEFAULT_CONFIG)(req, {}, () => {
                expect(req.tvmTickets.userTicket).toBe(
                    // eslint-disable-next-line max-len
                    '3:user:CNYdENi60dcFGmUKBgiRx-j0DgoGCPWDsPUOCgYIutH99Q4KBgiq0af3DgoGCNDW0vcOCgYIzNua-A4KBgjm25r4DgoGCJjs7_gOCgkI74XvhKr3gAIQutH99Q4aDGJiOnNlc3Npb25pZCDkiXooAQ:UUNcQb4ZI_qQZEM-lkEszPh-3xphRc6HtC9RNXkhumUA5akAT99ieZq-YVynl0XjA7ffrfeHE3FvSGg_qF6b_nCYfJA_dwVFCvB7msYFUHFx4PS6n6UxkH6qxph7r8vH1rRI_QSoiRz8_NcuxwzvL4zKGJ1KEWKdZCuvyioCl8Y'
                );
                done();
            }
            );
        });

        it('должна класть user в req', (done) => {
            const req = Object.assign({}, DEFAULT_REQ);
            getAuthMiddleware(DEFAULT_CONFIG)(req, {}, () => {
                expect(req.user).toMatchSnapshot();
                done();
            });
        });

        it('должна выбирать пользователя, заданного в defaultUid если он есть', (done) => {
            const req = Object.assign({
                defaultUid: '4008306858'
            }, DEFAULT_REQ);
            getAuthMiddleware(DEFAULT_CONFIG)(req, {}, () => {
                expect(req.user).toMatchSnapshot();
                done();
            });
        });

        it('должна выбирать выбирать пользователя стандартным образом, если пользователь с defaultUid не найден', (done) => {
            const req = Object.assign({
                defaultUid: '0'
            }, DEFAULT_REQ);
            getAuthMiddleware(DEFAULT_CONFIG)(req, {}, () => {
                expect(req.user).toMatchSnapshot();
                done();
            });
        });
    });

    describe('Авторизация не найдена', () => {
        beforeEach(() => {
            ask.mockImplementation(() => Promise.resolve({
                data: JSON.stringify({
                    status: { value: 'INVALID', id: 5 },
                    error: 'there is no data for given keyspace'
                })
            }));
        });
        it('не должна обсфуцировать сессионные куки вида noauth:1510039387', (done) => {
            const req = Object.assign({}, DEFAULT_REQ, {
                cookies: {
                    Session_id: 'noauth:1510039387',
                    sessionid2: 'noauth:1510039387'
                }
            });
            getAuthMiddleware(DEFAULT_CONFIG)(req, {}, () => {
                expect(lastLoggerParams.serviceUrl).toBe(
                    // eslint-disable-next-line max-len
                    'https://blackbox.yandex.net/blackbox?method=sessionid&multisession=yes&host=disk.yandex.ru&sessionid=noauth%3A1510039387&sslsessionid=noauth%3A1510039387&renew=no&userip=fe80%3A%3A1&tvmTicket=3%3Aserv%3ACNwdEKH109cFIgcI5Il6EOAB%3A***&dbfields=userinfo.lang.uid&dbfields=subscription.suid.59&dbfields=subscription.login.669&attributes=1011&aliases=1%2C5%2C24'
                );
                done();
            });
        });

        it('должна класть req объект для неавторизованного пользователя', (done) => {
            const req = Object.assign({}, DEFAULT_REQ);
            getAuthMiddleware(DEFAULT_CONFIG)(req, { cookies: {} }, () => {
                expect(req.user).toMatchSnapshot();
                done();
            });
        });

        it('должна логгировать ошибку', (done) => {
            /* eslint-disable no-console */
            const req = Object.assign({}, DEFAULT_REQ);
            const originalLog = console.log;
            console.log = () => {};
            ask.mockImplementation(() => Promise.resolve({}));
            getAuthMiddleware(DEFAULT_CONFIG)(req, {}, () => {
                expect(lastLoggerParams.level).toBe('error');
                console.log = originalLog;
                done();
            });
        });

        it('должна фолбэчится на неавторизиванного пользователя в случае ошибки', (done) => {
            /* eslint-disable no-console */
            const req = Object.assign({}, DEFAULT_REQ);
            const originalLog = console.log;
            console.log = () => {};
            ask.mockImplementation(() => Promise.resolve({}));
            getAuthMiddleware(DEFAULT_CONFIG)(req, {}, () => {
                expect(req.user).toMatchSnapshot();
                console.log = originalLog;
                done();
            });
            /* eslint-enable no-console */
        });
    });
});
