'use strict';

const method = require('./check_captcha.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            rep: 'REP',
            key: 'KEY'
        },
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('invalid schema', async () => {
    expect.assertions(3);
    core.params = { foo: 'bar' };

    try {
        await method(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(400);
        expect(e.message).toContain('invalid params schema');
    }
});

test('ok', async () => {
    mockService.mockResolvedValue('ok');

    const res = await method(core);
    expect(core.service).toHaveBeenCalledWith('captcha');
    expect(mockService).toHaveBeenCalledWith('/check', {
        rep: 'REP',
        key: 'KEY'
    });
    expect(res).toEqual({ status: 'ok' });
});

test('error', async () => {
    mockService.mockResolvedValue({
        '_': 'status',
        '$.error': 'error'
    });

    const res = await method(core);
    expect(res).toEqual({
        status: 'status',
        error: 'error'
    });
});

test('http error 4xx', async () => {
    expect.assertions(2);
    mockService.mockRejectedValue(httpError(423));

    try {
        await method(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(400);
    }
});

test('http error non 4xx', async () => {
    expect.assertions(2);
    mockService.mockRejectedValue(httpError(500));

    try {
        await method(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(500);
    }
});
