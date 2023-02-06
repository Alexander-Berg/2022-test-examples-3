'use strict';

const method = require('./reset_fresh.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        service: () => mockService,
        request: jest.fn().mockResolvedValue({})
    };
});

test('ходит в мету', async () => {
    mockService.mockResolvedValueOnce();

    await method(core);

    expect(mockService).toHaveBeenCalledWith('/reset_fresh_counter');
});

test('-> OK', async () => {
    mockService.mockResolvedValueOnce({});

    const result = await method(core);

    expect(result).toEqual({});
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
