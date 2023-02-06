'use strict';

const messages = require('./messages.js');

const filteredLabelsMock = require('../../../test/mock/filtered-labels.json');
const messagesByFolderMock = require('../../../test/mock/meta/messages-by-folder.json');
const messagesByThreadMock = require('../../../test/mock/meta/messages-by-thread.json');
const messagesByLabelMock = require('../../../test/mock/meta/messages-by-label.json');
const threadsByFolderMock = require('../../../test/mock/meta/threads-by-folder.json');
const threadsWithRecipientsMock = require('../../../test/mock/meta/threads-with-recipients.json');
const taksaMock = require('../../../test/mock/taksa.json');

const { CUSTOM_ERROR, HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

const messagesSchema = require('../_helpers/messages-schema.json');
const validateSchema = require('../_helpers/validate-schema.js')(messagesSchema);

const status = require('../_helpers/status');

let core;
let mockMeta;
let mockTaksa;

beforeEach(() => {
    mockMeta = jest.fn();
    mockTaksa = jest.fn();
    core = {
        params: {
            withWidgets: '1'
        },
        service: (service) => service === 'taksa' ? mockTaksa : mockMeta,
        request: jest.fn().mockResolvedValue(filteredLabelsMock),
        console: {
            error: jest.fn()
        },
        yasm: {
            sum: jest.fn()
        },
        getServiceOptions: jest.fn(),
        res: {}
    };
    core.status = status(core);
});

test('-> PERM_FAIL без requests', async () => {
    const res = await messages(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('no requests provided');
});

test('-> PERM_FAIL с невалидным requests', async () => {
    core.params.requests = { foo: 'bar' };

    const res = await messages(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('invalid requests schema');
});

describe('запрос с получателями', () => {
    beforeEach(() => {
        mockMeta.mockResolvedValueOnce(threadsWithRecipientsMock);

        core.params.requests = [
            {
                fid: '4',
                first: 0,
                last: 1,
                threaded: true,
                returnIfModified: true
            }
        ];
    });

    it('без параметра не отдает recipients', async () => {
        const res = await messages(core);

        expect(Object.keys(res[0].messageBatch.messages[0])).not.toInclude('recipients');
    });

    describe('с параметром recipientsCount', () => {
        it('добавляет в ответ recipients', async () => {
            core.params.requests[0].recipientsCount = 5;

            const res = await messages(core);

            expect(Object.keys(res[0].messageBatch.messages[0])).toInclude('recipients');
        });

        it('учитывает значение recipientsCount', async () => {
            core.params.requests[0].recipientsCount = 5;

            const res = await messages(core);

            expect(res[0].messageBatch.messages[0].recipients).toMatchSnapshot();
        });
    });
});

describe('одиночный запрос', () => {
    beforeEach(() => {
        core.params.requests = [ {
            fid: '1',
            first: 0,
            last: 10,
            returnIfModified: true
        } ];
    });

    it('нет ответа labels', async () => {
        core.request.mockRejectedValueOnce(new CUSTOM_ERROR({ message: 'omg' }));
        mockMeta.mockResolvedValueOnce();

        const res = await messages(core);

        expect(res.status.status).toBe(2);
        expect(res.status.phrase).toInclude('LABELS_REQUEST_ERROR');
    });

    it('ответ только от labels', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);
        mockMeta.mockRejectedValueOnce(new CUSTOM_ERROR({ code: 555, message: 'uzhs' }));

        const res = await messages(core);

        await validateSchema(res);
        expect(res[0].header.error).toBe(3);
    });

    it('happy path', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);
        mockMeta.mockResolvedValueOnce(messagesByFolderMock);
        mockTaksa.mockResolvedValueOnce(taksaMock);

        const res = await messages(core);

        await validateSchema(res);
        expect(res).toMatchSnapshot();
    });

    describe('запрашивает мету с правильными параметрами', () => {
        beforeEach(() => {
            core.request.mockResolvedValueOnce(filteredLabelsMock);
            mockMeta.mockResolvedValueOnce({});
        });

        it('threads_by_folder', async () => {
            core.params.requests = [ {
                first: '0',
                last: '20',
                fid: '1',
                returnIfModified: true,
                threaded: true,
                md5: 'eccc3e7867d0c8e1f34c2acd8b116121'
            } ];

            await messages(core);

            const args = mockMeta.mock.calls[0];
            expect(args[0]).toBe('/threads_by_folder');
            expect(args[1]).toEqual({ first: '0', count: 20, fid: '1' });
        });

        it('messages_by_folder', async () => {
            core.params.requests = [ {
                first: '0',
                last: '10',
                fid: '13',
                returnIfModified: true,
                threaded: false,
                md5: '34cc3e7867decc02ac8e11612fcd8b11'
            } ];

            await messages(core);

            const args = mockMeta.mock.calls[0];
            expect(args[0]).toBe('/messages_by_folder');
            expect(args[1]).toEqual({ first: '0', count: 10, fid: '13' });
        });

        it('messages_by_thread', async () => {
            core.params.requests = [ {
                first: '0',
                last: '10',
                tid: '42',
                returnIfModified: true,
                threaded: false,
                md5: '7d0c8ec2ac3e786111f34cd8becc6121'
            } ];

            await messages(core);

            const args = mockMeta.mock.calls[0];
            expect(args[0]).toBe('/messages_by_thread');
            expect(args[1]).toEqual({ first: '0', count: 10, tid: '42' });
        });

        it('messages_by_label', async () => {
            core.params.requests = [ {
                first: '0',
                last: '10',
                lid: '42',
                returnIfModified: true,
                threaded: false,
                md5: 'a5f7983c82f24c58fea552baf5041f18'
            } ];

            await messages(core);

            const args = mockMeta.mock.calls[0];
            expect(args[0]).toBe('/messages_by_label');
            expect(args[1]).toEqual({ first: '0', count: 10, lid: '42' });
        });
    });
});

