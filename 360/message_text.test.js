'use strict';

const method = require('./message_text.js');

const mbodyMock = require('../../../test/mock/message_text.json');
const ai = require('../../../test/mock/ai.json');

const status = require('../_helpers/status');
const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;

let core;
let mockMbody;

beforeEach(() => {
    mockMbody = jest.fn();
    core = {
        params: {},
        service: () => mockMbody,
        auth: {
            get: jest.fn().mockReturnValue(ai)
        },
        console: {
            error: jest.fn()
        },
        yasm: {
            sum: jest.fn()
        }
    };
    core.status = status(core);
});

test('вызывает сервис для каждого мида', async () => {
    core.params = {
        mids: '1,163536961468896255,163536961468896253'
    };
    mockMbody.mockResolvedValue(mbodyMock[0]);

    await method(core);

    expect(mockMbody).toBeCalledTimes(3);
});

test('прокидывает правильные параметры в сервис', async () => {
    core.params = {
        mids: '1,163536961468896255,163536961468896253'
    };
    mockMbody.mockResolvedValue(mbodyMock[0]);

    await method(core);

    expect(mockMbody.mock.calls[0]).toEqual([ '/v1/message/text', { mid: '1' } ]);
    expect(mockMbody.mock.calls[1]).toEqual([ '/v1/message/text', { mid: '163536961468896255' } ]);
    expect(mockMbody.mock.calls[2]).toEqual([ '/v1/message/text', { mid: '163536961468896253' } ]);
});

test('отвечает PERM_FAIL если сервис ответил 500 (CUSTOM_ERROR)', async () => {
    core.params = { mids: '42,43,44' };
    mockMbody
        .mockResolvedValueOnce(mbodyMock[0])
        .mockRejectedValueOnce(new CUSTOM_ERROR({}))
        .mockResolvedValueOnce(mbodyMock[1]);

    const res = await method(core);

    expect(res[0].status.status).toBe(1);
    expect(res[1].status.status).toBe(3);
    expect(res[2].status.status).toBe(1);
});
