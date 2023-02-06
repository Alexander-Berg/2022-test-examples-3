import test from 'ava';
import cookieParser from 'cookie-parser';
import express from 'express';
import nock from 'nock';
import supertest from 'supertest';

import { expressBlackbox } from './index.js';

const BLACKBOX_API = 'http://mockbox.yandex.ru';

function mockBlackbox({ headers, query }, status, result) {
    nock(BLACKBOX_API, {
        reqheaders: Object.assign({}, headers),
    })
        .persist()
        .get(actualUri => actualUri.startsWith('/blackbox'))
        .query(actualQuery => Object.entries(query).every(([key, val]) => actualQuery[key] === val))

        .reply(status, result);
}

test.cb('returns error without cookie-parser', t => {
    const req = {};
    expressBlackbox({ api: BLACKBOX_API })(req, {}, err => {
        t.truthy(/cookie-parser/.test(err.message));
        t.end();
    });
});

test.cb('throws error without api option', t => {
    t.throws(
        () => {
            expressBlackbox();
        },
        { message: /api option is required./ },
    );
    t.end();
});

test.cb('returns invalid status without cookies', t => {
    const req = {
        cookies: {},
    };

    expressBlackbox({ api: BLACKBOX_API })(req, {}, err => {
        t.falsy(err);
        t.is(req.blackbox.status, 'INVALID_PARAMS');
        t.end();
    });
});

