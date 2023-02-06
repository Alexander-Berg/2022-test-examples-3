'use strict';

const store = require('./store');
const aiMock = require('../../../test/mock/ai.json');
const status = require('../_helpers/status');

let core;
let mockService;
const mockSendbernar = jest.fn();

jest.mock('../_helpers/sendbernar.js', () => ({
    prepareParams: () => mockSendbernar(),
    error: jest.requireActual('../_helpers/sendbernar.js').error,
    filter: jest.requireActual('../_helpers/sendbernar.js').filter
}));

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            uuid: 'deadbeef42'
        },
        config: {
            secrets: {}
        },
        auth: {
            get: jest.fn()
        },
        req: {},
        yasm: {
            sum: jest.fn()
        },
        console: {
            error: jest.fn()
        },
        service: () => mockService,
        status: status(core)
    };
});

test('-> PERM_FAIL, если что-то пошло не так', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockService.mockResolvedValueOnce({});
    mockSendbernar.mockRejectedValueOnce({});

    const res = await store(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toBe('sendbernar_error');
});

test('-> PERM_FAIL, bad_request, reason', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSendbernar.mockResolvedValueOnce({
        status: 'bad_request',
        reason: 'some_reason',
        object: {
            category: 'category',
            message: 'msg',
            reason: 'reason'
        }
    });

    const res = await store(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toBe('some_reason');
});

test('-> PERM_FAIL, bad_request, no reason', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSendbernar.mockResolvedValueOnce({
        status: 'bad_request',
        reason: undefined,
        object: {
            category: 'category',
            message: 'msg',
            reason: 'reason'
        }
    });

    const res = await store(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toBe('msg: reason');
});

test('-> PERM_FAIL, bad_request, no object', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSendbernar.mockResolvedValueOnce({
        status: 'bad_request',
        reason: undefined
    });

    const res = await store(core);

    expect(res.status.phrase).toBe('sendbernar_error');
});

test('-> PERM_FAIL, bad_request', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSendbernar.mockResolvedValueOnce({
        status: 'limited',
        object: {
            messageId: 'message_id',
            category: 'category',
            message: 'msg',
            reason: 'reason'
        }
    });

    const res = await store(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toBe('limited');
});

test('ok', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSendbernar.mockResolvedValueOnce({
        status: 'ok',
        object: {
            messageId: 'message_id',
            stored: {
                mid: 'draft_mid',
                fid: 'draft_fid'
            },
            attachments: [
                {
                    name_uri_encoded: 'picture.png',
                    subtype: 'png; charset=binary',
                    type: 'image',
                    name: 'picture.png',
                    clas: 'general',
                    hid: '1.1',
                    length: 1024,
                    old_hid: '1.1.2',
                    hash: 'deadbeef=='
                },
                {
                    name_uri_encoded: 'video.mov',
                    subtype: 'quicktime; charset=binary',
                    type: 'video',
                    name: 'video.mov',
                    clas: 'general',
                    hid: '1.2',
                    length: 4096,
                    old_hid: '1.1.3',
                    hash: 'beefdead=='
                }
            ]
        }
    });

    const res = await store(core);

    expect(res).toEqual({
        status: {
            status: 1
        },
        mid: 'draft_mid',
        fid: 'draft_fid',
        attachments: {
            attachment: [
                {
                    hid: '1.1',
                    old_hid: '1.1.2',
                    narod: false,
                    display_name: 'picture.png',
                    class: 'image',
                    size: 1024,
                    mime_type: 'image/png',
                    download_url: '',
                    preview_supported: true,
                    is_inline: false,
                    hash: 'deadbeef=='
                },
                {
                    hid: '1.2',
                    old_hid: '1.1.3',
                    narod: false,
                    display_name: 'video.mov',
                    class: 'video',
                    size: 4096,
                    mime_type: 'video/quicktime',
                    download_url: '',
                    preview_supported: false,
                    is_inline: false,
                    hash: 'beefdead=='
                }
            ]
        }
    });
});
