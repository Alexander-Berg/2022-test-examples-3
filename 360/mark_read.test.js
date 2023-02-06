'use strict';

const markRead = require('./mark_read.js');
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
    const res = await markRead(core);

    expect(res.status).toBe(3);
    expect(res.phrase).toInclude('tids or mids param is missing');
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockMops.mockResolvedValueOnce([]);
    });

    it('метод', async () => {
        core.params.tids = '42';

        await markRead(core);

        expect(mockMops.mock.calls[0][0]).toBe('/mark');
    });

    it('tids', async () => {
        core.params.tids = '42';

        await markRead(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.tids).toBe('42');
    });

    it('mids', async () => {
        core.params.mids = '43';

        await markRead(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.mids).toBe('43');
    });

    it('status', async () => {
        core.params.mids = '43';

        await markRead(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.status).toBe('read');
    });
});

test('-> OK', async () => {
    core.params.tids = '42';
    mockMops.mockResolvedValueOnce({});

    const result = await markRead(core);

    expect(result).toEqual({ status: 1 });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.tids = '42';
    });

    it('-> PERM_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(400));
        const result = await markRead(core);

        expect(result.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(500));
        const result = await markRead(core);

        expect(result.status).toBe(2);
    });
});
