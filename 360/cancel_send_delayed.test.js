'use strict';

const cancelSendDelayed = require('./cancel_send_delayed');
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

    const res = await cancelSendDelayed(core);

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

    const res = await cancelSendDelayed(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toBe('some_reason');
});

test('-> OK', async () => {
    core.auth.get.mockReturnValue(aiMock);
    core.params.mid = '42';

    mockSendbernar.mockResolvedValueOnce({
        status: 'ok'
    });

    const res = await cancelSendDelayed(core);

    expect(res).toEqual({
        status: {
            status: 1
        }
    });
});
