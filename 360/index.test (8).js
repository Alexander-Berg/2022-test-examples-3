'use strict';

const router = require('./index.js');
const got = require('got');
const nock = require('nock');
const express = require('@yandex-int/duffman').express;
const authMock = require('../../test/mock/auth.json');
const Core = require('../../routes/helpers/extra-core.js');

let core;
let request;
let server;
let akitaNock;
let metaNock;

const ApiError = require('../helpers/api-error.js');
const mockGetAuthTvm = jest.fn();
jest.mock('../helpers/get-auth-tvm.js', () => () => mockGetAuthTvm());

beforeEach((done) => {
    const routePath = '/mobileapi/v2';
    const app = express();

    app.set('port', 0);
    app.set('x-powered-by', false);
    app.use(routePath, router);

    server = app.listen(done);
    const port = server.address().port;

    const req = {
        cookies: {},
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/u2709/api/models',
            'x-request-id': '12345',
            'x-real-ip': '2a02:6b8::25'
        },
        query: {
            uuid: 'deadbeef42',
            client: 'iphone',
            client_version: '10.0.3'
        },
        body: {}
    };
    const res = {
        set: jest.fn(),
        on: jest.fn(),
        send: jest.fn(),
        status: jest.fn()
    };
    core = new Core(req, res);

    request = (options) => {
        options = options || {};
        options.followRedirect = false;
        options.agent = false;

        options.query = Object.assign(options.query || {}, {
            uuid: '0123456789',
            client: 'iphone',
            client_version: '10.0.3'
        });
        options.headers = Object.assign(options.headers || {}, {
            'x-real-ip': 'dead::beef',
            'x-request-id': 'ababbabababba',
            'x-original-host': 'test',
            'x-https-request': 'yes',
            'user-agent': 'aser ugent'
        });

        return got(`http://localhost:${port}${routePath}`, options);
    };

    akitaNock = nock(core.config.services.akita).filteringPath((path) => path.replace(/\?.*/, ''));
    metaNock = nock(core.config.services.meta).filteringPath((path) => path.replace(/\?.*/, ''));
});

afterEach((done) => {
    server.close(done);
    nock.cleanAll();
});

describe('на несуществующий метод', () => {
    it('должен вернуть 501', async () => {
        expect.assertions(1);

        try {
            await request({ headers: { 'x-api-method': 'trololo' } });
        } catch (err) {
            expect(err.statusCode).toEqual(501);
        }
    });

    it('должен вернуть message: No such method', async () => {
        expect.assertions(1);

        try {
            await request({ headers: { 'x-api-method': 'trololo' } });
        } catch (err) {
            const body = JSON.parse(err.response.body);
            expect(body.message).toEqual('No such method');
        }
    });
});

describe('на существующий метод', () => {
    const headers = {
        'authorization': 'Oauth 00000',
        'x-api-method': 'reset_fresh'
    };

    beforeEach(() => {
        akitaNock.get('/auth').reply(200, authMock);
        metaNock.get('/reset_fresh_counter').reply(200, {});
    });

    it('должен вернуть 200', async () => {
        const resp = await request({ headers });

        expect(resp.statusCode).toEqual(200);
    });
});

describe('tvm авторизация', () => {
    const headers = {
        'x-ya-user-ticket': '3:user:ticket',
        'x-ya-service-ticket': '3:service:ticket',
        'x-api-method': 'reset_fresh'
    };

    beforeEach(() => {
        metaNock.get('/reset_fresh_counter').reply(200, {});
    });

    it('должен вернуть 403', async () => {
        mockGetAuthTvm.mockRejectedValue(new ApiError(403));

        try {
            await request({ headers });
        } catch (e) {
            expect(e.statusCode).toEqual(403);
        }
    });

    it('должен вернуть 200', async () => {
        mockGetAuthTvm.mockResolvedValue({ uid: 42 });

        const resp = await request({ headers });

        expect(resp.statusCode).toEqual(200);
    });
});

describe('если нет авторизации', () => {
    const headers = {
        'authorization': 'Oauth 00000',
        'x-api-method': 'reset_fresh'
    };

    beforeEach(() => {
        akitaNock.get('/auth').reply(200, { error: { code: '2001' } });
    });

    it('должен ответить 401', async () => {
        expect.assertions(1);

        try {
            await request({ headers });
        } catch (err) {
            expect(err.statusCode).toEqual(401);
        }
    });

    it('должен вернуть message', async () => {
        expect.assertions(1);

        try {
            await request({ headers });
        } catch (err) {
            const body = JSON.parse(err.response.body);
            expect(body).toContainEntry([
                'message', 'AUTH_ERROR AUTH_NO_AUTH: 2001'
            ]);
        }
    });
});

describe('если акита отвечает ошибкой', () => {
    describe('500', () => {
        const headers = {
            'authorization': 'Oauth 00000',
            'x-api-method': 'reset_fresh'
        };

        beforeEach(() => {
            akitaNock.get('/auth').reply(500, {});
        });

        it('должен ответить 500', async () => {
            expect.assertions(1);

            try {
                await request({ headers });
            } catch (err) {
                expect(err.statusCode).toEqual(500);
            }
        });
    });

    describe('400', () => {
        const headers = {
            'authorization': 'Oauth 00000',
            'x-api-method': 'reset_fresh'
        };

        beforeEach(() => {
            akitaNock.get('/auth').reply(400, {});
        });

        it('должен ответить 500', async () => {
            expect.assertions(1);

            try {
                await request({ headers });
            } catch (err) {
                expect(err.statusCode).toEqual(500);
            }
        });
    });

    describe('http error', () => {
        const headers = {
            'authorization': 'Oauth 00000',
            'x-api-method': 'reset_fresh'
        };

        beforeEach(() => {
            akitaNock.get('/auth').delayConnection();
        });

        it('должен ответить 500', async () => {
            expect.assertions(1);

            try {
                await request({ headers });
            } catch (err) {
                expect(err.statusCode).toEqual(500);
            }
        });
    });
});
