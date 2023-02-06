'use strict';

jest.mock('@ps-int/mail-lib/middlewares/middleware-tvm2.js');
jest.mock('@yandex-int/yandex-geobase', () => ({
    default: {
        v6: () => ({
            getRegionByIp: () => 213
        })
    }
}));
jest.unmock('@yandex-int/duffman');

const got = require('got');
const { express, middleware: { cookieParser } } = require('@yandex-int/duffman');
const route = require('../index.js');
const nock = require('nock');
const httpMock = require('node-mocks-http');

const yandexuid = '12345678901234567890';

const SharedApiCore = require('../../helpers/shared-api-core/index.js');
const routePath = '/web-api/shared';
const routePathSharedModels = `${routePath}/models`;
const req = httpMock.createRequest({
    headers: {
        'x-original-host': 'mail.yandex.ru',
        'x-original-uri': routePathSharedModels,
        'x-real-ip': '2a02:6b8::25',
        'x-https-request': 'yes',
        'cookie': `yandexuid=${yandexuid}`
    },
    body: {}
});
const res = httpMock.createResponse();

const setAuth = (core) => {
    core.auth.set({
        mdb: 'mdb1',
        suid: '34',
        timezone: 'Europe/Moscow',
        tz_offset: -180,
        uid: '12',
        users: []
    });
};

let core;
let server;
let genCkey;
let request;

const commonHeaders = {
    'x-real-ip': '123.123.123.123',
    'x-real-port': '6666',
    'x-request-id': '12345'
};
let nocks = [];

beforeEach((done) => {
    const app = express();

    app.set('etag', false);
    app.set('port', 0);
    app.set('x-powered-by', false);
    app.use(cookieParser);
    app.use(routePath, route);

    server = app.listen(done);
    const port = server.address().port;

    // required for stable ckey generation
    jest.spyOn(Date, 'now').mockImplementation(() => 1540487881000);
    genCkey = () => {
        core.ckey.yandexuid = yandexuid;
        return core.ckey.renew();
    };

    request = (options = {}) => {
        options.agent = false;
        options.json = true;

        options.headers = {
            'x-original-host': 'test',
            'x-https-request': 'yes',
            'cookie': 'Session_id=FAKE; yandexuid=' + yandexuid,
            ...(options.headers || {})
        };

        options.body = {
            _exp: '1,2,3',
            _eexp: '1,2',
            _locale: 'ru',
            ...(options.body || {})
        };

        return got(`http://localhost:${port}${routePathSharedModels}`, options);
    };

    core = new SharedApiCore(req, res);
    setAuth(core);
    nock.enableNetConnect(`localhost:${port}`);

    nocks.push(require('./__nocks__/auth.js')(core));
});

afterEach((done) => {
    nocks.forEach((nock) => nock.done());
    nock.cleanAll();
    nocks = [];
    jest.resetModules();
    server.close(done);
});

test('запрос unsafe моделей без ckey, даёт ошибку в ответе', async () => {
    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                { name: 'get-domenator-domain-status/v1' }
            ]
        }
    });

    expect(body).toMatchSnapshot();
});

test('запрашивает модели с нужными хедерами', async () => {
    nocks.push(
        nock(core.config.services.domenator, {
            reqheaders: {
                ...commonHeaders,
                'X-Yandex-ExpBoxes': '1,2,3',
                'X-Yandex-EnabledExpBoxes': '1,2',
                'X-Yandex-ClientType': 'CLIENT',
                'X-Yandex-ClientVersion': 'CLIENT_VERSION',
                'x-request-id': '12345'
            }
        })
            .filteringPath((path) => path.replace(/\?.*/, ''))
            .get('/api/domains/status/12')
            .reply(200, {})
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                { name: 'get-domenator-domain-status/v1' }
            ],
            _ckey: genCkey(),
            _service: 'CLIENT',
            _version: 'CLIENT_VERSION'
        }
    });

    expect(body.models[0].status).toBe('ok');
});

test('устанавливает нужные хеадеры', async () => {
    const { headers } = await request({
        body: {
            _ckey: genCkey()
        }
    });

    expect(headers).toContainEntries([
        [ 'x-response-with', 'YMail' ],
        [ 'content-type', 'application/json; charset=utf-8' ],
        [ 'cache-control', 'max-age=0, must-revalidate, proxy-revalidate, no-cache, no-store, private' ],
        [ 'expires', 'Thu, 01 Jan 1970 00:00:01 GMT' ],
        [ 'pragma', 'no-cache' ]
    ]);
});

test('устанавливает нужные параметры в тело ответа', async () => {
    const { body } = await request({
        headers: commonHeaders,
        body: {
            _ckey: genCkey()
        }
    });

    expect(body).toContainAllKeys([
        'models',
        'timestamp',
        'uid',
        'cookieRenew',
        'versions'
    ]);
});

test('если авторизация через OAuth, то проверять ckey не нужно', async () => {
    nocks.push(
        nock(core.config.services.domenator)
            .filteringPath((path) => path.replace(/\?.*/, ''))
            .get('/api/domains/status/12')
            .reply(200, {})
    );

    const { body } = await request({
        headers: {
            ...commonHeaders,
            authorization: 'OAuth XXX',
            cookie: ''
        },
        body: {
            models: [
                { name: 'get-domenator-domain-status/v1' }
            ]
        }
    });

    expect(body).toMatchSnapshot();
});

