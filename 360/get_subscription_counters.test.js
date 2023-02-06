'use strict';

const method = require('./get_subscription_counters.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;

beforeEach(() => {
    core = {
        params: {
            uuid: 'deadbeef',
            client: 'aphone'
        },
        request: jest.fn()
    };
});

test('вызывает get-subscription-counters/v1', async () => {
    core.request.mockResolvedValueOnce({});
    core.request.mockResolvedValueOnce({
        active: 42,
        hidden: 0
    });

    const data = await method.call(core);

    expect(core.request).toHaveBeenCalledWith('get-subscription-counters/v1', {});
    expect(data).toMatchSnapshot();
});

test('вызывает get-subscription-counters/v1 с включенным оптин', async () => {
    core.request.mockResolvedValueOnce({
        mail_b2c_can_use_opt_in_subs: true,
        opt_in_subs_enabled: true
    });
    core.request.mockResolvedValueOnce({
        pending: 42,
        active: 1,
        hidden: 5
    });

    const data = await method.call(core);

    expect(core.request).toHaveBeenCalledWith('get-subscription-counters/v1', { optinEnabled: true });
    expect(data).toMatchSnapshot();
});

test('обрабатывает ошибки', async () => {
    expect.assertions(3);
    core.request.mockRejectedValueOnce({ message: 'foo' });

    try {
        await method.call(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('foo');
    }
});
