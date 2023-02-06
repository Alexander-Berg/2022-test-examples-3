'use strict';

const updateLabel = require('./update_label.js');
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

test('-> PERM_FAIL без параметра lid', async () => {
    const res = await updateLabel(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('lid param is missing');
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce([]);
        core.params.lid = '42';
    });

    it('метод', async () => {
        await updateLabel(core);

        const meth = mockService.mock.calls[0][0];
        expect(meth).toBe('/labels/update');
    });

    it('name', async () => {
        core.params.name = 'label42';

        await updateLabel(core);

        const params = mockService.mock.calls[0][1];
        expect(params.name).toBe('label42');
    });

    it('цвет пробрасывается как есть', async () => {
        core.params.color = '304255257';

        await updateLabel(core);

        const params = mockService.mock.calls[0][1];
        expect(params.color).toBe('304255257');
    });
});

test('-> OK', async () => {
    core.params.lid = '1';
    mockService.mockResolvedValueOnce();

    const result = await updateLabel(core);

    expect(result.status.status).toBe(1);
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.lid = '1';
    });

    it('-> PERM_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(400));

        const result = await updateLabel(core);

        expect(result.status.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(500));

        const result = await updateLabel(core);

        expect(result.status.status).toBe(2);
    });
});
