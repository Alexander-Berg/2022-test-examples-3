'use strict';

const messageHeader = require('./message_header.js');

const filteredLabelsMock = require('../../../test/mock/filtered-labels.json');
const envelopeMock = require('../../../test/mock/envelope.json');

const status = require('../_helpers/status');
const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockMeta;

beforeEach(() => {
    mockMeta = jest.fn();
    core = {
        params: {},
        service: () => mockMeta,
        request: () => filteredLabelsMock
    };
    core.status = status(core);
});

test('-> PERM_FAIL mid', async () => {
    const res = await messageHeader(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('mid is mandatory');
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockMeta.mockResolvedValueOnce(envelopeMock);
        core.params.mid = '42';
    });

    it('метод', async () => {
        await messageHeader(core);

        const meth = mockMeta.mock.calls[0][0];
        expect(meth).toBe('/filter_search');
    });

    it('params', async () => {
        await messageHeader(core);

        const params = mockMeta.mock.calls[0][1];
        expect(params).toEqual({ mids: [ '42' ] });
    });
});

test('-> OK', async () => {
    core.params.mid = '42';
    mockMeta.mockResolvedValueOnce(envelopeMock);

    const result = await messageHeader(core);

    expect(result.status.status).toBe(1);
    expect(result).toHaveProperty('message');
    expect(result).toHaveProperty('widgets');
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.mid = '42';
    });

    it('-> PERM_FAIL', async () => {
        mockMeta.mockRejectedValueOnce(httpError(400));

        const result = await messageHeader(core);

        expect(result.status.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockMeta.mockRejectedValueOnce(httpError(500));

        const result = await messageHeader(core);

        expect(result.status.status).toBe(2);
    });
});
