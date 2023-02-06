'use strict';

const method = require('./gap.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const duffmanErrors = require('@yandex-int/duffman').errors;

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            logins: [ 'login', 'login2' ],
            date_from: '2019-11-01',
            date_to: '2019-11-30'
        },
        service: () => mockService
    };
});

test('невалидные параметры', async () => {
    expect.assertions(3);

    core.params = {};

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('invalid params schema');
    }
});

test('ходит в gap', async () => {
    mockService.mockResolvedValueOnce({ gaps: [] });

    await method(core);

    expect(mockService).toHaveBeenCalledWith('/gap-api/api/gaps_find/', {
        person_login: [ 'login', 'login2' ],
        date_from: '2019-11-01',
        date_to: '2019-11-30'
    });
});

test('отвечает', async () => {
    mockService.mockResolvedValueOnce({
        gaps: [
            {
                comment: 'Москва – Saint Petersburg – Москва\n',
                workflow: 'trip',
                work_in_absence: true,
                date_from: '2019-10-31T00:00:00',
                to_notify: [],
                person_login: 'login',
                date_to: '2019-11-02T00:00:00',
                full_day: true,
                id: 1234
            },
            {
                comment: '.',
                workflow: 'absence',
                work_in_absence: false,
                date_from: '2019-11-12T00:00:00',
                to_notify: [],
                person_login: 'login2',
                date_to: '2019-11-13T00:00:00',
                full_day: true,
                id: 5678
            }
        ],
        total: 2,
        limit: 100,
        page: 0,
        pages: 1
    });

    const res = await method(core);

    expect(res).toMatchSnapshot();
});

test('обрабатывает http ошибки', async () => {
    expect.assertions(3);

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

    mockService.mockRejectedValueOnce(new Error('wtf'));

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('wtf');
    }
});
