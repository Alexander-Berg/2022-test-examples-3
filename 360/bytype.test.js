'use strict';

const filteredLabelsMock = require('../../../test/mock/filtered-labels.json');

const envelopesMock = require('../../../test/mock/envelopes.json');
const bytype = require('./bytype.js');

const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockMsearch;

beforeEach(() => {
    mockMsearch = jest.fn();
    core = {
        params: {},
        service: () => mockMsearch,
        request: () => filteredLabelsMock
    };
    core.status = status(core);
});

test('-> PERM_FAIL без параметров', async () => {
    const res = await bytype(core);

    expect(res.status.status).toBe(3);
});

describe('отправляемся в сервис', () => {
    it('с нужными параметрами', async () => {
        core.params = {
            type: 2,
            page_number: 2,
            msg_limit: 10
        };
        mockMsearch.mockResolvedValueOnce(envelopesMock);

        await bytype(core);

        expect(mockMsearch.mock.calls).toMatchSnapshot();
    });

    it('-> OK если ответили', async () => {
        core.params = {
            type: 2,
            page_number: 2,
            msg_limit: 10
        };
        mockMsearch.mockResolvedValueOnce(envelopesMock);

        const res = await bytype(core);

        expect(res.status.status).toBe(1);
    });

    it('-> PERM_FAIL если 4xx', async () => {
        core.params = {
            type: 2,
            page_number: 2,
            msg_limit: 10
        };
        mockMsearch.mockRejectedValueOnce(httpError(400));

        const res = await bytype(core);

        expect(res.status.status).toBe(3);
    });

    it('-> TMP_FAIL если 5xx', async () => {
        core.params = {
            category: 'people'
        };
        mockMsearch.mockRejectedValueOnce(httpError(500));

        const res = await bytype(core);

        expect(res.status.status).toBe(2);
    });
});