test('returns data about user', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                attributes: '1007,1008',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                sessionid: sessionId,
            },
        },
        200,
        {
            error: 'OK',
            display_name: {
                // eslint-disable-line camelcase
                name: 'Звездный Лорд',
            },
            attributes: {
                '1007': 'Блекбоксович Экспресс',
                '1008': 'express-blackbox',
            },
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {
                    fio: '1007',
                    login: '1008',
                },
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.login, 'express-blackbox');
            t.is(req.blackbox.fio, 'Блекбоксович Экспресс');
            t.is(req.blackbox.displayName, 'Звездный Лорд');

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('returns phone attributes data', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                attributes: '1007',
                getphones: 'all',
                phone_attributes: '102', // eslint-disable-line camelcase
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                sessionid: sessionId,
            },
        },
        200,
        {
            error: 'OK',
            phones: [
                {
                    id: '1',
                    attributes: {
                        '102': '+79001002030',
                    },
                },
            ],
            attributes: {
                '1007': 'FIO',
            },
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {
                    fio: '1007',
                },
                phones: {
                    kind: 'all',
                    attributes: {
                        e164: '102',
                    },
                },
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.fio, 'FIO');
            t.deepEqual(req.blackbox.phones, [
                {
                    id: '1',
                    e164: '+79001002030',
                },
            ]);

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('returns correct answer w/o dbfields when fields are false', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                sessionid: sessionId,
            },
        },
        200,
        {
            error: 'OK',
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {},
                fields: false,
            }),
        )
        .use((req, res) => {
            t.is(req.blackbox.dbfields, undefined);

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('returns errors from blackbox', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                sessionid: sessionId,
            },
        },
        200,
        {
            status: {
                value: 'INVALID_PARAMS',
                id: 2,
            },
            error: 'BlackBox error: Missing userip argument',
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {},
            }),
        )
        .use((req, res) => {
            t.is(req.blackbox.status, 'INVALID_PARAMS');
            t.is(req.blackbox.error, 'BlackBox error: Missing userip argument');

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('returns errors from requests', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                sessionid: sessionId,
            },
        },
        200,
        {
            status: {
                value: 'REQUEST_ERROR',
                id: -1,
            },
            error: 'Super error',
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://alkjshdlakjhdlakd.foo.bar.zot',
                attributes: {},
                timeout: 999999,
            }),
        )
        .use((req, res) => {
            t.is(req.blackbox.status, 'REQUEST_ERROR');
            t.truthy(/getaddrinfo ENOTFOUND/.test(req.blackbox.error));

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('returns ticket from TVM', async t => {
    const sessionId = String(Math.random());
    const sslSessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                sessionid: sessionId,
                sslsessionid: sslSessionId,
                getticket: 'yes',
                client_id: '11', // eslint-disable-line camelcase
                consumer: 'express-blackbox',
            },
        },
        200,
        {
            ticket: 'TICKET!',
            error: 'OK',
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                getticket: 'yes',
                attributes: {},
                client_id: '11', // eslint-disable-line camelcase
                client_secret: 'TVM_SECRET', // eslint-disable-line camelcase
                consumer: 'express-blackbox',
            }),
        )
        .use((req, res) => {
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.ticket, 'TICKET!');

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId};sessionid2=${sslSessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('returns user by oauth (header)', async t => {
    const oAuthToken = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'oauth',
                format: 'json',
                attributes: '1007,1008',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                oauth_token: oAuthToken, // eslint-disable-line camelcase
            },
        },
        200,
        {
            error: 'OK',
            display_name: {
                // eslint-disable-line camelcase
                name: 'Звездный Лорд',
            },
            attributes: {
                '1007': 'Блекбоксович Экспресс',
                '1008': 'express-blackbox',
            },
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {
                    fio: '1007',
                    login: '1008',
                },
                oauth: true,
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.login, 'express-blackbox');
            t.is(req.blackbox.fio, 'Блекбоксович Экспресс');
            t.is(req.blackbox.displayName, 'Звездный Лорд');

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Authorization', `Oauth ${oAuthToken}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('returns user by oauth', async t => {
    const oAuthToken = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'oauth',
                format: 'json',
                attributes: '1007,1008',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                oauth_token: oAuthToken, // eslint-disable-line camelcase
            },
        },
        200,
        {
            error: 'OK',
            display_name: {
                // eslint-disable-line camelcase
                name: 'Звездный Лорд',
            },
            attributes: {
                '1007': 'Блекбоксович Экспресс',
                '1008': 'express-blackbox',
            },
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {
                    fio: '1007',
                    login: '1008',
                },
                oauth: oAuthToken,
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.login, 'express-blackbox');
            t.is(req.blackbox.fio, 'Блекбоксович Экспресс');
            t.is(req.blackbox.displayName, 'Звездный Лорд');

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('returns tvm ticket via oauth', async t => {
    const oAuthToken = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'oauth',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                oauth_token: oAuthToken, // eslint-disable-line camelcase,
                getticket: 'yes',
                client_id: '11', // eslint-disable-line camelcase
                consumer: 'express-blackbox',
            },
        },
        200,
        {
            error: 'OK',
            display_name: {
                // eslint-disable-line camelcase
                name: 'Звездный Лорд',
            },
            ticket: 'TICKET!',
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                oauth: oAuthToken,
                getticket: 'yes',
                attributes: {},
                client_id: '11', // eslint-disable-line camelcase
                client_secret: 'TVM_SECRET', // eslint-disable-line camelcase
                consumer: 'express-blackbox',
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.displayName, 'Звездный Лорд');
            t.is(req.blackbox.ticket, 'TICKET!');

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('tvm2', async t => {
    const sessionId = String(Math.random());
    const xYaServiceTicket = String(Math.random());
    const userTicket = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                get_user_ticket: 'yes', // eslint-disable-line camelcase
            },
            headers: {
                'X-Ya-Service-Ticket': xYaServiceTicket,
            },
        },
        200,
        {
            error: 'OK',
            user_ticket: userTicket, // eslint-disable-line camelcase
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                getServiceTicket() {
                    return xYaServiceTicket;
                },
                attributes: {},
            }),
        )
        .use((req, res) => {
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.userTicket, userTicket);

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Host', 'yandex.ru')
        .set('Cookie', `Session_id=${sessionId}`)
        .expect(200);
});

test('tvm2 with async getServiceTicket', async t => {
    const sessionId = String(Math.random());
    const xYaServiceTicket = String(Math.random());
    const userTicket = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                get_user_ticket: 'yes', // eslint-disable-line camelcase
            },
            headers: {
                'X-Ya-Service-Ticket': xYaServiceTicket,
            },
        },
        200,
        {
            error: 'OK',
            user_ticket: userTicket, // eslint-disable-line camelcase
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                getServiceTicket() {
                    return Promise.resolve(xYaServiceTicket);
                },
                attributes: {},
            }),
        )
        .use((req, res) => {
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.userTicket, userTicket);

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Host', 'yandex.ru')
        .set('Cookie', `Session_id=${sessionId}`)
        .expect(200);
});