describe('множественный запрос', () => {
    beforeEach(() => {
        core.params.requests = [
            {
                fid: '1',
                first: 0,
                last: 4,
                threaded: true,
                returnIfModified: true
            },
            {
                fid: '2',
                first: 0,
                last: 4,
                threaded: false,
                returnIfModified: true
            },
            {
                lid: '3',
                first: 0,
                last: 4,
                threaded: true,
                md5: 'a552baf5041f18a5f7983c82f24c58fe',
                returnIfModified: true
            },
            {
                tid: '4',
                first: 0,
                last: 4,
                threaded: true,
                returnIfModified: true
            }
        ];
    });

    it('нет ответа labels', async () => {
        core.request.mockRejectedValueOnce(new CUSTOM_ERROR({ message: 'omg' }));
        mockMeta.mockResolvedValue();

        const res = await messages(core);

        expect(res.status.status).toBe(2);
        expect(res.status.phrase).toInclude('LABELS_REQUEST_ERROR');
    });

    it('ответ только от labels', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);
        mockMeta.mockRejectedValue(new CUSTOM_ERROR({ code: 555, message: 'uzhs' }));

        const res = await messages(core);

        await validateSchema(res);
        expect(res.map((r) => r.header.error)).toEqual([ 3, 3, 3, 3 ]);
    });

    it('happy path (все ответили, иногда ответила такса)', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);

        mockMeta
            .mockResolvedValueOnce(threadsByFolderMock)
            .mockResolvedValueOnce(messagesByFolderMock)
            .mockResolvedValueOnce(messagesByThreadMock)
            .mockResolvedValueOnce(messagesByLabelMock);

        mockTaksa
            .mockResolvedValueOnce(new CUSTOM_ERROR({ error: 'some error' }))
            .mockResolvedValueOnce(httpError(403))
            .mockResolvedValueOnce(taksaMock)
            .mockResolvedValueOnce(new CUSTOM_ERROR({ error: 'some error' }));

        const res = await messages(core);

        await validateSchema(res);
        expect(res).toMatchSnapshot();
    });

    it('не все ответили', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);
        mockMeta
            .mockRejectedValueOnce(httpError(500))
            .mockResolvedValueOnce(messagesByFolderMock)
            .mockRejectedValueOnce(httpError(404))
            .mockResolvedValueOnce(messagesByLabelMock);

        const res = await messages(core);

        await validateSchema(res);
        expect(res).toMatchSnapshot();
    });
});

describe('устанавливает mmapiStatus', () => {
    beforeEach(() => {
        core.params.requests = [
            {
                fid: '1',
                first: 0,
                last: 4,
                threaded: true,
                returnIfModified: true
            },
            {
                fid: '2',
                first: 0,
                last: 4,
                threaded: false,
                returnIfModified: true
            },
            {
                lid: '3',
                first: 0,
                last: 4,
                threaded: true,
                md5: 'a552baf5041f18a5f7983c82f24c58fe',
                returnIfModified: true
            },
            {
                tid: '4',
                first: 0,
                last: 4,
                threaded: true,
                returnIfModified: true
            }
        ];
    });

    it('1', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);

        mockMeta
            .mockResolvedValueOnce(threadsByFolderMock)
            .mockResolvedValueOnce(messagesByFolderMock)
            .mockResolvedValueOnce(messagesByThreadMock)
            .mockResolvedValueOnce(messagesByLabelMock);

        const res = await messages(core);

        await validateSchema(res);
        expect(core.res.mmapiStatus).toBe(1);
    });

    it('3', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);

        mockMeta
            .mockResolvedValueOnce(threadsByFolderMock)
            .mockRejectedValueOnce(new CUSTOM_ERROR({ error: { code: 2 } }))
            .mockResolvedValueOnce(messagesByThreadMock)
            .mockResolvedValueOnce(messagesByLabelMock);

        const res = await messages(core);

        await validateSchema(res);
        expect(core.res.mmapiStatus).toBe(3);
    });
});
