'use strict';

const method = require('./apply_filter_rule.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const { EXTERNAL_ERROR } = require('@yandex-int/duffman').errors;

let core;
let mockRequest;

beforeEach(() => {
    mockRequest = jest.fn();
    core = {
        params: {
            method: 'full_hierarchy'
        },
        request: mockRequest
    };
});

test('invalid schema', async () => {
    expect.assertions(3);
    core.params = {
        id: 1
    };

    try {
        await method.call(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(400);
        expect(e.message).toEqual('invalid params schema');
    }
});

test('ok', async () => {
    core.params = {
        id: '1'
    };
    mockRequest.mockResolvedValue({ any: 'answer' });

    const res = await method.call(core);
    expect(mockRequest).toHaveBeenCalledWith('filters-apply', {
        id: '1'
    });
    expect(res).toEqual({});
});

test('furita error', async () => {
    expect.assertions(3);
    core.params = {
        id: '1'
    };
    mockRequest.mockRejectedValue(new EXTERNAL_ERROR('furita_error'));

    try {
        await method.call(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(400);
        expect(e.message).toEqual('furita_error');
    }
});
