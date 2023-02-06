'use strict';

const method = require('./generate_operation_id.js');
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
        status: 'ok',
        object: {
            id: 'ID'
        }
    });

    const res = await method(core);
    expect(core.service).toHaveBeenCalledWith('sendbernar');
    expect(mockService).toHaveBeenCalledWith('generate_operation_id', {});
    expect(res).toEqual({ operation_id: 'ID' });
});

test('error', async () => {
    expect.assertions(3);
    mockService.mockResolvedValue({
        status: 'fail',
        object: {
            reason: 'REASON'
        }
    });

    try {
        await method(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(400);
        expect(e.message).toEqual('REASON');
    }
});

test('http error', async () => {
    expect.assertions(2);
    mockService.mockRejectedValue();

    try {
        await method(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(400);
    }
});

test('http error 5xx', async () => {
    expect.assertions(2);
    mockService.mockRejectedValue(httpError(500));

    try {
        await method(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(500);
    }
});
