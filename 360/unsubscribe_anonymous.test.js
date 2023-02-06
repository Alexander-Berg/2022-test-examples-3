'use strict';

const unsubscribeAnonymous = require('./unsubscribe_anonymous.js');
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
        config: {
            secrets: {}
        },
        service: () => mockService,
        status: status(core)
    };
});

describe('-> PERM_FAIL без нужных параметров', () => {
    it('uuid', async () => {
        delete core.params.uuid;

        const res = await unsubscribeAnonymous(core);

        expect(res.status.status).toBe(3);
    });

    it('uid', async () => {
        core.params.push_token = 'token42';

        const res = await unsubscribeAnonymous(core);

        expect(res.status.status).toBe(3);
    });

    it('push_token', async () => {
        core.params.uid = '42';

        const res = await unsubscribeAnonymous(core);

        expect(res.status.status).toBe(3);
    });
});

describe('happy path', () => {
    beforeEach(() => {
        core.params.push_token = 'token42';
        core.params.uid = '42';
        core.params.device = 'iphone';

        mockService.mockResolvedValueOnce();
    });

    it('-> OK', async () => {
        const res = await unsubscribeAnonymous(core);

        expect(res.status.status).toBe(1);
    });

    it('дергает сервис с правильными параметрами', async () => {
        await unsubscribeAnonymous(core);

        expect(mockService.mock.calls[0][0]).toBe('/v2/unsubscribe/app');
        expect(mockService.mock.calls[0][1]).toMatchObject({
            device: 'iphone',
            uid: '42',
            service: 'mail',
            uuid: 'deadbeef42'
        });
        expect(mockService.mock.calls[0][1]).toHaveProperty('token');
        expect(mockService.mock.calls[0][2]).toMatchObject({
            push_token: 'token42',
            method: 'POST'
        });
    });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.push_token = 'token42';
        core.params.uid = '42';
    });

    it('-> TMP_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(500));
        const res = await unsubscribeAnonymous(core);

        expect(res.status.status).toBe(2);
    });

    it('-> PERM_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(400));
        const res = await unsubscribeAnonymous(core);

        expect(res.status.status).toBe(3);
    });
});
