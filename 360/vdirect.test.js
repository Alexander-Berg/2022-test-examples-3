'use strict';

const vdirect = require('./vdirect.js');
const status = require('../_helpers/status');

let core;
let mockRequest;

beforeEach(() => {
    mockRequest = jest.fn();

    core = {
        params: {},
        status: status(core),
        request: mockRequest
    };
});

test('-> PERM_FAIL если нет url в параметрах', async () => {
    const res = await vdirect(core);

    expect(res.status.status).toBe(3);
});

test('вызывает модель', async () => {
    core.params.url = 'http://ya.ru';
    mockRequest.mockResolvedValueOnce({ status: 'ok' });

    await vdirect(core);

    const [ model, params ] = mockRequest.mock.calls[0];
    expect(model).toBe('vdirect');
    expect(params).toEqual({
        url: 'http://ya.ru'
    });
});
