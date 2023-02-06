'use strict';

const apnsQueueRepeat = require('./apns_queue_repeat.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const successResponse = require('../../../test/mock/apns_queue_repeat.json');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            app_name: 'ru.yandex.mail',
            device: 'DB57DB9F-D83E-4B1C-A3E6-0E50EC769C54',
            pos: '100',
            count: '10'
        },
        config: {
            secrets: {
                pushSubscribeToken: 'XXX'
            }
        },
        service: () => mockService
    };
});

test('ходит в xiva и прокидывает параметры', async () => {
    mockService.mockResolvedValueOnce(successResponse);

    await apnsQueueRepeat(core);

    expect(mockService).toHaveBeenCalledWith('/v2/apns_queue_repeat', {
        app_name: 'ru.yandex.mail',
        device: 'DB57DB9F-D83E-4B1C-A3E6-0E50EC769C54',
        pos: '100',
        count: '10'
    }, {
        headers: {
            Authorization: 'Xiva XXX'
        }
    });
});

test('проксирует ответ', async () => {
    mockService.mockResolvedValueOnce(successResponse);

    const response = await apnsQueueRepeat(core);

    expect(response).toEqual({
        result: {
            repeated_count: 5
        }
    });
});

test('возвращает 400 без параметра app_name', async () => {
    delete core.params.app_name;

    try {
        await apnsQueueRepeat(core);
        throw new Error('MUST REJECT');
    } catch (err) {
        expect(err.code).toEqual(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('возвращает 400 без параметра device', async () => {
    delete core.params.device;

    try {
        await apnsQueueRepeat(core);
        throw new Error('MUST REJECT');
    } catch (err) {
        expect(err.code).toEqual(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('возвращает 400 без параметра pos', async () => {
    delete core.params.pos;

    try {
        await apnsQueueRepeat(core);
        throw new Error('MUST REJECT');
    } catch (err) {
        expect(err.code).toEqual(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('возвращает 400 без параметра count', async () => {
    delete core.params.count;

    try {
        await apnsQueueRepeat(core);
        throw new Error('MUST REJECT');
    } catch (err) {
        expect(err.code).toEqual(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('если сервис валится, отвечаем 500', async () => {
    mockService.mockRejectedValueOnce({});

    try {
        await apnsQueueRepeat(core);
        throw new Error('MUST REJECT');
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toEqual(500);
    }
});
