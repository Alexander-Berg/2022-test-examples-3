'use strict';

const method = require('./create_filter_rule.js');
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
        name: 1,
        field1: 1,
        field2: 1,
        field3: 1
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
        foo: 'bar',
        name: '1',
        field1: [ 'to' ],
        field2: [ '1' ],
        field3: [ 'qwe' ],
        move_label: [
            'lid_read'
        ]
    };
    mockRequest.mockResolvedValue({ id: '42', session: 'deadbeef' });

    const res = await method.call(core);
    expect(mockRequest).toHaveBeenCalledWith('do-filters-add', {
        name: '1',
        field1: [ 'to' ],
        field2: [ '1' ],
        field3: [ 'qwe' ],
        move_label: [ 'lid_read' ]
    });
    expect(res).toEqual({ id: '42' });
});

test('furita error', async () => {
    expect.assertions(3);
    core.params = {
        foo: 'bar',
        name: '1',
        field1: [ 'to' ],
        field2: [ '1' ],
        field3: [ 'qwe' ]
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
