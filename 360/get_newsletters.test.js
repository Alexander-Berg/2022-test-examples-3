'use strict';

const method = require('./get_newsletters.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;

const newslettersMock = [
    {
        email: 'foo@example.com',
        displayName: 'foo',
        messageType: 0,
        readFrequency: 0.1
    },
    {
        email: 'foo2@example.com',
        displayName: 'foo2',
        messageType: 13,
        readFrequency: 0.3
    },
    {
        email: 'foo3@example.com',
        displayName: 'foo3',
        messageType: 14,
        readFrequency: 0
    }
];

beforeEach(() => {
    core = {
        params: {},
        request: jest.fn()
    };
});

test('вызывает get-newsletters/v1', async () => {
    core.request.mockResolvedValueOnce({});

    await method.call(core);

    expect(core.request).toHaveBeenCalledWith('get-newsletters/v1');
});

test('отвечает количеством', async () => {
    core.request.mockResolvedValueOnce({
        newsletters: newslettersMock
    });

    const result = await method.call(core);

    expect(result).toMatchSnapshot();
});

test('отвечает полностью', async () => {
    core.params.full = true;
    core.request.mockResolvedValueOnce({
        newsletters: newslettersMock
    });

    const result = await method.call(core);

    expect(result).toMatchSnapshot();
});

test('проверяет схему', async () => {
    expect.assertions(3);

    core.params.full = 'true';

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
