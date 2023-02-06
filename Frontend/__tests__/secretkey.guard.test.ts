/* eslint-disable @typescript-eslint/no-explicit-any */

import { APP_GUARD } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import { AsyncStorageModule } from '@yandex-int/nest-common';
import supertest from 'supertest';

import { BlackboxModule, BlackboxService } from '../../blackbox';
import { YandexuidService } from '../../yandexuid';
import { SecretkeyGuard } from '../secretkey.guard';
import { SecretkeyModule } from '../secretkey.module';
import { SecretkeyService } from '../secretkey.service';
import { SecretkeyConfigService } from '../secretkeyConfig.service';
import { SecretkeyOptions } from '../types';

import { SecretkeyGuardTestModule } from './module/secretkeyGuardTest.module';

const blackboxService = {
    getBlackbox: jest.fn() as jest.Mock,
};

const yandexuidService = {
    getYandexuid: jest.fn() as jest.Mock,
};

// @ts-ignore
SecretkeyService.prototype.getSecretkey.disableMemoize();
// @ts-ignore
SecretkeyService.prototype.validate.disableMemoize();

const getAppWithGlobalGuard = async () => {
    const moduleRef = await Test.createTestingModule({
        imports: [
            SecretkeyGuardTestModule,
            BlackboxModule.forRoot({ api: 'pass-test.yandex.ru' }),
            AsyncStorageModule.forRoot(),
            SecretkeyModule,
        ],
        providers: [
            {
                provide: APP_GUARD,
                useClass: SecretkeyGuard,
            },
        ],
    })
        .overrideProvider(BlackboxService)
        .useValue(blackboxService)
        .overrideProvider(YandexuidService)
        .useValue(yandexuidService)
        .compile();

    const app = moduleRef.createNestApplication();
    await app.init();

    return app;
};

const getApp = async () => {
    const moduleRef = await Test.createTestingModule({
        imports: [
            SecretkeyGuardTestModule,
            BlackboxModule.forRoot({ api: 'pass-test.yandex.ru' }),
            AsyncStorageModule.forRoot(),
        ],
    })
        .overrideProvider(BlackboxService)
        .useValue(blackboxService)
        .overrideProvider(YandexuidService)
        .useValue(yandexuidService)
        .compile();

    const app = moduleRef.createNestApplication();
    await app.init();

    return app;
};

const getKey = async (options?: SecretkeyOptions) => {
    const service = new SecretkeyService(
        blackboxService as any,
        yandexuidService as any,
        new SecretkeyConfigService(options),
        { method: 'GET' } as any,
    );

    return service.getSecretkey();
};

describe('SecretkeyGuard', () => {
    describe('as global guard', () => {
        beforeAll(() => {
            blackboxService.getBlackbox.mockReturnValue({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValue('yandexuid_foo');
        });

        test('should pass method ignored methods', async () => {
            const app = await getAppWithGlobalGuard();

            return supertest(app.getHttpServer()).get('/simple/get').expect(200);
        });

        test('should pass correct key', async () => {
            const key = await getKey();
            const app = await getAppWithGlobalGuard();

            return supertest(app.getHttpServer())
                .post('/simple/post')
                .set('csrf-token', key || '')
                .expect(201);
        });

        test('should forbidden incorrect key', async () => {
            const key = await getKey();
            const app = await getAppWithGlobalGuard();

            return supertest(app.getHttpServer()).post('/simple/post').set('csrf-token', `${key}_foo`).expect(403);
        });
    });

    describe('as controller guard', () => {
        beforeAll(() => {
            blackboxService.getBlackbox.mockReturnValue({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValue('yandexuid_foo');
        });

        test('should pass method ignored methods', async () => {
            const app = await getApp();

            return supertest(app.getHttpServer()).get('/controller/get').expect(200);
        });

        test('should pass correct key', async () => {
            const key = await getKey();
            const app = await getApp();

            return supertest(app.getHttpServer())
                .post('/controller/post')
                .set('csrf-token', key || '')
                .expect(201);
        });

        test('should forbidden incorrect key', async () => {
            const key = await getKey();
            const app = await getApp();

            return supertest(app.getHttpServer()).post('/controller/post').set('csrf-token', `${key}_foo`).expect(403);
        });
    });

    describe('as method guard', () => {
        beforeAll(() => {
            blackboxService.getBlackbox.mockReturnValue({ uid: 'uid_foo', status: 'VALID' });
            yandexuidService.getYandexuid.mockReturnValue('yandexuid_foo');
        });

        test('should pass method ignored methods', async () => {
            const app = await getApp();

            return supertest(app.getHttpServer()).get('/method/get').expect(200);
        });

        test('should pass correct key', async () => {
            const key = await getKey();
            const app = await getApp();

            return supertest(app.getHttpServer())
                .post('/method/post')
                .set('csrf-token', key || '')
                .expect(201);
        });

        test('should forbidden incorrect key', async () => {
            const key = await getKey();
            const app = await getApp();

            return supertest(app.getHttpServer()).post('/method/post').set('csrf-token', `${key}_foo`).expect(403);
        });
    });
});
