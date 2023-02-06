'use strict';

const corpAva = require('./corp_ava.js');
const corpAvaMock = require('../../../test/mock/corp-ava.json');
const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockAva;

beforeEach(() => {
    mockAva = jest.fn();
    core = {
        params: {},
        config: {
            IS_CORP: true
        },
        req: {
            headers: {}
        },
        res: {
            set: jest.fn()
        },
        service: () => mockAva
    };
    core.status = status(core);
});

test('-> PERM_FAIL когда нет login в параметрах', async () => {
    const res = await corpAva(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('login param is missing');
});

test('-> PERM_FAIL для не-корпа', async () => {
    core.config.IS_CORP = false;

    const res = await corpAva(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('service unavailable');
});

test('-> OK happy path', async () => {
    mockAva.mockResolvedValueOnce({
        statusCode: 200,
        ...corpAvaMock
    });
    core.params.login = 'arcadiy';

    const result = await corpAva(core);

    expect(result).toBeInstanceOf(Buffer);
    expect(core.res.set).toHaveBeenCalledWith({
        'Cache-Control': 'max-age=0, must-revalidate, proxy-revalidate, no-cache, no-store, private',
        'Content-Type': 'text/html; charset=utf-8',
        'Expires': 'Thu, 01 Jan 1970 00:00:01 GMT',
        'Pragma': 'no-cache',
        'content-type': 'image/png'
    });
});

test('login with dots', async () => {
    core.req.headers.authorization = 'OAuth XXX';
    core.params.login = 'ar.ca.diy';

    mockAva.mockResolvedValueOnce({
        statusCode: 200,
        ...corpAvaMock
    });

    const result = await corpAva(core);

    expect(result).toBeInstanceOf(Buffer);
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.login = 'arcadiy';
    });

    it('-> TMP_FAIL когда ручка отвечает 5xx', async () => {
        mockAva.mockRejectedValueOnce(httpError(500));

        const result = await corpAva(core);

        expect(result.status.status).toBe(2);
    });

    it('-> PERM_FAIL когда ручка отвечает 4xx', async () => {
        core.req.headers.authorization = 'OAuth XXX';
        mockAva.mockRejectedValueOnce(httpError(400));

        const result = await corpAva(core);

        expect(result.status.status).toBe(3);
    });
});
