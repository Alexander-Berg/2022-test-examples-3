'use strict';

const deleteFolder = require('./delete_folder.js');
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
    const res = await deleteFolder(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('fid param is missing');
});

test('параметры, уходящие в сервис правильные', async () => {
    mockMops.mockResolvedValueOnce([]);
    core.params.fid = '42';
    core.params.parent_fid = '41';

    await deleteFolder(core);
    expect(mockMops).toHaveBeenCalledWith('/folders/delete', {
        fid: '42'
    });
});

test('-> OK', async () => {
    core.params.fid = '42';
    mockMops.mockResolvedValueOnce({});

    const result = await deleteFolder(core);

    expect(result.status.status).toBe(1);
    expect(result).toHaveProperty('taskId');
    expect(result).toHaveProperty('taskType');
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.fid = '42';
    });

    it('-> PERM_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(400));

        const result = await deleteFolder(core);

        expect(result.status.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(500));

        const result = await deleteFolder(core);

        expect(result.status.status).toBe(2);
    });
});
