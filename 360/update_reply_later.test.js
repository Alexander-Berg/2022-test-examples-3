'use strict';

const method = require('./update_reply_later.js');
const ApiError = require('../../../routes/helpers/api-error.js');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode, obj) => new HTTP_ERROR({ statusCode, ...obj });

let core;

beforeEach(() => {
    core = {
        params: {},
        request: jest.fn()
    };
});

test('вызывает do-reply-later-update', async () => {
    core.params = { mid: '1', date: 42, foo: true, uuid: 'deadbeef' };
    core.request.mockResolvedValueOnce({});

    await method.call(core);

    expect(core.request).toHaveBeenCalledWith('do-reply-later-update', {
        mid: '1',
        date: 42
    });
});

test('отвечает', async () => {
    core.params = { mid: '1', date: 42 };
    core.request.mockResolvedValueOnce({ foo: 'bar' });

    const result = await method.call(core);

    expect(result).toEqual({});
});

test('обрабатыват 4хх ошибку', async () => {
    expect.assertions(2);

    core.params = { mid: '1', date: 42 };
    core.request.mockRejectedValueOnce(httpError(400, { body: { message: 'MESSAGE' } }));

    try {
        await method.call(core);
    } catch (e) {
        expect(e.code).toEqual(400);
        expect(e.message).toEqual('MESSAGE');
    }
});

test('проверяет схему', async () => {
    expect.assertions(3);
    core.params = { mid: 1 };

    try {
        await method.call(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('invalid params schema');
    }
});
