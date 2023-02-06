'use strict';

const method = require('./purge_items.js');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        service: () => mockService
    };
});

test('отвечаем 400, если нет параметров', async () => {
    expect.assertions(2);

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toInclude('tids or mids or fid param is missing');
    }
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce([]);
    });

    it('метод', async () => {
        core.params.tids = '42';

        await method(core);

        const meth = mockService.mock.calls[0][0];
        expect(meth).toBe('/purge');
    });

    it('tids', async () => {
        core.params.tids = '42';

        await method(core);

        const params = mockService.mock.calls[0][1];
        expect(params.tids).toBe('42');
    });

    it('mids', async () => {
        core.params.mids = '43';

        await method(core);

        const params = mockService.mock.calls[0][1];
        expect(params.mids).toBe('43');
    });

    it('fid', async () => {
        core.params.fid = '1';

        await method(core);

        const params = mockService.mock.calls[0][1];
        expect(params.fid).toBe('1');
    });
});

test('-> OK', async () => {
    core.params.tids = '42';
    mockService.mockResolvedValueOnce({});

    const result = await method(core);

    expect(result).toEqual({});
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.tids = '42';
    });

    it('сервис отвечает 400', async () => {
        expect.assertions(1);
        mockService.mockRejectedValueOnce(httpError(400));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(400);
        }
    });

    it('сервис отвечает 500', async () => {
        expect.assertions(1);
        mockService.mockRejectedValueOnce(httpError(500));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(500);
        }
    });

    it('сервис отвечает 502', async () => {
        expect.assertions(1);
        mockService.mockRejectedValueOnce(httpError(502));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(500);
        }
    });
});
