'use strict';

const router = require('./index.js');
const got = require('got');
const nock = require('nock');
const { express } = require('@yandex-int/duffman');
const authMock = require('../../test/mock/auth.json');
const Core = jest.requireActual('../helpers/extra-core.js');

let core;
let request;
let server;
let akitaNock;
let metaNock;
let sendbernarNock;

const mockHideParamInLog = jest.fn();
jest.mock('../helpers/extra-core.js', () => class Core extends jest.requireActual('../helpers/extra-core.js') {
    hideParamInLog(...args) {
        return mockHideParamInLog(...args);
    }
});

const ApiError = require('../helpers/api-error.js');
const mockGetAuthTvm = jest.fn();
jest.mock('../helpers/get-auth-tvm.js', () => () => mockGetAuthTvm());

beforeEach((done) => {
    const routePath = '/api/mobile/v1';
    const app = express();

    const req = {
        cookies: {},
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/apimobile/v1',
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

    app.set('port', 0);
    app.set('x-powered-by', false);
    app.use(routePath, router);

    server = app.listen(done);
    const port = server.address().port;

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
    sendbernarNock = nock(core.config.services.sendbernar).filteringPath((path) => path.replace(/\?.*/, ''));
});

afterEach((done) => {
    server.close(done);
    nock.cleanAll();
});

describe('на несуществующий метод', () => {
    it('должен вернуть 200', async () => {
        const resp = await request({ headers: { 'x-api-method': 'trololo' } });

        expect(resp.statusCode).toEqual(200);
    });

    it('должен вернуть PERM_FAIL(3)', async () => {
        const resp = await request({ headers: { 'x-api-method': 'trololo' } });

        const body = JSON.parse(resp.body);
        expect(body.status).toEqual({
            status: 3,
            phrase: 'No such method'
        });
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

    it('должен вернуть status OK(1)', async () => {
        const resp = await request({ headers });

        const body = JSON.parse(resp.body);
        expect(body.status.status).toEqual(1);
    });

    it('должен добвить заголовок x-mmapi-status: 1', async () => {
        const resp = await request({ headers });

        expect(resp.headers).toContainEntry([ 'x-mmapi-status', '1' ]);
    });
});

describe('прячет параметры', () => {
    const headers = { authorization: 'Oauth 00000' };

    beforeEach(() => {
        akitaNock.get('/auth').reply(200, authMock);
        sendbernarNock.post('/save_draft').reply(200, {});
        sendbernarNock.post('/send_message').reply(200, {});
    });

    [ 'send', 'store' ].forEach((method) => {
        it(`${method}`, async () => {
            headers['x-api-method'] = method;

            await request({
                headers,
                json: true
            });

            expect(mockHideParamInLog).toHaveBeenCalledWith(expect.any(Object), null, 'send', '[HiddenParam `send`]');
            expect(mockHideParamInLog).toHaveBeenCalledWith(expect.any(Object), null, 'subj', '[HiddenParam `subj`]');
        });
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

    it('должен ответить 403', async () => {
        expect.assertions(1);

        mockGetAuthTvm.mockRejectedValue(new ApiError(403));

        try {
            await request({ headers });
        } catch (e) {
            expect(e.statusCode).toEqual(403);
        }
    });

    it('должен ответить 200', async () => {
        mockGetAuthTvm.mockResolvedValue({ uid: 42 });

        const resp = await request({ headers });

        expect(resp.statusCode).toEqual(200);
    });

    it('должен вернуть status OK(1)', async () => {
        mockGetAuthTvm.mockResolvedValue({ uid: 42 });

        const resp = await request({ headers });

        const body = JSON.parse(resp.body);
        expect(body.status.status).toEqual(1);
    });
});

describe('если нет авторизации (code 2005)', () => {
    const headers = {
        'authorization': 'Oauth 00000',
        'x-api-method': 'reset_fresh'
    };

    beforeEach(() => {
        akitaNock.get('/auth').reply(200, {
            error: {
                code: 2005,
                message: 'BlackBox error: Sign don\'t match',
                reason: 'internal error'
            }
        });
    });

    it('должен ответить 200', async () => {
        const resp = await request({ headers });

        expect(resp.statusCode).toEqual(200);
    });

    it('должен вернуть PERM_FAIL(3)', async () => {
        const resp = await request({ headers });

        const body = JSON.parse(resp.body);
        expect(body.status).toContainEntry([ 'status', 3 ]);
        expect(body.status.phrase).toInclude('PERM_FAIL AUTH_ERROR 2005');
    });
});

describe('если акита не отвечает', () => {
    const headers = {
        'authorization': 'Oauth 00000',
        'x-api-method': 'reset_fresh'
    };

    beforeEach(() => {
        akitaNock.get('/auth').delayConnection(50000).reply(500, { error: { code: '2001' } });
    });

    it('для xlist статус в массиве', async () => {
        headers['x-api-method'] = 'xlist';

        const resp = await request({ headers });

        const body = JSON.parse(resp.body);
        expect(Array.isArray(body)).toBe(true);
        expect(body[0].status.status).toEqual(2);
        expect(resp.headers).toContainEntry([ 'x-mmapi-status', '2' ]);
    });

    it('для messages статус в массиве', async () => {
        headers['x-api-method'] = 'messages';
        const requests = [ {}, {} ];

        const resp = await request({ headers, json: true, body: { requests } });

        const body = resp.body;
        expect(body).toBeArray();
        expect(body).toHaveLength(requests.length);
        expect(body[0].header.error).toBe(2);
    });

    [
        'mark_read',
        'mark_unread',
        'mark_with_label',
        'move_to_folder',
        'foo',
        'antifoo',
        'delete_items',
        'clear_folder',
        'set_parameters',
        'set_settings'
    ].forEach((method) => {
        it(`${method}`, async () => {
            headers['x-api-method'] = method;

            const resp = await request({ headers, json: true });

            const body = resp.body;
            expect(body.status).toBe(2);
            expect(body.phrase).toInclude('HTTP_ERROR');
        });
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

    it('должен ответить 200', async () => {
        const resp = await request({ headers });

        expect(resp.statusCode).toEqual(200);
    });

    it('должен вернуть PERM_FAIL(3)', async () => {
        const resp = await request({ headers });

        const body = JSON.parse(resp.body);
        expect(body.status).toContainEntry([ 'status', 3 ]);
        expect(body.status.phrase).toInclude('2001');
    });

    it('должен добвить заголовок x-mmapi-status: 3', async () => {
        const resp = await request({ headers });

        expect(resp.headers).toContainEntry([
            'x-mmapi-status', '3'
        ]);
    });

    describe('отвечает правильно без авторизации', () => {
        it('для xlist статус в массиве', async () => {
            headers['x-api-method'] = 'xlist';

            const resp = await request({ headers });

            const body = JSON.parse(resp.body);
            expect(Array.isArray(body)).toBe(true);
            expect(body[0].status.status).toBe(3);
            expect(resp.headers).toContainEntry([
                'x-mmapi-status', '3'
            ]);
        });

        it('для messages статус в массиве', async () => {
            headers['x-api-method'] = 'messages';
            const requests = [ {}, {} ];

            const resp = await request({ headers, json: true, body: { requests } });

            const body = resp.body;
            expect(body).toBeArray();
            expect(body).toHaveLength(requests.length);
            expect(body[0].header.error).toBe('2001');
        });

        [
            'mark_read',
            'mark_unread',
            'mark_with_label',
            'move_to_folder',
            'foo',
            'antifoo',
            'delete_items',
            'clear_folder',
            'set_parameters',
            'set_settings'
        ].forEach((method) => {
            it(`${method}`, async () => {
                headers['x-api-method'] = method;

                const resp = await request({ headers, json: true });

                const body = resp.body;
                expect(body.status).toBe(3);
                expect(body.phrase).toInclude('2001');
            });
        });
    });
});