test('getYaboxOptions()', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                sessionid: sessionId,
            },
            headers: {
                Cookie: 'some_cookie=foobar',
            },
        },
        200,
        {
            error: 'OK',
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {},
                getYaboxOptions(req) {
                    return {
                        headers: {
                            Cookie: `some_cookie=${req.cookies.some_cookie}`,
                        },
                    };
                },
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'OK');

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId};some_cookie=foobar`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('multisession returns all users', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                sessionid: sessionId,
                multisession: 'yes',
            },
        },
        200,
        {
            error: 'OK',
            default_uid: '2',
            users: [
                {
                    id: '1',
                    uid: {
                        value: '1'
                    },
                    display_name: {
                        name: 'Первая голова',
                    },
                    attributes: {
                        '1007': 'Змей Горыныч',
                        '1008': 'first@gorini.ch',
                    },
                },
                {
                    id: '2',
                    uid: {
                        value: '2'
                    },
                    display_name: {
                        name: 'Главная голова',
                    },
                    attributes: {
                        '1007': 'Змей Горыныч',
                        '1008': 'main@gorini.ch',
                    },
                },
                {
                    id: '3',
                    uid: {
                        value: '3'
                    },
                    display_name: {
                        name: 'Правильная голова',
                    },
                    attributes: {
                        '1007': 'Змей Горыныч',
                        '1008': 'right@gorini.ch',
                    },
                }
            ]
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {
                    fio: '1007',
                    login: '1008',
                },
                multisession: 'yes',
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.uid, '2');
            t.is(req.blackbox.login, 'main@gorini.ch');
            t.is(req.blackbox.fio, 'Змей Горыныч');
            t.is(req.blackbox.displayName, 'Главная голова');

            t.true(Array.isArray(req.blackbox.users));
            t.is(req.blackbox.users.length, 2);

            t.is(req.blackbox.users[0].uid, '1');
            t.is(req.blackbox.users[0].login, 'first@gorini.ch');
            t.is(req.blackbox.users[0].fio, 'Змей Горыныч');
            t.is(req.blackbox.users[0].displayName, 'Первая голова');

            t.is(req.blackbox.users[1].uid, '3');
            t.is(req.blackbox.users[1].login, 'right@gorini.ch');
            t.is(req.blackbox.users[1].fio, 'Змей Горыныч');
            t.is(req.blackbox.users[1].displayName, 'Правильная голова');

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('multisession fallback to first user', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                sessionid: sessionId,
                multisession: 'yes',
            },
        },
        200,
        {
            error: 'OK',
            users: [
                {
                    display_name: {
                        name: 'Звездный Лорд',
                    },
                    attributes: {
                        '1007': 'Блекбоксович Экспресс',
                        '1008': 'express-blackbox',
                    },
                }
            ]
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {
                    fio: '1007',
                    login: '1008',
                },
                multisession: 'yes',
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'OK');
            t.is(req.blackbox.login, 'express-blackbox');
            t.is(req.blackbox.fio, 'Блекбоксович Экспресс');
            t.is(req.blackbox.displayName, 'Звездный Лорд');

            t.true(Array.isArray(req.blackbox.users));
            t.is(req.blackbox.users.length, 0);

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});

test('multisession error result', async t => {
    const sessionId = String(Math.random());

    mockBlackbox(
        {
            query: {
                method: 'sessionid',
                format: 'json',
                regname: 'yes',
                host: 'yandex.ru',
                userip: '127.0.0.1',
                sessionid: sessionId,
                multisession: 'yes',
            },
        },
        200,
        {
            error: 'Cookie was got in wrong environment',
            status: { value: 'INVALID', id: 5 },
        },
    );

    const app = express()
        .enable('trust proxy')
        .use(cookieParser())
        .use(
            expressBlackbox({
                api: 'http://mockbox.yandex.ru',
                attributes: {
                    fio: '1007',
                    login: '1008',
                },
                multisession: 'yes',
            }),
        )
        .use((req, res) => {
            t.truthy(req.blackbox.raw);
            t.is(req.blackbox.error, 'Cookie was got in wrong environment');
            t.is(req.blackbox.status, 'INVALID');
            t.true(Array.isArray(req.blackbox.users));
            t.is(req.blackbox.users.length, 0);

            res.sendStatus(200);
        });

    await supertest(app)
        .get('/')
        .set('X-Forwarded-For', '127.0.0.1')
        .set('Cookie', `Session_id=${sessionId}`)
        .set('Host', 'yandex.ru')
        .expect(200);
});
