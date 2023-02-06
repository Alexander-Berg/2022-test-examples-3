'use strict';

const send = require('./send');
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
    mockSendbernar.mockRejectedValueOnce('sendbernar_error');

    const res = await send(core);

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

    const res = await send(core);

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

    const res = await send(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toBe('msg: reason');
});

test('-> PERM_FAIL, bad_request, no object', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSendbernar.mockResolvedValueOnce({
        status: 'bad_request',
        reason: undefined
    });

    const res = await send(core);

    expect(res.status.phrase).toBe('sendbernar_error');
});

test('-> PERM_FAIL, limited', async () => {
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

    const res = await send(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toBe('limited');
});

test('-> OK', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSendbernar.mockResolvedValueOnce({
        status: 'ok',
        object: {
            messageId: 'message_id'
        }
    });

    const res = await send(core);

    expect(res).toEqual({
        status: {
            status: 1
        }
    });
});

test('-> OK, limited', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSendbernar.mockResolvedValueOnce({
        status: 'ok',
        object: {
            messageId: 'message_id',
            limited: [ {
                login: 'some_login',
                domain: 'some_domain',
                limit: '228'
            } ]
        }
    });

    const res = await send(core);

    expect(res).toEqual({
        status: {
            status: 1
        },
        limited: [ {
            login: 'some_login',
            domain: 'some_domain',
            limit: '228'
        } ]
    });
});
