'use strict';

const messageBody = require('./message_body.js');

const mbodyMock = require('../../../test/mock/mbody2.json').response;
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
        mids: '1,163536961468896255,163536961468896253',
        novdirect: 'yes'
    };
    mockMbody.mockResolvedValue(mbodyMock);

    await messageBody(core);

    expect(mockMbody).toBeCalledTimes(3);
});

describe('прокидывает правильные параметры в сервис', () => {
    [ 'yes', 'no' ].forEach((novdirect) => {
        it(`novdirect: ${novdirect}`, async () => {
            core.params = {
                mids: '1,163536961468896255,163536961468896253',
                novdirect
            };

            mockMbody.mockResolvedValue(mbodyMock);

            const flags = novdirect === 'yes' ?
                'XmlStreamerOn,XmlStreamerMobile,ShowContentMeta,NoVdirectLinksWrap' :
                'XmlStreamerOn,XmlStreamerMobile,ShowContentMeta';

            await messageBody(core);

            expect(mockMbody.mock.calls[0]).toEqual([ '/message', { mid: '1', flags } ]);
            expect(mockMbody.mock.calls[1]).toEqual([ '/message', { mid: '163536961468896255', flags } ]);
            expect(mockMbody.mock.calls[2]).toEqual([ '/message', { mid: '163536961468896253', flags } ]);
        });
    });
});

test('прокидывает правильные параметры в сервис без novdirect', async () => {
    core.params = {
        mids: '1,163536961468896255,163536961468896253'
    };
    mockMbody.mockResolvedValue(mbodyMock);
    const flags = 'XmlStreamerOn,XmlStreamerMobile,ShowContentMeta';

    await messageBody(core);

    expect(mockMbody.mock.calls[0]).toEqual([ '/message', { mid: '1', flags } ]);
    expect(mockMbody.mock.calls[1]).toEqual([ '/message', { mid: '163536961468896255', flags } ]);
    expect(mockMbody.mock.calls[2]).toEqual([ '/message', { mid: '163536961468896253', flags } ]);
});

test('отвечает PERM_FAIL если сервис ответил 500 (CUSTOM_ERROR)', async () => {
    core.params = { mids: '42,43,44' };
    mockMbody
        .mockResolvedValueOnce(mbodyMock)
        .mockRejectedValueOnce(new CUSTOM_ERROR({}))
        .mockResolvedValueOnce(mbodyMock);

    const res = await messageBody(core);

    expect(res[0].status.status).toBe(1);
    expect(res[1].status.status).toBe(3);
    expect(res[2].status.status).toBe(1);
});
