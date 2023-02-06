'use strict';

jest.useFakeTimers('modern');
jest.setSystemTime(1580554500751);

let mockTimeCacheData = null;
jest.mock('./_helpers/get-location.js', () => () => 'FAKE_LOCATION');
jest.mock('../../../helpers/time-cache.js', () => jest.fn(() => ({
    isEmpty: () => !mockTimeCacheData,
    get: () => mockTimeCacheData,
    set: (data) => {
        mockTimeCacheData = data;
    }
})));

const method = require('./flags');
const ApiError = require('../../../routes/helpers/api-error.js');
const uazMock = require('../../../test/mock/uaz-flags-fake.json');
const globalFlagsResponseMock = require('../../../test/mock/global-flags.json');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockUserSplit;
let mockGlobalFlags;

beforeEach(() => {
    mockUserSplit = jest.fn();
    mockGlobalFlags = jest.fn();
    core = {
        params: {
            client: 'aphone',
            uuid: 'deadbeef42'
        },
        res: {
            items: jest.fn()
        },
        config: {
            USER_IP: 'FAKE_IP'
        },
        request: mockUserSplit,
        service: () => mockGlobalFlags
    };
    mockTimeCacheData = null;
});

test('пробрасывает 400 без параметра client', async () => {
    expect.assertions(3);
    delete core.params.client;

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('client param is missing');
    }
});

test('пробрасывает 400 без параметра uuid', async () => {
    expect.assertions(3);
    delete core.params.uuid;

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('uuid param is missing');
    }
});

test('пробрасывает 400 с невалидными `params`', async () => {
    expect.assertions(3);

    core.params.params = {
        a: { type: 'string', value: 1 }
    };

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('invalid params schema');
    }
});

test('-> OK', async () => {
    mockUserSplit.mockResolvedValueOnce(uazMock);
    mockGlobalFlags.mockResolvedValueOnce(globalFlagsResponseMock);

    const res = await method(core);

    expect(res).toMatchSnapshot();
});

test('-> OK w/params', async () => {
    core.params.params = {
        hasTeamAccount: { type: 'boolean', value: true }
    };

    mockUserSplit.mockResolvedValueOnce(uazMock);
    mockGlobalFlags.mockResolvedValueOnce(globalFlagsResponseMock);

    const res = await method(core);

    expect(res).toMatchSnapshot();
});

test('дергает модель и сервис с правильными параметрами', async () => {
    mockUserSplit.mockResolvedValueOnce(uazMock);
    mockGlobalFlags.mockResolvedValueOnce(globalFlagsResponseMock);

    await method(core);

    expect(mockUserSplit.mock.calls).toMatchSnapshot();
    expect(mockGlobalFlags.mock.calls).toMatchSnapshot();
});

test('отвечаем пустыми экспериментами, если сервисы не ответили', async () => {
    mockUserSplit.mockResolvedValueOnce({
        ExpBoxes: '',
        Handlers: []
    });
    mockGlobalFlags.mockResolvedValueOnce({});

    const res = await method(core);

    expect(res.configurations).toEqual([]);
});

describe('ошибки', () => {
    it('user-split ответил 400 -> отвечаем 500', async () => {
        expect.assertions(2);
        mockUserSplit.mockRejectedValueOnce(httpError(400));

        try {
            await method(core);
        } catch (err) {
            expect(err).toBeInstanceOf(ApiError);
            expect(err.code).toBe(500);
        }
    });

    it('user-split ответил 500 -> отвечаем 500', async () => {
        expect.assertions(2);
        mockUserSplit.mockRejectedValueOnce(httpError(500));

        try {
            await method(core);
        } catch (err) {
            expect(err).toBeInstanceOf(ApiError);
            expect(err.code).toBe(500);
        }
    });
});
