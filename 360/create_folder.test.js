'use strict';

const createFolder = require('./create_folder.js');
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

test('-> PERM_FAIL без параметра name', async () => {
    const res = await createFolder(core);

    expect(res.status.status).toBe(3);
});

test('параметры, уходящие в сервис правильные', async () => {
    mockMops.mockResolvedValueOnce([]);
    core.params.name = 'folder42';
    core.params.parent_fid = '41';
    core.params.symbol = 'inbox';

    await createFolder(core);

    expect(mockMops).toHaveBeenCalledWith('/folders/create', {
        parent_fid: '41',
        name: 'folder42',
        symbol: 'inbox'
    });
});

test('-> OK', async () => {
    core.params.name = '1';
    mockMops.mockResolvedValueOnce();

    const result = await createFolder(core);

    expect(result.status.status).toBe(1);
});

test('-> PERM_FAIL', async () => {
    core.params.name = '1';
    mockMops.mockRejectedValueOnce(httpError(400));

    const result = await createFolder(core);

    expect(result.status.status).toBe(3);
});

test('-> TMP_FAIL', async () => {
    core.params.name = '1';
    mockMops.mockRejectedValueOnce(httpError(500));

    const result = await createFolder(core);

    expect(result.status.status).toBe(2);
});
