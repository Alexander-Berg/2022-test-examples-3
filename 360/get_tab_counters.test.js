'use strict';

const method = require('./get_tab_counters.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        service: () => mockService
    };
});

test('ходит в мету', async () => {
    mockService.mockResolvedValueOnce({ new_counters: {} });

    await method(core);

    expect(mockService).toHaveBeenCalledWith('/folder_tabs_new_counters', {});
});

test('проксирует ответ', async () => {
    mockService.mockResolvedValueOnce({
        new_counters: {
            news: 1,
            social: 2,
            relevant: 0
        }
    });

    const result = await method(core);

    expect(result).toEqual({
        new_counters: {
            default: 0,
            social: 2,
            news: 1
        }
    });
});

test('ошибка с неправильной схемой запроса', async () => {
    expect.assertions(3);
    core.params = { limit: 400 };
    mockService.mockResolvedValueOnce({
        foo: 'bar'
    });

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('invalid params schema');
    }
});

test('обрабатывает ошибки', async () => {
    expect.assertions(3);
    mockService.mockRejectedValueOnce({ message: 'foo' });

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('foo');
    }
});
