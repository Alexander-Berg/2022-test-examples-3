'use strict';

const method = require('./register_domain.js');
const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;
const ApiError = require('../../../routes/helpers/api-error.js');

let core;

beforeEach(() => {
    core = {
        params: {
            domain: 'foo',
            login: 'bar'
        },
        request: jest.fn()
    };
});

test('calls method', async () => {
    core.request.mockResolvedValue({});
    const res = await method.call(core);
    expect(core.request)
        .toHaveBeenCalledWith('domenator-register-domain/v1', { domain: 'foo', login: 'bar' });
    expect(res).toEqual({});
});

test('subscription error', async () => {
    expect.assertions(2);
    core.request.mockRejectedValue(new CUSTOM_ERROR('Subscription needed'));

    try {
        await method.call(core);
    } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        expect(e.errorCode).toEqual('NO_SUBSCRIPTION');
    }
});
