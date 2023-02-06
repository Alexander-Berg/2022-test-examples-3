'use strict';

jest.mock('@ps-int/mail-lib', () => ({
    constants: {
        PROTECTED_SETTINGS: [ 'fakeProtectedSetting' ]
    },
    errors: {
        AKITA_ERROR: class AKITA_ERROR_MOCK {}
    }
}));

const setParameters = require('./set_parameters.js');
const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            params: 'foo=bar:boo=baz'
        },
        service: () => mockService,
        status: status(core)
    };
});

test('-> PERM_FAIL без параметров', async () => {
    delete core.params.params;

    const res = await setParameters(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('params is mandatory');
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce();
    });

    it('params', async () => {
        await setParameters(core);

        const params = mockService.mock.calls[0][1];
        expect(params).toEqual({ foo: 'bar', boo: 'baz' });
    });

    it('метод', async () => {
        await setParameters(core);

        const meth = mockService.mock.calls[0][0];
        expect(meth).toBe('/update_params');
    });
});

test('фильтрует защищенные настройки', async () => {
    mockService.mockResolvedValueOnce({});

    core.params = {
        params: 'foo=bar:boo=baz:fakeProtectedSetting=value'
    };

    await setParameters(core);

    const params = mockService.mock.calls[0][1];
    expect(params).toEqual({ foo: 'bar', boo: 'baz' });
});

test('не вызывает сервис, если нечего сохранять', async () => {
    core.params = {
        params: 'fakeProtectedSetting=value'
    };

    await setParameters(core);

    expect(mockService).toBeCalledTimes(0);
});

test('-> OK', async () => {
    mockService.mockResolvedValueOnce({});

    const result = await setParameters(core);

    expect(result.status).toBe(1);
});

describe('ошибки', () => {
    it('-> PERM_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(400));

        const result = await setParameters(core);

        expect(result.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(500));

        const result = await setParameters(core);

        expect(result.status).toBe(2);
    });
});
