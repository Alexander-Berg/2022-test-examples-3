'use strict';

const getAuthTvm = require('./get-auth-tvm.js');
const ExtraCore = require('./extra-core.js');
const ApiError = require('./api-error.js');

let core;
let request;
let response;

const tvmConfig = {
    clients: [
        {
            name: 'client',
            id: '42'
        }
    ],
    rules: [
        {
            path: 'v1/method',
            allow: [ 'client' ]
        }
    ]
};

const mockTvmConfig = jest.fn();

const mockCheckService = jest.fn();
const mockCheckUser = jest.fn();

jest.mock('./tvm/check-service.js', () => () => mockCheckService());
jest.mock('./tvm/check-user.js', () => () => mockCheckUser());

beforeEach(function() {
    request = {
        cookies: {},
        headers: {
            'host': 'mail.yandex.ru',
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/api/mobile/v1',
            'x-request-id': '12345',
            'x-real-ip': '2a02:6b8::25'
        },
        query: {
            uuid: 'deadbeef42',
            client_version: '10.0.3'
        },
        body: {},
        apiVer: 'v1',
        apiMethod: 'method'
    };

    response = {
        on: () => ({})
    };

    core = new ExtraCore(request, response);

    mockTvmConfig.mockReturnValue(tvmConfig);
    Object.defineProperty(core.config, 'tvmConfig', {
        get: jest.fn(() => mockTvmConfig())
    });
});

test('happy path', async () => {
    mockCheckUser.mockResolvedValue({ uid: '11' });
    mockCheckService.mockResolvedValue({ src: '42' });

    const res = await getAuthTvm({
        core,
        headers: {
            ...request.headers,
            'x-ya-user-ticket': '3:user:ticket',
            'x-ya-service-ticket': '3:service:ticket'
        }
    });

    expect(res).toMatchSnapshot();
});

test('emtpy rules', async () => {
    expect.assertions(3);

    core.req.apiMethod = 'method2';

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(403);
        expect(e.message).toBe('tvm rule for v1/method2 not configured');
    }
});

test('unknown rule', async () => {
    expect.assertions(3);

    mockTvmConfig.mockReturnValue({ clients: [], rules: [] });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(403);
        expect(e.message).toBe('no tvm rules found');
    }
});

test('not allowed client', async () => {
    expect.assertions(3);

    mockCheckUser.mockResolvedValue({ uid: '11' });
    mockCheckService.mockResolvedValue({ src: '43' });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(403);
        expect(e.message).toBe('service not allowed to do v1/method');
    }
});

test('check user failed', async () => {
    expect.assertions(3);

    mockCheckUser.mockRejectedValue({
        code: 'TVM_ERROR',
        message: 'message',
        data: { logging_string: 'logging_string' }
    });
    mockCheckService.mockResolvedValue({ src: '42' });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(403);
        expect(e.message).toBe('user: message (logging_string)');
    }
});

test('check user invalid', async () => {
    expect.assertions(3);

    mockCheckUser.mockRejectedValue({
        code: 'INVALID_PARAMS',
        message: 'message'
    });
    mockCheckService.mockResolvedValue({ src: '42' });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(403);
        expect(e.message).toBe('user: message');
    }
});

test('check user unknown', async () => {
    expect.assertions(3);

    mockCheckUser.mockRejectedValue({
        code: 'UNKNOWN',
        message: 'message'
    });
    mockCheckService.mockResolvedValue({ src: '42' });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(400);
        expect(e.message).toBe('user: unknown error');
    }
});

test('check service failed', async () => {
    expect.assertions(3);

    mockCheckUser.mockResolvedValue({ uid: '42' });
    mockCheckService.mockRejectedValue({
        code: 'TVM_ERROR',
        message: 'message',
        data: { logging_string: 'logging_string' }
    });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(403);
        expect(e.message).toBe('service: message (logging_string)');
    }
});

test('check service ivalid', async () => {
    expect.assertions(3);

    mockCheckUser.mockResolvedValue({ uid: '42' });
    mockCheckService.mockRejectedValue({
        code: 'INVALID_PARAMS',
        message: 'message'
    });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(403);
        expect(e.message).toBe('service: message');
    }
});

test('check service unknown', async () => {
    expect.assertions(3);

    mockCheckUser.mockResolvedValue({ uid: '42' });
    mockCheckService.mockRejectedValue({
        code: 'UNKNOWN',
        message: 'message'
    });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(400);
        expect(e.message).toBe('service: unknown error');
    }
});

test('uid=0 => 403', async () => {
    expect.assertions(2);

    mockCheckUser.mockResolvedValue({ uid: '0' });
    mockCheckService.mockResolvedValue({ src: '42' });

    try {
        await getAuthTvm({
            core,
            headers: {
                ...request.headers,
                'x-ya-user-ticket': '3:user:ticket',
                'x-ya-service-ticket': '3:service:ticket'
            }
        });
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toBe(403);
    }
});
