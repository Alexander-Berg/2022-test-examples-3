'use strict';

const method = require('./generate_captcha.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('ok', async () => {
    mockService.mockResolvedValue({
        '_': 'KEY',
        '$.url': 'URL'
    });

    const res = await method(core);
    expect(core.service).toHaveBeenCalledWith('captcha');
    expect(mockService).toHaveBeenCalledWith('/generate', {
        checks: 4
    });
    expect(res).toEqual({ key: 'KEY', url: 'URL' });
});

test('no data', async () => {
    expect.assertions(3);
    mockService.mockResolvedValue('ok');

    try {
        await method(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(500);
        expect(e.message).toEqual('NO_DATA');
    }
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
