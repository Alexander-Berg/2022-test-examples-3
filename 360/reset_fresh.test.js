'use strict';

const resetFresh = require('./reset_fresh.js');
const status = require('../_helpers/status');

const { CUSTOM_ERROR, HTTP_ERROR } = require('@yandex-int/duffman').errors;
const { AKITA_ERROR } = require('@ps-int/mail-lib').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            uuid: 'deadbeef42'
        },
        req: {
            body: {}
        },
        config: {
            secrets: {}
        },
        service: () => mockService,
        request: jest.fn().mockResolvedValue({}),
        status: status(core)
    };
});

test('-> ОК если ручка отвечает успехом', async () => {
    mockService.mockResolvedValueOnce({});

    const result = await resetFresh(core);

    expect(result.status.status).toBe(1);
});

test('-> TMP_FAIL если ручка отвечает ошибкой', async () => {
    mockService.mockRejectedValue(new AKITA_ERROR(new CUSTOM_ERROR({
        error: {
            code: 0,
            message: 'Unknown error',
            reason: 'No such account for suid 112000000033082'
        }
    })));

    const result = await resetFresh(core);

    expect(result.status.status).toBe(2);
});

describe('ошибки', () => {
    it('-> TMP_FAIL если ручка отвечает 5xx', async () => {
        mockService.mockRejectedValueOnce(httpError(500));

        const result = await resetFresh(core);

        expect(result.status.status).toBe(2);
    });

    it('-> PERM_FAIL если ручка отвечает 4xx', async () => {
        mockService.mockRejectedValueOnce(httpError(400));

        const result = await resetFresh(core);

        expect(result.status.status).toBe(3);
    });
});
