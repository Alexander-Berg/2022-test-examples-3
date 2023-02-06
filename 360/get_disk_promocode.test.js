'use strict';

const method = require('./get_disk_promocode.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;
let responce;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            tag: 'tag',
            device_id: 'device_id'
        },
        service: () => mockService,
        config: {
            IS_CORP: true
        }
    };
    responce = {
        statusCode: 200,
        body: 'code'
    };
    mockService.mockResolvedValueOnce(responce);
});

test('ходит в сервис', async () => {
    await method(core);

    expect(mockService).toHaveBeenCalledWith('/assign', {
        tag: 'tag',
        device_id: 'device_id'
    }, {
        getRaw: true
    });
});

test('отвечает', async () => {
    const result = await method(core);

    expect(result).toEqual({ code: 'code' });
});

test('проверяет схему', async () => {
    expect.assertions(2);
    core.params = {};
    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err).toMatchObject({
            code: 400,
            message: 'invalid params schema'
        });
    }
});

test('обрабатывает ошибку 404', async () => {
    responce.statusCode = 404;

    await expect(method(core)).rejects.toMatchObject({
        code: 404,
        message: 'no more codes'
    });
});

test('обрабатывает http ошибки', async () => {
    responce.statusCode = 405;

    await expect(method(core)).rejects.toMatchObject({
        code: 500,
        message: 'http error'
    });
});

test('обрабатывает неизвестные ошибки', async () => {
    mockService.mockReset().mockRejectedValueOnce({ message: 'oops' });

    await expect(method(core)).rejects.toMatchObject({
        code: 400,
        message: 'oops'
    });
});