test('не запрашивает internal модели', async () => {
    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                { name: 'auth' },
                { name: 'billing-user-services' },
                { name: 'is-user-recently-confirmed-phone' },
                { name: 'settings' }
            ],
            _ckey: genCkey()
        }
    });

    expect(body).toMatchSnapshot();
});

test('domenator-register-domain/v1', async () => {
    nocks.push(require('./__nocks__/bb.js')(core));
    nocks.push(nock('http://ps-billing')
        .filteringPath((path) => path.replace(/\?.*/, ''))
        .get('/v1/users/services')
        .reply(200, require('./__nocks__/billing.js')));
    nocks.push(nock('http://domenator')
        .filteringPath((path) => path.replace(/\?.*/, ''))
        .post('/api/domains/register/12')
        .reply(200, {}));

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'domenator-register-domain/v1',
                    params: {
                        domain: 'foo',
                        login: 'bar'
                    }
                }
            ],
            _ckey: genCkey()
        }
    });

    expect(body).toMatchSnapshot();
});

test('get-abook-vcard-by-email/v0', async () => {
    nocks.push(
        nock('http://aceventura')
            .filteringPath((path) => path.replace(/\?.*/, ''))
            .get('/v1/search_by_email')
            .reply(200, {})
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'get-abook-vcard-by-email/v0',
                    params: {
                        email: 'foo@bar.com'
                    }
                }
            ]
        }
    });

    expect(body).toMatchSnapshot();
});

test('get-domenator-domain-status/v1', async () => {
    nocks.push(
        nock(core.config.services.domenator)
            .filteringPath((path) => path.replace(/\?.*/, ''))
            .get('/api/domains/status/12')
            .reply(200, {})
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'get-domenator-domain-status/v1',
                    params: {}
                }
            ],
            _ckey: genCkey()
        }
    });

    expect(body).toMatchSnapshot();
});

test('get-domenator-domains-suggest/v1', async () => {
    nocks.push(
        nock(core.config.services.domenator)
            .filteringPath((path) => path.replace(/\?.*/, ''))
            .get('/api/domains/suggest/12')
            .reply(200, {})
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'get-domenator-domains-suggest/v1',
                    params: {}
                }
            ],
            _ckey: genCkey()
        }
    });

    expect(body).toMatchSnapshot();
});

test('get-event-intervals-by-email/v0', async () => {
    nocks.push(
        nock(core.config.services.calendar)
            .filteringPath((path) => path.replace(/\?.*/, ''))
            .post('/get-availability-intervals')
            .reply(200, {
                subjectAvailabilities: [
                    { email: 'foo@bar.com', status: 'ok', intervals: [] }
                ]
            })
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'get-event-intervals-by-email/v0',
                    params: {
                        email: 'foo@bar.com',
                        from: '42',
                        to: '42'
                    }
                }
            ]
        }
    });

    expect(body).toMatchSnapshot();
});

test('get-staff-gap-by-email/v0', async () => {
    nocks.push(
        nock(core.config.services['staff-gap'])
            .filteringPath((path) => path.replace(/\?.*/, ''))
            .post('/export_gaps')
            .reply(200, require('./__nocks__/gaps.js'))
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'get-staff-gap-by-email/v0',
                    params: {
                        login: 'foo',
                        only_one: false
                    }
                }
            ]
        }
    });

    expect(body).toMatchSnapshot();
});

test('get-memories', async () => {
    nocks.push(require('./__nocks__/memories.js')(core));

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'get-memories/v1',
                    params: {
                        image_size: '100x100'
                    }
                }
            ],
            _ckey: genCkey()
        }
    });

    expect(body).toMatchSnapshot();
});

test('get-space-widget-data', async () => {
    nocks.push(
        nock(core.config.services.disk)
            .filteringPath((path) => path.replace(/\?.*/, ''))
            .get('/json/space')
            .reply(200, require('./__nocks__/space.js'))
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'get-space-widget-data',
                    params: {}
                }
            ],
            _ckey: genCkey()
        }
    });

    expect(body).toMatchSnapshot();
});

test('enable-optin-subscriptions/v1', async () => {
    nocks.push(
        require('./__nocks__/settings-optin.js'),
        require('./__nocks__/settings-update.js')
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'enable-optin-subscriptions/v1',
                    params: {}
                }
            ],
            _ckey: genCkey()
        }
    });

    expect(body).toMatchSnapshot();
});

test('do-enable-hidden-trash', async () => {
    nocks.push(
        require('./__nocks__/settings-update.js'),
        require('./__nocks__/mops-create-hidden-trash')
    );

    const { body } = await request({
        headers: commonHeaders,
        body: {
            models: [
                {
                    name: 'do-enable-hidden-trash',
                    params: {}
                }
            ],
            _ckey: genCkey()
        }
    });

    expect(body).toMatchSnapshot();
});
