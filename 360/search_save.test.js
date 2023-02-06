'use strict';

const method = require('./search_save');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            request: 'test',
            client: 'aphone'
        },
        res: {
            status: jest.fn(() => ({
                send: jest.fn()
            })),
            set: jest.fn()
        },
        service: () => mockService
    };
});

test('возвращает 400 без параметра client', async () => {
    delete core.params.client;

    await method(core);

    expect(core.res.status).toHaveBeenCalledWith(400);
});

test('возвращает 400 без параметра request', async () => {
    delete core.params.request;

    await method(core);

    expect(core.res.status).toHaveBeenCalledWith(400);
});

test('добавляет в параметр mid, если указан', async () => {
    mockService.mockResolvedValueOnce({});
    core.params.mid = 42;

    await method(core);

    expect(mockService.mock.calls[0][1].mid).toBe(42);
});

test('добавляет request', async () => {
    mockService.mockResolvedValueOnce({});
    core.params.mid = 42;

    await method(core);

    expect(mockService.mock.calls[0][1].request).toBe('test');
});

test('если сервис валится, пробрасываем 500 с message', async () => {
    expect.assertions(3);
    mockService.mockRejectedValueOnce({ message: 'foo' });

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
        expect(err.message).toBe('foo');
    }
});

test('happy path', async () => {
    mockService.mockResolvedValueOnce({});

    const res = await method(core);

    expect(res).toEqual({});
});
