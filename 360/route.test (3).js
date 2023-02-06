'use strict';

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

const ApiCore = require('../../helpers/api-core/index.js');
const req = httpMock.createRequest({
    headers: {
        'x-original-host': 'mail.yandex.ru',
        'x-original-uri': '/web-api/models/touch1',
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

beforeEach((done) => {
    const routePath = '/web-api/models';
    const routePathLiza1 = `${routePath}/touch1`;
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

        options.headers = Object.assign({
            'x-original-host': 'test',
            'x-https-request': 'yes',
            'cookie': 'Session_id=FAKE; yandexuid=' + yandexuid
        }, options.headers || {});

        return got(`http://localhost:${port}${routePathLiza1}`, options);
    };

    core = new ApiCore(req, res);
    setAuth(core);
    const auth = core.auth.get();
    nock.enableNetConnect(`localhost:${port}`);

    nock(core.config.services.meta);
    nock(core.config.services.akita)
        .filteringPath((path) => path.replace(/\?.*/, ''))
        .get('/auth')
        .reply(200, {
            account_information: {
                account: {
                    userId: auth.uid,
                    serviceUserId: auth.suid,
                    mailDataBase: auth.mdb,
                    timeZone: {
                        timezone: auth.timezone,
                        offset: auth.tz_offset
                    },
                    karma: {},
                    userTicket: 'tvm-user-ticket'
                },
                addresses: {
                    defaultAddress: '',
                    internalAddresses: []
                }
            }
        });
});

afterEach((done) => {
    server.close(done);
    nock.cleanAll();
});

test('запрос моделей без ckey, даёт ошибку в ответе', async () => {
    const { body } = await request({
        headers: {
            'x-real-ip': '123',
            'x-real-port': '6666',
            'x-request-id': '12345'
        },
        body: {
            '_model.0': 'do-label',
            '_exp': '1,2,3',
            '_eexp': '1,2',
            '_locale': 'ru'
        }
    });

    expect(body).toMatchSnapshot();
});

test('запрашивает модели с ckey', async () => {
    const mopsNock = nock(core.config.services.mops)
        .filteringPath((path) => path.replace(/\?.*/, ''))
        .post('/label')
        .reply(200, {});

    const ckey = genCkey();

    const { body } = await request({
        headers: {
            'x-real-ip': '123',
            'x-real-port': '6666',
            'x-request-id': '12345'
        },
        body: {
            '_model.0': 'do-label',
            '_ckey': ckey,
            '_exp': '1,2,3',
            '_eexp': '1,2',
            '_locale': 'ru'
        }
    });

    expect(body).toMatchSnapshot();
    mopsNock.done();
});

test('запрашивает модели с нужными хедерами', async () => {
    const mopsNock = nock(core.config.services.mops, {
        reqheaders: {
            'user-agent': 'web-api-node',
            'x-real-ip': '123',
            'x-real-port': '6666',
            'X-Yandex-ExpBoxes': '1,2,3',
            'X-Yandex-EnabledExpBoxes': '1,2',
            'X-Yandex-ClientType': 'TOUCH',
            'X-Yandex-ClientVersion': 'TOUCH_VERSION',
            'x-request-id': '12345'
        }
    }).filteringPath((path) => path.replace(/\?.*/, ''))
        .post('/label')
        .reply(200, {});

    const ckey = genCkey();

    const { body } = await request({
        headers: {
            'x-real-ip': '123',
            'x-real-port': '6666',
            'x-request-id': '12345'
        },
        body: {
            '_model.0': 'do-label',
            '_ckey': ckey,
            '_exp': '1,2,3',
            '_eexp': '1,2',
            '_locale': 'ru',
            '_service': 'TOUCH',
            '_version': 'TOUCH_VERSION'
        }
    });

    expect(body.models[0].status).toBe('ok');
    mopsNock.done();
});

test('не пятисотим на запросах с дырками', async () => {
    const { body } = await request({
        query: {
            'so.1': 'me',
            'str.2': 'ange',
            'req.3': 'uest'
        }
    });

    expect(body.models).toEqual([
        null,
        null,
        null,
        null
    ]);
});

test('устанавливает нужные хеадеры', async () => {
    const ckey = genCkey();

    const { headers } = await request({
        body: {
            _ckey: ckey,
            _exp: '1,2,3',
            _eexp: '1,2',
            _locale: 'ru'
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
    const ckey = genCkey();

    const { body } = await request({
        headers: {
            'x-real-ip': '123',
            'x-real-port': '6666',
            'x-request-id': '12345'
        },
        body: {
            _ckey: ckey,
            _exp: '1,2,3',
            _eexp: '1,2',
            _locale: 'ru'
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

test('возвращает в параметре versions версии u2709, web-api, duffman', async () => {
    const ckey = genCkey();

    const { body } = await request({
        body: {
            _ckey: ckey,
            _exp: '1,2,3',
            _eexp: '1,2',
            _locale: 'ru'
        }
    });

    expect(body.versions).toEqual({ ...core.config.VERSION });
});

test('если авторизация через OAuth, то проверять ckey не нужно', async () => {
    const mopsNock = nock(core.config.services.mops)
        .filteringPath((path) => path.replace(/\?.*/, ''))
        .post('/label')
        .reply(200, {});

    const { body } = await request({
        headers: {
            'x-real-ip': '123',
            'x-real-port': '6666',
            'x-request-id': '12345',
            'authorization': 'OAuth XXX',
            'cookie': ''
        },
        body: {
            '_model.0': 'do-label',
            '_exp': '1,2,3',
            '_eexp': '1,2',
            '_locale': 'ru'
        }
    });

    expect(body).toMatchSnapshot();
    mopsNock.done();
});

test('подновляет устаревший ckey', async () => {
    const ckey = genCkey();
    const outdatedCkey = ckey.split('!')[0] + '!jnoqjj3c';

    const { body } = await request({
        headers: {
            'x-real-ip': '123',
            'x-real-port': '6666',
            'x-request-id': '12345'
        },
        body: {
            _ckey: outdatedCkey,
            _exp: '1,2,3',
            _eexp: '1,2',
            _locale: 'ru'
        }
    });

    expect(body).toContainEntry([ 'ckey', 'kbydplfc8Sul2Nb21MEtCPh14yI=!jnqa5ezs' ]);
});
