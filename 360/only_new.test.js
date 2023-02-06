'use strict';

const onlyNew = require('./only_new.js');

const filteredLabelsMock = require('../../../test/mock/filtered-labels.json');
const envelopesMock = require('../../../test/mock/envelopes.json');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

const status = require('../_helpers/status');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        req: {},
        service: () => mockService,
        request: jest.fn(),
        getServiceOptions: jest.fn()
    };
    core.status = status(core);
});

describe('-> TMP_FAIL', () => {
    it('когда не ответили labels', async () => {
        core.request.mockRejectedValueOnce(httpError(500));
        mockService.mockResolvedValueOnce({});

        const res = await onlyNew(core);

        expect(res.status.status).toBe(2);
    });

    it('когда не ответил meta', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);
        mockService.mockRejectedValueOnce(httpError(500));

        const res = await onlyNew(core);

        expect(res.status.status).toBe(2);
    });
});

describe('отправляемся в сервис', () => {
    beforeEach(() => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);
        mockService.mockResolvedValueOnce(envelopesMock);

        core.req.body = {
            first: 0,
            last: 10
        };
    });

    it('-> OK', async () => {
        const res = await onlyNew(core);

        expect(res.status.status).toBe(1);
    });

    describe('сервис вызван с правильными параметрами', () => {
        it('с дефолтными параметрами пагинации', async () => {
            await onlyNew(core);

            expect(mockService.mock.calls[0][0]).toEqual('/messages_unread');
            expect(mockService.mock.calls[0][1]).toEqual({
                count: 10,
                first: 0
            });
        });

        it('с параметрами пагинации', async () => {
            core.params = { first: 0, last: 40 };

            await onlyNew(core);

            expect(mockService.mock.calls[0][0]).toEqual('/messages_unread');
            expect(mockService.mock.calls[0][1]).toEqual({
                count: 40,
                first: 0
            });
        });
    });
});
