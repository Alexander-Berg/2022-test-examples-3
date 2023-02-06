'use strict';

const withAttachments = require('./with_attachments.js');

const filteredLabelsMock = require('../../../test/mock/filtered-labels.json');
const envelopesMock = require('../../../test/mock/envelopes.json');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });
const status = require('../_helpers/status');

let core;
let mockRequest;
let mockService;

beforeEach(() => {
    mockRequest = jest.fn();
    mockService = jest.fn();
    core = {
        params: {},
        req: {},
        request: mockRequest,
        service: () => mockService,
        getServiceOptions: jest.fn()
    };
    core.status = status(core);
});

describe('-> TMP_FAIL', () => {
    it('когда не ответили labels', async () => {
        mockRequest.mockRejectedValueOnce(httpError(500));
        mockService.mockResolvedValueOnce({});

        const res = await withAttachments(core);

        expect(res.status.status).toBe(2);
    });

    it('когда не ответил meta', async () => {
        mockRequest.mockResolvedValueOnce(filteredLabelsMock);
        mockService.mockRejectedValueOnce(httpError(500));

        const res = await withAttachments(core);

        expect(res.status.status).toBe(2);
    });
});

describe('отправляемся в сервис', () => {
    beforeEach(() => {
        mockRequest.mockResolvedValueOnce(filteredLabelsMock);
        mockService.mockResolvedValueOnce(envelopesMock);

        core.req.body = {
            first: 0,
            last: 10
        };
    });

    it('-> OK', async () => {
        const res = await withAttachments(core);

        expect(res.status.status).toBe(1);
    });

    describe('сервис вызван с правильными параметрами', () => {
        it('с дефолтными параметрами пагинации', async () => {
            await withAttachments(core);

            expect(mockService).toHaveBeenCalledWith('/messages_with_attaches', {
                count: 10,
                first: 0
            }, {});
        });

        it('с параметрами пагинации', async () => {
            core.params = { first: 0, last: 40 };

            await withAttachments(core);

            expect(mockService).toHaveBeenCalledWith('/messages_with_attaches', {
                count: 40,
                first: 0
            }, {});
        });
    });
});
