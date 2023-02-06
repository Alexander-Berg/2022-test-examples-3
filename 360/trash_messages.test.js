'use strict';

const method = require('./trash_messages.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;

beforeEach(() => {
    core = {
        params: {
            search: {
                from: 'foo@example.com'
            },
            messageType: 13
        },
        request: jest.fn()
    };
});

test('вызывает update-subscriptions/v1', async () => {
    core.request.mockResolvedValueOnce({});

    await method.call(core);

    expect(core.request).toHaveBeenCalledWith('update-subscriptions/v1', {
        subscriptions: [
            {
                action: 'hide',
                email: 'foo@example.com',
                moveExisting: true
            }
        ]
    });
});

test('вызывает trash-messages/v1', async () => {
    core.request.mockResolvedValueOnce({});
    core.params = {
        messageIds: [ '1', '2', '3' ]
    };

    await method.call(core);

    expect(core.request).toHaveBeenCalledWith('trash-messages/v1', {
        messageIds: [ '1', '2', '3' ]
    });
});

test('проверяет схему', async () => {
    expect.assertions(3);
    core.params = {};

    try {
        await method.call(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('invalid params schema');
    }
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
