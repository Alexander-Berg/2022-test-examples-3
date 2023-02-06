'use strict';

const classification = require('./classification.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        service: () => mockService,
        hideParamInLog: jest.fn()
    };
});

test('прокидывает правильные параметры в запрос', async () => {
    core.params.mids = [ '1', '2', '3' ];
    mockService.mockResolvedValueOnce([]);

    await classification(core);

    const params = mockService.mock.calls[0][1];
    const options = mockService.mock.calls[0][2];
    expect(params.side).toBe('mobile');
    expect(options.method).toBe('post');
    expect(options.json).toBe(true);
    expect(options.body).toBe('["1","2","3"]');
});

test('возвращает правильный ответ', async () => {
    core.params.mids = [ '1', '2', '3' ];
    mockService.mockResolvedValueOnce([ '1', '2', '3' ]);

    const response = await classification(core);

    expect(response.mids).toEqual([ '1', '2', '3' ]);
});

test('если сервис валится, отвечаем 500', async () => {
    expect.assertions(2);
    core.params.mids = [ '1', '2', '3' ];
    mockService.mockRejectedValueOnce();

    try {
        await classification(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
    }
});

test('отвечает 400, если не указан mids', async () => {
    expect.assertions(2);

    try {
        await classification(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('отвечает 400, если mids пустой', async () => {
    expect.assertions(2);
    core.params.mids = [];

    try {
        await classification(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('отвечает 400, если mids неправильного формата', async () => {
    expect.assertions(2);
    core.params.mids = [ 1, 'qwe', 'q1' ];

    try {
        await classification(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toInclude('invalid params schema');
    }
});
