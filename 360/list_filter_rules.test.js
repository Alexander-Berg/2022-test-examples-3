'use strict';

const method = require('./list_filter_rules.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const { EXTERNAL_ERROR } = require('@yandex-int/duffman').errors;

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            method: 'full_hierarchy'
        },
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('invalid schema', async () => {
    expect.assertions(3);
    core.params = { master: 'true' };

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
        master: true
    };
    mockService.mockResolvedValue({
        rules: []
    });

    const res = await method.call(core);
    expect(mockService).toHaveBeenCalledWith('/api/list.json', {
        detailed: '1',
        master: true,
        type: 'user'
    });
    expect(res).toEqual({
        rules: []
    });
});

test('furita error', async () => {
    expect.assertions(3);
    core.params = {
        id: '1'
    };
    mockService.mockRejectedValue(new EXTERNAL_ERROR('furita_error'));

    try {
        await method.call(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.code).toEqual(400);
        expect(e.message).toEqual('furita_error');
    }
});
