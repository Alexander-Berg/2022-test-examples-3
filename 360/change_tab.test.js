'use strict';

const changeTab = require('./change_tab.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            tab: 'default',
            mids: []
        },
        service: () => mockService
    };
});

test('ходит в мопс', async () => {
    mockService.mockResolvedValueOnce();

    await changeTab(core);

    expect(mockService.mock.calls[0][0]).toBe('/change_tab');
    expect(mockService.mock.calls[0][1]).toEqual({
        dest_tab: 'relevant',
        mids: ''
    });
});

test('-> OK отвечает пустым объектом', async () => {
    mockService.mockResolvedValueOnce({});

    const result = await changeTab(core);

    expect(result).toEqual({});
});

test('ошибка с неправильной схемой запроса', async () => {
    expect.assertions(3);
    core.params = { limit: 400 };
    mockService.mockResolvedValueOnce({
        foo: 'bar'
    });

    try {
        await changeTab(core);
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
        await changeTab(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('foo');
    }
});
