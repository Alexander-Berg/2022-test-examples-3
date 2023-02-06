'use strict';

const deleteItems = require('./delete_items.js');
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

test('-> PERM_FAIL без параметров', async () => {
    const res = await deleteItems(core);

    expect(res.status).toBe(3);
    expect(res.phrase).toInclude('tids or mids param is missing');
});

test('-> PERM_FAIL с отрицательными mid', async () => {
    core.params.mids = '-1,2,3';
    const res = await deleteItems(core);

    expect(res.status).toBe(3);
    expect(res.phrase).toInclude('tids or mids param is invalid');
});

test('-> PERM_FAIL с отрицательными tid', async () => {
    core.params.tids = '-1,2,3';
    const res = await deleteItems(core);

    expect(res.status).toBe(3);
    expect(res.phrase).toInclude('tids or mids param is invalid');
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockMops.mockResolvedValueOnce([]);
    });

    it('метод', async () => {
        core.params.tids = '42';

        await deleteItems(core);

        expect(mockMops.mock.calls[0][0]).toBe('/remove');
    });

    it('tids', async () => {
        core.params.tids = '42';

        await deleteItems(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.tids).toBe('42');
    });

    it('mids', async () => {
        core.params.mids = '43';

        await deleteItems(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.mids).toBe('43');
    });
});

test('-> OK', async () => {
    core.params.tids = '42';
    mockMops.mockResolvedValueOnce({});

    const result = await deleteItems(core);

    expect(result).toEqual({ status: 1 });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.tids = '42';
    });

    it('-> PERM_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(400));
        const result = await deleteItems(core);

        expect(result.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(500));
        const result = await deleteItems(core);

        expect(result.status).toBe(2);
    });
});
