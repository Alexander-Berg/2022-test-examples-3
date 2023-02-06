'use strict';

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;

const method = require('./get_events.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const eventsMock = require('../../../test/mock/calendar/events.json');

const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();

    core = {
        params: {},
        auth: {
            get: () => ({ uid: '42', locale: 'ru' })
        },
        service: () => mockService
    };
});

test('отвечаем 400, если нет параметров', async () => {
    expect.assertions(2);

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('вызывает сервис с нужными параметрами', async () => {
    expect.assertions(1);

    mockService.mockResolvedValueOnce({
        events: eventsMock
    });
    core.params.from = '2020-01-21';
    core.params.to = '2020-01-22';

    await method(core);

    expect(mockService.mock.calls[0]).toEqual([
        '/get-events',
        {
            from: '2020-01-21',
            to: '2020-01-22',
            lang: 'ru'
        },
        {}
    ]);
});

test('вызывает сервис с нужными параметрами, если передан targetUid', async () => {
    expect.assertions(1);

    mockService.mockResolvedValueOnce({
        events: eventsMock
    });
    core.params.targetUid = 'FAKE_TARGET_UID';
    core.params.from = '2020-01-21';
    core.params.to = '2020-01-22';

    await method(core);

    expect(mockService.mock.calls[0]).toEqual([
        '/get-events',
        {
            actorUid: '42',
            from: '2020-01-21',
            lang: 'ru',
            targetUid: 'FAKE_TARGET_UID',
            to: '2020-01-22'
        },
        {
            extendWithUid: false
        }
    ]);
});

test('http error', async () => {
    expect.assertions(2);

    mockService.mockRejectedValueOnce(httpError(500));
    core.params.from = '2020-01-21';
    core.params.to = '2020-01-22';

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
    }
});

test('service error', async () => {
    expect.assertions(3);

    mockService.mockRejectedValueOnce({ error: { message: 'ooops' } });
    core.params.from = '2020-01-21';
    core.params.to = '2020-01-22';

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('ooops');
    }
});
