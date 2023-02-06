'use strict';

const setSettings = require('./set_settings.js');
const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            mobile_sign: 'Написано на моем iPhone X 512G'
        },
        service: () => mockService,
        status: status(core)
    };
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce();
    });

    it('params', async () => {
        await setSettings(core);

        const params = mockService.mock.calls[0][1];
        expect(params).toEqual({ mobile_sign: 'Написано на моем iPhone X 512G' });
    });

    it('метод', async () => {
        await setSettings(core);

        const meth = mockService.mock.calls[0][0];
        expect(meth).toBe('/update_profile');
    });
});

test('-> OK', async () => {
    mockService.mockResolvedValueOnce({});

    const result = await setSettings(core);

    expect(result).toEqual({ status: 1 });
});

describe('ошибки', () => {
    it('-> PERM_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(400));

        const result = await setSettings(core);

        expect(result.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(500));

        const result = await setSettings(core);

        expect(result.status).toBe(2);
    });
});
