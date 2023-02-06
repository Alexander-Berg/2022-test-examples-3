/* eslint-disable @typescript-eslint/no-explicit-any */

import { SecretkeyService } from '../secretkey.service';
import { SecretkeyConfigService } from '../secretkeyConfig.service';
import { SecretkeyOptions } from '../types';

const blackboxService = {
    getBlackbox: jest.fn() as jest.Mock,
};

const yandexuidService = {
    getYandexuid: jest.fn() as jest.Mock,
};

const defaultReq = {
    method: 'GET',
};

// @ts-ignore
SecretkeyService.prototype.getSecretkey.disableMemoize();
// @ts-ignore
SecretkeyService.prototype.validate.disableMemoize();

const getService = (req: any = defaultReq, options?: SecretkeyOptions) => {
    const context = { req } as any;

    return new SecretkeyService(
        blackboxService as any,
        yandexuidService as any,
        new SecretkeyConfigService(options),
        context,
    );
};

describe('SecretkeyService', () => {
    describe('generate', () => {
        test('should return key', async () => {
            const service = getService();

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const key = await service.getSecretkey();

            expect(key).not.toBeFalsy();
        });

        test('should return key with version 2', async () => {
            const service = getService(defaultReq, { version: 2, salt: 'abc' });

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const key = await service.getSecretkey();

            expect(key).not.toBeFalsy();
        });
    });

    describe('validate v1', () => {
        test('should be valid', async () => {
            const generateService = getService();

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const key = await generateService.getSecretkey();
            const validateService = getService({ method: 'POST', headers: { 'csrf-token': key } });

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const valid = await validateService.validate();

            expect(valid).toBe(true);
        });

        test('should be invalid with different uid', async () => {
            const generateService = getService();

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const key = await generateService.getSecretkey();
            const validateService = getService({ method: 'POST', headers: { 'csrf-token': key } });

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_bar', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const valid = await validateService.validate();

            expect(valid).toBe(false);
        });

        test('should be invalid with different yandexuid', async () => {
            const generateService = getService();

            blackboxService.getBlackbox.mockReturnValueOnce({});
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const key = await generateService.getSecretkey();
            const validateService = getService({ method: 'POST', headers: { 'csrf-token': key } });

            blackboxService.getBlackbox.mockReturnValueOnce({});
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_bar');

            const valid = await validateService.validate();

            expect(valid).toBe(false);
        });
    });

    describe('validate v2', () => {
        test('should be valid', async () => {
            const generateService = getService(defaultReq, { version: 2, salt: 'salt_foo' });

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const key = await generateService.getSecretkey();
            const validateService = getService(
                { method: 'POST', headers: { 'csrf-token': key } },
                { version: 2, salt: 'salt_foo' },
            );

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const valid = await validateService.validate();

            expect(valid).toBe(true);
        });

        test('should be invalid with different uid', async () => {
            const generateService = getService(defaultReq, { version: 2, salt: 'salt_foo' });

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const key = await generateService.getSecretkey();
            const validateService = getService(
                { method: 'POST', headers: { 'csrf-token': key } },
                { version: 2, salt: 'salt_foo' },
            );

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_bar', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const valid = await validateService.validate();

            expect(valid).toBe(false);
        });

        test('should be invalid with different yandexuid', async () => {
            const generateService = getService(defaultReq, { version: 2, salt: 'salt_foo' });

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_foo');

            const key = await generateService.getSecretkey();
            const validateService = getService(
                { method: 'POST', headers: { 'csrf-token': key } },
                { version: 2, salt: 'salt_foo' },
            );

            blackboxService.getBlackbox.mockReturnValueOnce({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValueOnce('yandexuid_bar');

            const valid = await validateService.validate();

            expect(valid).toBe(false);
        });
    });
});
