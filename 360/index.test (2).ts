/// <reference types="jest" />

import { errors } from '@duffman-int/core';
import furita from './index';
import type { FuritaServiceCore } from './index';

describe('service', () => {
    function prepare() {
        const core: FuritaServiceCore = {
            got: jest.fn(),
            ip: '127.0.0.1',
            auth: { uid: '42' },
            requestId: 'fake',
        } as unknown as FuritaServiceCore;
        const options = {
            service: { url: '@/' },
            headers: {},
            orgId: 100500,
        };

        return { core, options };
    }

    it('default path', async() => {
        const { core, options } = prepare();

        await furita(core, 'method', { test: 1 }, options);
        expect(core.got).toHaveBeenCalledWith('@/method', {
            headers: {
                'Yandex-Cloud-Request-ID': 'fake',
                'x-user-ip': '127.0.0.1',
            },
            query: {
                orgid: 100500,
                test: 1,
            },
        });
    });

    it('transforms http error', async() => {
        const { core, options } = prepare();

        (core.got as jest.MockedFunction<typeof core.got>).mockRejectedValue(
            new errors.HTTP_ERROR({
                statusCode: 400,
                body: {
                    code: 'code',
                    message: 'message',
                    params: 'params',
                },
            } as any),
        );
        await expect(furita(core, 'method', { test: 1 }, options)).rejects
            .toEqual({
                type: 'INVALID_PARAMS',
                code: 'code',
                message: 'message',
                error: {
                    type: 'INVALID_PARAMS',
                    code: 'code',
                    message: 'message',
                    params: 'params',
                },
            });
    });

    it('pass other errors', async() => {
        const { core, options } = prepare();

        (core.got as jest.MockedFunction<typeof core.got>).mockRejectedValue(
            new errors.EXTERNAL_ERROR({ test: 'me' } as any),
        );
        await expect(furita(core, 'method', { test: 1 }, options)).rejects
            .toEqual({
                error: {
                    test: 'me',
                },
            });
    });
});
