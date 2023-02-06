const mockGetLocation = jest.fn().mockReturnValue('FAKE_LOCATION');

jest.mock('../../helpers/logged-ask.js');
jest.mock('../../helpers/get-location.js', () => mockGetLocation);

const buildUaasMiddleware = require('../../middleware/uaas.js');
const loggedAsk = require('../../helpers/logged-ask.js');
const { encode } = require('../../helpers/base64.js');
const AskError = require('asker-as-promised').Error;
const { last } = require('lodash');

const key = new Buffer('RW5jb2RlIHRvIEJhc2U2NCBmb3JtYXQ=', 'base64');

const middlewareConfig = {
    logger: { log: () => {} },
    uaasHost: 'host',
    uaasService: 'dv',
    blowfishKey: key
};

const expboxes = '1,0,0;2,0,0';

const experiments = [
    [{ HANDLER: 'REPORT', CONTEXT: {} }],
    [{ HANDLER: 'DISK', CONTEXT: { DISK: { data: [{ scale: '0.5' }], flags: ['do_that'] } } }]
];

const expflags = experiments.map((exp) => encode(JSON.stringify(exp))).join(',');

const requestHeaders = {
    'x-forwarded-for': '192.168.0.52',
    'user-agent': 'some browser',
    cookie: 'key=value; yandexuid=123'
};

