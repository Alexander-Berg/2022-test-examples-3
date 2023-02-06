'use strict';

const method = require('./get_messenger_id.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const duffmanErrors = require('@yandex-int/duffman').errors;

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        service: () => mockService,
        config: {
            IS_CORP: true
        }
    };
    mockService.mockResolvedValueOnce({
        users: [
            { id: '42' }
        ]
    });
});

test('ходит в blackbox', async () => {
    mockService.mockResolvedValueOnce({
        status: 'ok',
        data: {
            guid: '00000000-0000-0000-0000-000000000000',
            uid: 42,
            login: ''
        }
    });
    core.params.login = 'example';

    await method(core);

    expect(mockService).toHaveBeenCalledWith('userinfo', {
        login: 'example'
    });
});

test('обрабатывает ошибку от blackbox', async () => {
    mockService.mockReset().mockRejectedValueOnce({ message: 'omg' });
    core.params.login = 'example';

    await expect(method(core)).rejects.toMatchObject({
        code: 400,
        message: 'omg'
    });
});

test('обрабатывает несуществуюший uid', async () => {
    mockService.mockReset().mockResolvedValueOnce({
        users: [
            { id: '' }
        ]
    });
    core.params.login = 'example';

    await expect(method(core)).rejects.toMatchObject({
        code: 404,
        message: 'user does not exist'
    });
});

test('ходит в messenger', async () => {
    mockService.mockResolvedValueOnce({
        status: 'ok',
        data: {
            guid: '00000000-0000-0000-0000-000000000000',
            uid: 42,
            login: ''
        }
    });
    core.params.login = 'example';

    await method(core);

    expect(mockService).toHaveBeenCalledWith('/meta_api/', {
        method: 'get_user_info',
        params: {
            uid: 42
        }
    });
});

test('отвечает', async () => {
    core.params.login = 'example';
    mockService.mockResolvedValueOnce({
        status: 'ok',
        data: {
            guid: '00000000-0000-0000-0000-000000000000',
            uid: 42,
            login: ''
        }
    });

    const result = await method(core);

    expect(result).toEqual({
        guid: '00000000-0000-0000-0000-000000000000'
    });
});

test('проверяет схему', async () => {
    expect.assertions(3);

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

    core.params.login = 'example';
    mockService.mockRejectedValueOnce(new duffmanErrors.CUSTOM_ERROR({
        data: {
            code: 'user_does_not_exist'
        }
    }));

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(404);
        expect(err.message).toBe('user does not exist');
    }
});

test('обрабатывает неизвестные ошибки', async () => {
    expect.assertions(2);

    core.params.login = 'example';
    mockService.mockRejectedValueOnce(new duffmanErrors.CUSTOM_ERROR({
        data: {
            code: 'unknown_error'
        }
    }));

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
    }
});

test('обрабатывает http ошибки', async () => {
    expect.assertions(3);

    core.params.login = 'example';
    mockService.mockRejectedValueOnce(new duffmanErrors.HTTP_ERROR({}));

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
        expect(err.message).toBe('http error');
    }
});

test('обрабатывает странные ошибки', async () => {
    expect.assertions(3);

    core.params.login = 'example';
    mockService.mockRejectedValueOnce(new Error('wtf'));

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('wtf');
    }
});
