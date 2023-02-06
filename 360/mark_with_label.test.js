'use strict';

const markWithLabel = require('./mark_with_label.js');
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
    const res = await markWithLabel(core);

    expect(res.status).toBe(3);
    expect(res.phrase).toInclude('tids or mids or lid param is missing');
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        core.params.lid = '1';
        mockMops.mockResolvedValueOnce([]);
    });

    describe('метод', () => {
        describe('unlabel', () => {
            it('с параметром mark=0', async () => {
                core.params.mark = '0';
                core.params.tids = '42';

                await markWithLabel(core);

                const meth = mockMops.mock.calls[0][0];
                expect(meth).toBe('/unlabel');
            });

            it('без параметра mark', async () => {
                core.params.tids = '42';

                await markWithLabel(core);

                const meth = mockMops.mock.calls[0][0];
                expect(meth).toBe('/unlabel');
            });
        });

        it('label', async () => {
            core.params.tids = '42';
            core.params.mark = '1';

            await markWithLabel(core);

            const meth = mockMops.mock.calls[0][0];
            expect(meth).toBe('/label');
        });
    });

    it('tids', async () => {
        core.params.tids = '42';

        await markWithLabel(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.tids).toBe('42');
    });

    it('mids', async () => {
        core.params.mids = '43';

        await markWithLabel(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.mids).toBe('43');
    });
});

test('-> OK', async () => {
    core.params.lid = '42';
    core.params.tids = '42';
    mockMops.mockResolvedValueOnce({});

    const result = await markWithLabel(core);

    expect(result).toEqual({ status: 1 });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.lid = '42';
        core.params.tids = '42';
    });

    it('-> PERM_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(400));

        const result = await markWithLabel(core);

        expect(result.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(500));

        const result = await markWithLabel(core);

        expect(result.status).toBe(2);
    });
});
