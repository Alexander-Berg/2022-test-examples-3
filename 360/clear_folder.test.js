'use strict';

const clearFolder = require('./clear_folder.js');
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

test('-> PERM_FAIL без параметра fid', async () => {
    const res = await clearFolder(core);

    expect(res.status).toBe(3);
});

test('параметры, уходящие в сервис правильные', async () => {
    mockMops.mockResolvedValueOnce([]);
    core.params.fid = '42';

    await clearFolder(core);

    expect(mockMops).toHaveBeenCalledWith('/purge', {
        fid: '42'
    });
});

test('-> OK', async () => {
    core.params.fid = '1';
    mockMops.mockResolvedValueOnce();

    const result = await clearFolder(core);

    expect(result).toEqual({ status: 1 });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.fid = '1';
    });

    it('-> TMP_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(500));

        const result = await clearFolder(core);

        expect(result.status).toBe(2);
    });

    it('-> PERM_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(400));

        const result = await clearFolder(core);

        expect(result.status).toBe(3);
    });
});
