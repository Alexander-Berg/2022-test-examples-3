'use strict';

const attach = require('./attach.js');
const status = require('../_helpers/status');

const ai = require('../../../test/mock/ai.json');

const { HTTP_ERROR, CUSTOM_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });
const customError = (error) => new CUSTOM_ERROR(error);

let core;
let mockMeta;

beforeEach(() => {
    mockMeta = jest.fn();
    core = {
        params: {},
        auth: {
            get: jest.fn().mockReturnValue(ai)
        },
        service: () => mockMeta,
        config: {
            urls: {
                webattach: 'TEST_WEBATTACH_URL'
            }
        }
    };
    core.status = status(core);
});

describe('-> PERM_FAIL с недостающими параметрами', () => {
    it('не указан mid и hid', async () => {
        const res = await attach(core);

        expect(res.status.status).toBe(3);
    });
    it('не указан mid', async () => {
        core.params.hid = '1.1';

        const res = await attach(core);

        expect(res.status.status).toBe(3);
    });
    it('не указан hid', () => {
        core.params.mid = '424242';
        return attach(core).then((res) => {
            expect(res.status.status).toBe(3);
        });
    });
});

test('-> PERM_FAIL если не указан mid и hid', async () => {
    const result = await attach(core);

    expect(result.status.status).toBe(3);
});

describe('-> OK happy path', () => {
    beforeEach(() => {
        const response = {
            1.1: 'sid42'
        };
        mockMeta.mockResolvedValueOnce(response);
        core.params.mid = '424242';
        core.params.hid = '1.1';
    });

    it('без опциональных параметров name и thumb', async () => {
        core.params.mid = '424242';
        core.params.hid = '1.1';

        const result = await attach(core);

        expect(result.status.status).toBe(1);
        expect(result.url).toInclude('sid42');
    });

    it('all-in', async () => {
        core.params.name = 'attachment.jpg';
        core.params.thumb = 'y';

        const result = await attach(core);

        expect(result.status.status).toBe(1);
        expect(result.url).toEqual('TEST_WEBATTACH_URL/message_part_real/attachment.jpg?sid=sid42&thumb=y');
    });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.mid = '424242';
        core.params.hid = '1.1';
    });

    it('-> PERM_FAIL когда сервис отвечает 4xx', async () => {
        mockMeta.mockRejectedValueOnce(httpError(400));

        const res = await attach(core);

        expect(res.status.status).toBe(3);
    });

    it('-> PERM_FAIL когда сервис отвечает ошибкой 4xx', async () => {
        mockMeta.mockRejectedValueOnce(customError({
            error: {
                code: 2001, message: 'not authenticated', reason: ''
            }
        }));

        const res = await attach(core);

        expect(res.status.status).toBe(3);
    });

    it('-> TMP_FAIL когда сервис отвечает 5xx', async () => {
        mockMeta.mockRejectedValueOnce(httpError(500));

        const res = await attach(core);

        expect(res.status.status).toBe(2);
    });
});
