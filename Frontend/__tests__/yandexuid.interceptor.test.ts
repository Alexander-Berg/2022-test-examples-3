import { APP_INTERCEPTOR } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import { AsyncStorageModule } from '@yandex-int/nest-common';
import request from 'supertest';

import { UatraitsModule, UatraitsService } from '../../uatraits';
import { YandexuidInterceptor } from '../yandexuid.interceptor';
import { YandexuidModule } from '../yandexuid.module';

import { YandexuidTestModule } from './module';

const uatraitsService = {
    getUatraits: jest.fn() as jest.Mock,
};

const getAppWithGlobalInterceptor = async () => {
    const moduleRef = await Test.createTestingModule({
        imports: [YandexuidTestModule, YandexuidModule, UatraitsModule.forRoot(), AsyncStorageModule.forRoot()],
        providers: [
            {
                provide: APP_INTERCEPTOR,
                useClass: YandexuidInterceptor,
            },
        ],
    })
        .overrideProvider(UatraitsService)
        .useValue(uatraitsService)
        .compile();

    const app = moduleRef.createNestApplication();
    await app.init();

    return app;
};

const noHeader = (header: string) => (res: Response) => {
    expect(res.headers[header.toLowerCase()]).toBeUndefined();
};

describe('YandexuidInterceptor', () => {
    test('working as global interceptor', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.ru')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.ru/);
    });

    test('working as controller interceptor', async () => {
        const moduleRef = await Test.createTestingModule({
            imports: [YandexuidTestModule, UatraitsModule.forRoot(), AsyncStorageModule.forRoot()],
        })
            .overrideProvider(UatraitsService)
            .useValue(uatraitsService)
            .compile();

        const app = moduleRef.createNestApplication();
        await app.init();

        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/controller')
            .set('Host', 'tests.yandex.ru')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.ru/);
    });

    test('working as method interceptor', async () => {
        const moduleRef = await Test.createTestingModule({
            imports: [YandexuidTestModule, UatraitsModule.forRoot(), AsyncStorageModule.forRoot()],
        })
            .overrideProvider(UatraitsService)
            .useValue(uatraitsService)
            .compile();

        const app = moduleRef.createNestApplication();
        await app.init();

        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/method')
            .set('Host', 'tests.yandex.ru')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.ru/);
    });

    test("shouldn't set cookie if it's already exists", async () => {
        const moduleRef = await Test.createTestingModule({
            imports: [YandexuidTestModule, UatraitsModule.forRoot(), AsyncStorageModule.forRoot()],
        })
            .overrideProvider(UatraitsService)
            .useValue(uatraitsService)
            .compile();

        const app = moduleRef.createNestApplication();
        await app.init();
        const validYandexuid = '3329078461605337812';

        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/method')
            .set('Host', 'tests.yandex.ru')
            .set('Cookie', `yandexuid=${validYandexuid}`)
            .expect(200)
            .expect(noHeader('Set-Cookie'));
    });

    test('should set yandexuid, if request was from yandex.by domain', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.by')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.by/);
    });

    test('should set yandexuid, if request was from yandex.kz domain', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.kz')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.kz/);
    });

    test('should set yandexuid, if request was from yandex.ua domain', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.ua')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.ua/);
    });

    test('should set yandexuid, if request was from ya.ru', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'p.ya.ru')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.ya\.ru/);
    });

    test('should set yandexuid, if request was from yandex.com', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'yandex.com')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.com/);
    });

    test('should set yandexuid, if request was from yandex-ad.cn', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'yandex-ad.cn')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex-ad\.cn/);
    });

    test('should set yandexuid, if request was from yandex-team.ru', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'yandex-team.ru')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex-team\.ru/);
    });

    test('should set yandexuid, if request was from yandex.eu', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'yandex.eu')
            .expect(200)
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.eu/);
    });

    test('should preserve cookie, if it was in cookies before', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.ua')
            .set('Cookie', 'yandexuid=123456789' + String(Date.now()).substr(0, 10))
            .expect(noHeader('Set-cookie'));
    });

    test('invalid cookies should be denied', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.ru')
            .set('Cookie', 'yandexuid=123456789')
            .expect('Set-Cookie', /yandexuid=/)
            .expect('Set-Cookie', /Domain=\.yandex\.ru/);
    });

    test("should do nothing if the request came from an user agent that isn't a browser", async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: false }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.ua')
            .expect(noHeader('Set-cookie'));
    });

    test('should do nothing if the request came from a robot', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true, isRobot: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.ua')
            .expect(noHeader('Set-cookie'));
    });

    test("should do nothing if the request came from a browser that doesn't support cookies", async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple?nocookiesupport=yes')
            .set('Host', 'tests.yandex.ua')
            .expect(noHeader('Set-cookie'));
    });

    test('should do nothing if the request came from localhost', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer()).get('/simple').set('Host', 'localhost').expect(noHeader('Set-cookie'));
    });

    test('should do nothing if the request came from non root domain', async () => {
        const app = await getAppWithGlobalInterceptor();
        uatraitsService.getUatraits.mockReturnValueOnce(Promise.resolve({ isBrowser: true }));

        return request(app.getHttpServer())
            .get('/simple')
            .set('Host', 'tests.yandex.net')
            .expect(noHeader('Set-cookie'));
    });
});
