'use strict';

const updateFolder = require('./update_folder.js');
const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        service: () => mockService,
        status: status(core)
    };
});

test('-> PERM_FAIL без параметра fid', async () => {
    const res = await updateFolder(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('fid param is missing');
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce([]);
        core.params.fid = '42';
    });

    it('метод', async () => {
        await updateFolder(core);

        const meth = mockService.mock.calls[0][0];
        expect(meth).toBe('/folders/update');
    });

    it('name', async () => {
        core.params.name = 'folder42';

        await updateFolder(core);

        const params = mockService.mock.calls[0][1];
        expect(params.name).toBe('folder42');
    });

    it('parent_fid', async () => {
        core.params.parent_fid = '2';

        await updateFolder(core);

        const params = mockService.mock.calls[0][1];
        expect(params.parent_fid).toBe('2');
    });
});

test('-> OK', async () => {
    core.params.fid = '1';
    mockService.mockResolvedValueOnce();

    const result = await updateFolder(core);

    expect(result.status.status).toBe(1);
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.fid = '1';
    });

    it('-> PERM_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(400));

        const result = await updateFolder(core);

        expect(result.status.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(500));

        const result = await updateFolder(core);

        expect(result.status.status).toBe(2);
    });
});
