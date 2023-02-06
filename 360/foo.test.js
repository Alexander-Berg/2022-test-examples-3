'use strict';

const foo = require('./foo.js');
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
    const res = await foo(core);

    expect(res.status).toBe(3);
});

describe('параметры, уходящие в сервис правильные', () => {
    it('mids', async () => {
        mockMops.mockResolvedValueOnce([]);
        core.params.mids = '42,43,45';

        await foo(core);

        expect(mockMops).toHaveBeenCalledWith('/spam', {
            mids: '42,43,45'
        });
    });

    it('tids', async () => {
        mockMops.mockResolvedValueOnce([]);
        core.params.tids = '1,2,3,5,8';

        await foo(core);

        expect(mockMops).toHaveBeenCalledWith('/spam', {
            tids: '1,2,3,5,8'
        });
    });
});

test('-> OK', async () => {
    core.params.mids = '1';
    mockMops.mockResolvedValueOnce();

    const result = await foo(core);

    expect(result).toEqual({ status: 1 });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.mids = '1';
    });

    it('-> PERM_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(400));

        const result = await foo(core);

        expect(result.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(500));

        const result = await foo(core);

        expect(result.status).toBe(2);
    });
});
