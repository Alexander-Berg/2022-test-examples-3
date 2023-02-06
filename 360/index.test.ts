/// <reference types="jest" />

import { errors } from '@duffman-int/core';
import directory from './index';
import type { DirectoryServiceCore } from './index';

describe('service', () => {
    function prepare() {
        const core: DirectoryServiceCore = {
            got: jest.fn(),
            ip: '127.0.0.1',
            auth: { uid: '42' },
            requestId: 'fake',
        } as unknown as DirectoryServiceCore;
        const options: any = {
            service: { url: '@/' },
            headers: {},
            addUidHeader: true,
            orgId: 100500,
            uid: void 0,
            method: void 0,
        };

        return { core, options };
    }

    it('default path', async() => {
        const { core, options } = prepare();

        await directory(core, 'method', { test: 1 }, options);
        expect(core.got).toHaveBeenCalledWith('@/method', {
            headers: {
                'Yandex-Cloud-Request-ID': 'fake',
                'x-org-id': 100500,
                'x-uid': '42',
                'x-user-ip': '127.0.0.1',
            },
            query: {
                test: 1,
            },
        });
    });

    it('skips optional headers', async() => {
        const { core, options } = prepare();

        options.addUidHeader = false;
        options.orgId = void 0;
        options.method = 'DELETE';
        await directory(core, 'method', { test: 1 }, options);
        expect(core.got).toHaveBeenCalledWith('@/method', {
            headers: {
                'Yandex-Cloud-Request-ID': 'fake',
                'x-user-ip': '127.0.0.1',
            },
            query: {
                test: 1,
            },
            method: 'DELETE',
            allowEmpty: true,
        });
    });

    it('use uid from options', async() => {
        const { core, options } = prepare();

        options.uid = '1';
        await directory(core, 'method', { test: 1 }, options);
        expect(core.got).toHaveBeenCalledWith('@/method', {
            headers: {
                'Yandex-Cloud-Request-ID': 'fake',
                'x-org-id': 100500,
                'x-uid': '1',
                'x-user-ip': '127.0.0.1',
            },
            query: {
                test: 1,
            },
        });
    });

    it('transforms http error', async() => {
        const { core, options } = prepare();

        (core.got as jest.MockedFunction<typeof core.got>).mockRejectedValue(
            new errors.HTTP_ERROR({
                statusCode: 422,
                body: {
                    code: 'code',
                    message: 'message',
                    params: 'params',
                },
            } as any),
        );
        await expect(directory(core, 'method', { test: 1 }, options)).rejects
            .toEqual({
                type: 'INVALID_PARAMS',
                code: 'code',
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
        await expect(directory(core, 'method', { test: 1 }, options)).rejects
            .toEqual({
                error: {
                    test: 'me',
                },
            });
    });
});
