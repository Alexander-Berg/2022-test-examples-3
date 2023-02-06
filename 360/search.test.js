'use strict';

const search = require('./search.js');

const filteredLabelsMock = require('../../../test/mock/filtered-labels.json');
const envelopesMock = require('../../../test/mock/envelopes.json');
const searchMock = require('../../../test/mock/search.json');

const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
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
        request: jest.fn(),
        status: status(core)
    };
});

describe('-> TMP_FAIL', () => {
    beforeEach(() => {
        core.req.body = { query: 'find me' };
    });

    it('когда не ответили labels', async () => {
        core.request.mockRejectedValueOnce(httpError(500));

        const res = await search(core);

        expect(res.status.status).toBe(2);
    });

    it('когда meta ответил 5xx', async () => {
        mockService.mockRejectedValueOnce(httpError(500));

        const res = await search(core);

        expect(res.status.status).toBe(2);
    });
});

describe('-> PERM_FAIL', () => {
    it('когда msearch ответил 4xx', async () => {
        core.req.body = { query: 'find me' };
        mockService.mockRejectedValueOnce(httpError(400));

        const res = await search(core);

        expect(res.status.status).toBe(3);
        expect(res.status.phrase).toInclude('400');
    });
});

describe('отправляемся в сервис', () => {
    beforeEach(() => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);

        core.req.body = {
            side: 'mobile',
            scope: 'scope',
            from_date: '20170101',
            to_date: '20170531',
            msg_limit: 10,
            page_number: 1,
            query: 'find me',
            fid: '1234',
            lid: '5678',
            unread: 'yes'
        };
        mockService.mockResolvedValueOnce(envelopesMock);
    });

    it('-> OK', async () => {
        const res = await search(core);

        expect(res.status.status).toBe(1);
    });

    it('сервис вызван с правильными параметрами', async () => {
        await search(core);

        expect(mockService.mock.calls[0][0]).toBe('/search');
        expect(mockService.mock.calls[0][1]).toEqual({
            side: 'mobile',
            reqid: 'deadbeef42',
            count: 10,
            first: 0,
            from: '20170101',
            to: '20170531',
            scope: 'scope',
            request: 'find me',
            fid: '1234',
            lid: '5678',
            unread: 'yes'
        });
    });

    it('сервис вызван с правильными параметрами (withTabs)', async () => {
        core.params.withTabs = 1;
        core.req.body.fid = '-11';
        await search(core);

        expect(mockService.mock.calls[0][0]).toBe('/search');
        expect(mockService.mock.calls[0][1]).toEqual({
            side: 'mobile',
            reqid: 'deadbeef42',
            count: 10,
            first: 0,
            from: '20170101',
            to: '20170531',
            scope: 'scope',
            request: 'find me',
            tab: 'news',
            lid: '5678',
            unread: 'yes'
        });
    });

    describe('передает в сервис reqid', () => {
        it('равный uuid, если не указан', async () => {
            await search(core);

            expect(mockService.mock.calls[0][1]).toMatchObject({
                reqid: 'deadbeef42'
            });
        });

        it('если указан', async () => {
            core.params.reqid = '42';

            await search(core);

            expect(mockService.mock.calls[0][1]).toMatchObject({
                reqid: '42'
            });
        });
    });

    it('параметры по-умолчанию', async () => {
        core.req.body = { query: 'find me' };

        await search(core);

        expect(mockService.mock.calls[0][0]).toBe('/search');
        expect(mockService.mock.calls[0][1]).toMatchObject({
            count: 1,
            first: 0,
            request: 'find me'
        });
    });

    it('если указан параметр dont_save', async () => {
        core.req.body = { query: 'find me', dont_save: true };

        await search(core);

        const args = mockService.mock.calls[0][1];
        expect(args['save-request']).toBe(false);
    });

    it('если в конфиге есть searchFolderSet', async () => {
        core.req.body = { query: 'find me' };
        core.config.searchFolderSet = 'default';

        await search(core);

        const params = mockService.mock.calls[0][1];
        expect(params).toHaveProperty('folder_set');
    });

    it('отвечаем', async () => {
        mockService.mockResolvedValueOnce(searchMock);

        const res = await search(core);

        expect(res).toMatchSnapshot();
    });

    it('если указан параметр deleted', async () => {
        core.req.body = { deleted: true };

        await search(core);

        const params = mockService.mock.calls[0][1];
        expect(params).toHaveProperty('folder', 'hidden_trash');
    });
});
