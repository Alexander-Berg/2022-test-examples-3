'use strict';

const push = require('./push.js');
const authMock = require('../../../test/mock/ai.json');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });
const status = require('../_helpers/status');

let core;
let mockPush;
let mockMeta;

beforeEach(() => {
    mockPush = jest.fn();
    mockMeta = jest.fn();
    core = {
        params: {
            uuid: 'deadbeef42'
        },
        req: {
            headers: {}
        },
        auth: {
            get: jest.fn().mockReturnValue({})
        },
        config: {
            secrets: {}
        },
        service: (service) => service === 'meta' ? mockMeta : mockPush
    };
    core.status = status(core);
});

test('сразу ОК-аем с параметром os=courier', async () => {
    core.params.os = 'courier';

    const res = await push(core);

    expect(res.status.status).toBe(1);
});

describe('unsubscribe', () => {
    beforeEach(() => {
        core.params.unsubscribe = 'yes';
    });

    it('-> PERM_FAIL без uid', async () => {
        delete core.params.uuid;

        const res = await push(core);

        expect(res.status.status).toBe(3);
        expect(res.status.phrase).toInclude('uuid is mandatory');
    });

    it('передает верные параметры в got', async () => {
        mockPush.mockResolvedValueOnce({});

        await push(core);

        expect(mockPush.mock.calls[0][0]).toBe('/v2/unsubscribe/app');
        expect(mockPush.mock.calls[0][1]).toMatchObject({
            service: 'mail',
            client: 'mobile-api-v1-push',
            uuid: 'deadbeef42'
        });
        expect(mockPush.mock.calls[0][1]).toHaveProperty('token');
    });

    it('-> OK', async () => {
        mockPush.mockResolvedValueOnce({});

        const res = await push(core);

        expect(res.status.status).toBe(1);
    });

    describe('ошибки', () => {
        it('-> PERM_FAIL', async () => {
            mockPush.mockRejectedValueOnce(httpError(400));

            const res = await push(core);

            expect(res.status.status).toBe(3);
        });

        it('-> TMP_FAIL', async () => {
            mockPush.mockRejectedValueOnce(httpError(500));

            const res = await push(core);

            expect(res.status.status).toBe(2);
        });
    });
});

describe('subscribe', () => {
    beforeEach(() => {
        core.req.headers['user-agent'] = 'test user agent';
        core.params.push_token = 'token42';
        mockPush.mockResolvedValueOnce({});
    });

    it('-> PERM_FAIL без uid и push_token', async () => {
        delete core.params.uuid;

        const res = await push(core);

        expect(res.status.status).toBe(3);
        expect(res.status.phrase).toInclude('push_token and uuid are mandatory');
    });

    describe('передает верные параметры в got', () => {
        it('метод', async () => {
            await push(core);

            expect(mockPush.mock.calls[0][0]).toBe('/v2/subscribe/app');
        });

        it('plaform', async () => {
            core.params.os = 'ios';

            await push(core);

            expect(mockPush.mock.calls[0][1].platform).toBe('apns');
        });

        it('platform', async () => {
            core.params.platform = 'apnsqueue';

            await push(core);

            expect(mockPush.mock.calls[0][1].platform).toBe('apnsqueue');
        });

        it('device', async () => {
            core.params.device = 'device';

            await push(core);

            expect(mockPush.mock.calls[0][1].device).toBe('device');
        });

        it('os_version', async () => {
            core.params.os_version = '12.1.4';

            await push(core);

            expect(mockPush.mock.calls[0][1].os_version).toBe('12.1.4');
        });

        it('параметры без exclude_fids', async () => {
            await push(core);

            const params = mockPush.mock.calls[0][1];
            const options = mockPush.mock.calls[0][2];

            expect(params).toMatchObject({
                app_name: 'ru.yandex.mail.new',
                client: 'test_user_agent',
                filter: '',
                platform: 'gcm',
                service: 'mail',
                uuid: 'deadbeef42'
            });
            expect(params).toHaveProperty('token');
            expect(options.body).toEqual({
                push_token: 'token42'
            });
        });

        it('с exclude_fids', async () => {
            core.params.exclude_fids = '1,2,3';

            await push(core);

            const params = mockPush.mock.calls[0][1];
            const options = mockPush.mock.calls[0][2];
            expect(params).toMatchObject({
                app_name: 'ru.yandex.mail.new',
                client: 'test_user_agent',
                filter: '{"vars":{"EXFID":{"fid":{"$eq":["1","2","3"]}}},"rules":[{"if":"EXFID","do":"send_silent"}]}', // eslint-disable-line max-len
                platform: 'gcm',
                service: 'mail',
                uuid: 'deadbeef42'
            });
            expect(params).toHaveProperty('token');
            expect(options.body).toEqual({
                push_token: 'token42'
            });
        });

        it('с exclude_tabs', async () => {
            mockMeta.mockResolvedValueOnce({ folders: { 42: { symbolicName: { title: 'inbox' } } } });
            core.params.exclude_tabs = 'news,social';

            await push(core);

            const params = mockPush.mock.calls[0][1];
            const options = mockPush.mock.calls[0][2];
            expect(params).toMatchObject({
                app_name: 'ru.yandex.mail.new',
                client: 'test_user_agent',
                filter: '{"vars":{"INBOX":{"fid":{"$eq":["42"]}},"EXTAB":{"tab":{"$eq":["news","social"]}}},"rules":[{"if":"INBOX & EXTAB","do":"send_silent"}]}', // eslint-disable-line max-len
                platform: 'gcm',
                service: 'mail',
                uuid: 'deadbeef42'
            });
            expect(params).toHaveProperty('token');
            expect(options.body).toEqual({
                push_token: 'token42'
            });
        });

        it('с exclude_fids и exclulde_tabs', async () => {
            mockMeta.mockResolvedValueOnce({ folders: { 42: { symbolicName: { title: 'inbox' } } } });
            core.params.exclude_fids = '1,2,3';
            core.params.exclude_tabs = 'default,news';

            await push(core);

            const params = mockPush.mock.calls[0][1];
            const options = mockPush.mock.calls[0][2];
            expect(params).toMatchObject({
                app_name: 'ru.yandex.mail.new',
                client: 'test_user_agent',
                filter: '{"vars":{"EXFID":{"fid":{"$eq":["1","2","3"]}},"INBOX":{"fid":{"$eq":["42"]}},"EXTAB":{"tab":{"$eq":["default","news"]}}},"rules":[{"if":"EXFID","do":"send_silent"},{"if":"INBOX & EXTAB","do":"send_silent"}]}', // eslint-disable-line max-len
                platform: 'gcm',
                service: 'mail',
                uuid: 'deadbeef42'
            });
            expect(params).toHaveProperty('token');
            expect(options.body).toEqual({
                push_token: 'token42'
            });
        });

        it('заголовок x-bb-connectionid', async () => {
            core.auth.get.mockReturnValueOnce(authMock);

            await push(core);

            const options = mockPush.mock.calls[0][2];
            expect(options.headers).toMatchObject({
                'x-bb-connectionid': 's:1509442136308:chMepuYpXwwHAQAAuAYCKg:9'
            });
        });
    });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.req.headers['user-agent'] = 'test user agent';
        core.params.push_token = 'token42';
    });

    it('-> PERM_FAIL', async () => {
        mockPush.mockRejectedValueOnce(httpError(400));

        const res = await push(core);

        expect(res.status.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockPush.mockRejectedValueOnce(httpError(500));

        const res = await push(core);

        expect(res.status.status).toBe(2);
    });
});
