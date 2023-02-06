'use strict';

const deleteLabel = require('./delete_label.js');
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

test('-> PERM_FAIL без параметра lid', async () => {
    const res = await deleteLabel(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('lid param is missing');
});

test('параметры, уходящие в сервис правильные', async () => {
    mockMops.mockResolvedValueOnce([]);
    core.params.lid = '42';

    await deleteLabel(core);

    expect(mockMops).toHaveBeenCalledWith('/labels/delete', {
        lid: '42'
    });
});

test('-> OK', async () => {
    core.params.lid = '42';
    mockMops.mockResolvedValueOnce({});

    const result = await deleteLabel(core);

    expect(result.status.status).toBe(1);
    expect(result).toHaveProperty('taskId');
    expect(result).toHaveProperty('taskType');
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.lid = '42';
    });

    it('-> PERM_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(400));

        const result = await deleteLabel(core);

        expect(result.status.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(500));

        const result = await deleteLabel(core);

        expect(result.status.status).toBe(2);
    });
});
