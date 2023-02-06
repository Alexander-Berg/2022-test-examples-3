'use strict';

const method = require('./update_filter_rule.js');
const ApiError = require('../../../routes/helpers/api-error.js');

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
        field3: 1,
        move_label: '1'
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
        id: '1',
        name: '1',
        field1: [ 'to' ],
        field2: [ '1' ],
        field3: [ 'qwe' ],
        move_label: [ 'lid_read' ]
    };
    mockRequest.mockResolvedValue({ id: '42', session: 'deadbeef' });

    const res = await method.call(core);
    expect(mockRequest).toHaveBeenCalledWith('do-filters-edit', {
        id: '1',
        name: '1',
        field1: [ 'to' ],
        field2: [ '1' ],
        field3: [ 'qwe' ],
        move_label: [ 'lid_read' ]
    });
    expect(res).toEqual({ id: '42' });
});