describe('uaasMiddleware', () => {
    afterEach(() => {
        loggedAsk.mockClear();
    });

    it('передача test-id чтобы принудительно попасть в эксперимент', (done) => {
        const middleware = buildUaasMiddleware(middlewareConfig);

        const req = Object.create(null, {
            query: {
                value: {
                    'test-id': '123'
                }
            },
            headers: {
                value: requestHeaders
            }
        });

        loggedAsk.mockImplementation(() => Promise.resolve());

        middleware(req, {}, () => {
            expect(loggedAsk).toHaveBeenCalled();
            expect(last(loggedAsk.mock.calls)[0]).toEqual({
                host: 'host',
                path: '/dv',
                headers: {
                    'X-Yandex-UAAS': 'testing',
                    'x-forwarded-for-y': '192.168.0.52',
                    'x-backend-location': 'FAKE_LOCATION/stable',
                    'user-agent': 'some browser',
                    cookie: 'key=value; yandexuid=123'
                },
                query: {
                    'test-id': '123'
                },
                timeout: 100
            });

            done();
        });
    });
    it('должна передать uuid в query-параметрах', (done) => {
        const middleware = buildUaasMiddleware(middlewareConfig);

        const req = Object.create(null, {
            user: {
                value: {
                    id: '123'
                }
            },
            query: {
                value: {}
            },
            headers: {
                value: requestHeaders
            }
        });

        loggedAsk.mockImplementation(() => Promise.resolve());

        middleware(req, {}, () => {
            expect(loggedAsk).toHaveBeenCalled();
            expect(last(loggedAsk.mock.calls)[0]).toEqual({
                host: 'host',
                path: '/dv',
                headers: {
                    'x-forwarded-for-y': '192.168.0.52',
                    'user-agent': 'some browser',
                    'x-backend-location': 'FAKE_LOCATION/stable',
                    cookie: 'key=value; yandexuid=123'
                },
                query: {
                    uuid: '123'
                },
                timeout: 100
            });

            done();
        });
    });
    it('не должна передать uuid в query-параметрах', (done) => {
        const middleware = buildUaasMiddleware(middlewareConfig);

        const req = Object.create(null, {
            user: {
                value: {
                    id: '0'
                }
            },
            query: {
                value: {}
            },
            headers: {
                value: requestHeaders
            }
        });

        loggedAsk.mockImplementation(() => Promise.resolve());

        middleware(req, {}, () => {
            expect(loggedAsk).toHaveBeenCalled();
            expect(last(loggedAsk.mock.calls)[0]).toEqual({
                host: 'host',
                path: '/dv',
                headers: {
                    'x-forwarded-for-y': '192.168.0.52',
                    'user-agent': 'some browser',
                    'x-backend-location': 'FAKE_LOCATION/stable',
                    cookie: 'key=value; yandexuid=123'
                },
                query: {},
                timeout: 100
            });

            done();
        });
    });
    it('должна добавить эксперимент после запроса к сервису', (done) => {
        const middleware = buildUaasMiddleware(middlewareConfig);

        const req = { headers: { cookie: 'yandexuid=123' }, query: {} };

        const res = {
            headers: { 'x-yandex-expboxes': expboxes, 'x-yandex-expflags': expflags }
        };

        loggedAsk.mockImplementation(() => Promise.resolve(res));

        middleware(req, {}, () => {
            expect(loggedAsk).toHaveBeenCalled();
            expect(req.experiments).toEqual({
                boxes: ['1,0,0', '2,0,0'],
                metrika: 'RpsOJ6A4I4XvgnJ_lEXiIQ',
                ids: ['1', '2'],
                experiments: [
                    [{ HANDLER: 'REPORT', CONTEXT: {} }],
                    [{ HANDLER: 'DISK', CONTEXT: { DISK: { data: [{ scale: '0.5' }], flags: ['do_that'] } } }]
                ],
                diskFlags: {
                    do_that: { scale: '0.5' }
                }
            });

            done();
        });
    });
    it('не должна добавить эксперимент после запроса к сервису', (done) => {
        const middleware = buildUaasMiddleware(middlewareConfig);

        const res = { headers: {} };
        const req = { headers: { cookie: 'yandexuid=123' }, query: {} };

        loggedAsk.mockImplementation(() => Promise.resolve(res));

        middleware(req, {}, () => {
            expect(loggedAsk).toHaveBeenCalled();
            expect(req.experiments).toBeUndefined();

            done();
        });
    });
    it('должна перехватить и залогировать ошибку запроса к сервису', (done) => {
        const error = new Error('UAAS failed');
        const req = { headers: { cookie: 'yandexuid=123' }, query: {} };
        const logger = { log: jest.fn() };

        const middleware = buildUaasMiddleware(Object.assign({}, middlewareConfig, { logger }));

        loggedAsk.mockImplementation(() => Promise.reject(error));

        middleware(req, {}, () => {
            expect(logger.log).toHaveBeenCalledWith(req, 'nodejs', {}, error);
            expect(req.experiments).toBeUndefined();

            done();
        });
    });
    it('должна перехватить ошибку запроса к сервису без логирования', (done) => {
        const error = new AskError('UAAS failed');
        const req = { headers: { cookie: 'yandexuid=123' }, query: {} };
        const logger = { log: jest.fn(() => {}) };

        const middleware = buildUaasMiddleware(Object.assign({}, middlewareConfig, { logger }));

        loggedAsk.mockImplementation(() => Promise.reject(error));

        middleware(req, {}, () => {
            expect(logger.log).not.toHaveBeenCalled();
            expect(req.experiments).toBeUndefined();

            done();
        });
    });

    it('должна брать yandexuid из req.cookies если она там есть, а в headers нет', (done) => {
        const middleware = buildUaasMiddleware(middlewareConfig);

        const req = { headers: {}, query: {}, cookies: { yandexuid: '456' } };

        loggedAsk.mockImplementation(() => Promise.resolve());

        middleware(req, {}, () => {
            expect(loggedAsk).toHaveBeenCalled();
            expect(last(loggedAsk.mock.calls)[0].headers.cookie).toEqual('yandexuid=456');

            done();
        });
    });
    it('при добавлении yandexuid не должна удалять имеющиеся куки', (done) => {
        const middleware = buildUaasMiddleware(middlewareConfig);

        const req = { headers: { cookie: 'key=value' }, query: {}, cookies: { yandexuid: '456' } };

        loggedAsk.mockImplementation(() => Promise.resolve());

        middleware(req, {}, () => {
            expect(loggedAsk).toHaveBeenCalled();
            expect(last(loggedAsk.mock.calls)[0].headers.cookie).toEqual('yandexuid=456; key=value');

            done();
        });
    });
    it('не должна идти в uaas если нет ни uid ни yandexuid, должна залогировать эту ситуацию', (done) => {
        const req = { headers: {}, query: {}, cookies: {} };
        const logger = { log: jest.fn(() => {}) };

        const middleware = buildUaasMiddleware(Object.assign({}, middlewareConfig, { logger }));

        loggedAsk.mockImplementation(() => Promise.resolve());

        middleware(req, {}, () => {
            expect(loggedAsk).not.toHaveBeenCalled();
            middleware(req, {}, () => {
                expect(logger.log).toHaveBeenCalledWith(req, 'uaas', {
                    level: 'error',
                    message: 'uaas requires uid or yandexuid'
                });
                expect(req.experiments).toBeUndefined();

                done();
            });

            done();
        });
    });
});
