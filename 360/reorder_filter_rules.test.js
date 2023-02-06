'use strict';

const method = require('./reorder_filter_rules.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const { EXTERNAL_ERROR } = require('@yandex-int/duffman').errors;

let core;
let mockRequest;

beforeEach(() => {
    mockRequest = jest.fn();
    core = {
        params: {
            ids: [ '1', '2', '3' ]
        },
        request: mockRequest
    };
});

test('invalid schema', async () => {
    expect.assertions(3);
    core.params = {};

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
        ids: [ '1', '2' ]
    };
    mockRequest.mockResolvedValue({});

    const res = await method.call(core);
    expect(mockRequest).toHaveBeenCalledWith('do-filters-sort', {
        list: '1,2'
    });
    expect(res).toEqual({});
});

test('furita error', async () => {
    expect.assertions(3);
    core.params = {
        ids: [ '1' ]
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
