'use strict';

const method = require('./get_suggested_domains.js');

let core;

beforeEach(() => {
    core = {
        params: {
            domain_base: 'foo',
            login: 'bar'
        },
        request: jest.fn()
    };
});

test('calls method', async () => {
    core.request.mockResolvedValue({});
    const res = await method.call(core);
    expect(core.request)
        .toHaveBeenCalledWith('get-domenator-domains-suggest/v1', {
            domain_base: 'foo',
            login: 'bar'
        });
    expect(res).toEqual({
        domain_status: 'not_allowed'
    });
});

test('with limit', async () => {
    core.params.limit = 10;
    core.request.mockResolvedValue({});
    const res = await method.call(core);
    expect(core.request)
        .toHaveBeenCalledWith('get-domenator-domains-suggest/v1', {
            domain_base: 'foo',
            login: 'bar',
            limit: '10'
        });
    expect(res).toEqual({
        domain_status: 'not_allowed'
    });
});
