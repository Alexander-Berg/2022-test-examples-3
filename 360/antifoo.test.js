'use strict';

const antifoo = require('./antifoo.js');
const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockMops;

beforeEach(() => {
    mockMops = jest.fn();
    core = {
        params: {},
        service: () => mockMops
    };
    core.status = status(core);
});

test('-> PERM_FAIL без нужных параметров', async () => {
    const res = await antifoo(core);

    expect(res.status).toBe(3);
});

test('параметры, уходящие в сервис правильные', async () => {
    mockMops.mockResolvedValueOnce([]);
    core.params.mids = '42,43,45';

    await antifoo(core);

    expect(mockMops).toHaveBeenCalledWith('/unspam', {
        mids: '42,43,45'
    });
});

test('-> OK', async () => {
    core.params.mids = '1';
    mockMops.mockResolvedValueOnce();

    const result = await antifoo(core);

    expect(result).toEqual({ status: 1 });
});

test('-> PERM_FAIL', async () => {
    core.params.mids = '1';
    mockMops.mockRejectedValueOnce(httpError(400));

    const result = await antifoo(core);

    expect(result.status).toBe(3);
});

test('-> TMP_FAIL', async () => {
    core.params.mids = '1';
    mockMops.mockRejectedValueOnce(httpError(500));

    const result = await antifoo(core);

    expect(result.status).toBe(2);
});
