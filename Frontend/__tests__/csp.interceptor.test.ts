import { APP_INTERCEPTOR } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { AsyncStorageModule } from '@yandex-int/nest-common';

import { CspInterceptor } from '../csp.interceptor';
import { CspModule } from '../csp.module';
import { CspOptions } from '../types';
import { CSP } from '../constants';

import { CspInterceptorTestModule } from './module/cspInterceptorTest.module';

const getAppWithGlobalInterceptor = async (options?: CspOptions) => {
    const moduleRef = await Test.createTestingModule({
        imports: [CspInterceptorTestModule, CspModule.forRoot(options), AsyncStorageModule.forRoot()],
        providers: [
            {
                provide: APP_INTERCEPTOR,
                useClass: CspInterceptor,
            },
        ],
    }).compile();

    const app = moduleRef.createNestApplication();
    await app.init();

    return app;
};

const cspWithoutNonce = (expectedScpString: string) => (res: Response) => {
    expect(res.headers['content-security-policy'].replace(/\s'nonce-.+?'/g, '')).toBe(expectedScpString);
};

describe('CspInterceptor', () => {
    describe('csp functions', () => {
        test('Correct CSP header', async () => {
            const app = await getAppWithGlobalInterceptor({
                policies: {
                    'default-src': [CSP.SELF],
                    'script-src': [CSP.SELF, CSP.INLINE, 'somehost.com'],
                    'style-src': [CSP.SELF, 'mystyles.net'],
                    'img-src': ['data:', 'images.com'],
                    'worker-src': [CSP.NONE],
                    'block-all-mixed-content': true,
                },
            });

            const expected =
                "default-src 'self'; script-src 'self' 'unsafe-inline' somehost.com; style-src 'self' mystyles.net; img-src data: images.com; worker-src 'none'; block-all-mixed-content;";

            return request(app.getHttpServer()).get('/simple').expect(200).expect('Content-Security-Policy', expected);
        });

        test('default policies', async () => {
            const app = await getAppWithGlobalInterceptor();

            const expected =
                "default-src 'none'; script-src 'self' 'unsafe-eval' 'unsafe-inline' yastatic.net mc.yandex.ru; style-src 'self' 'unsafe-inline' yastatic.net mc.yandex.ru; font-src 'self' yastatic.net; img-src 'self' data: avatars.yandex.net mc.yandex.ru yastatic.net; frame-src 'self'; child-src 'self'; connect-src 'self' mc.yandex.ru;";

            return request(app.getHttpServer()).get('/simple').expect(200).expect(cspWithoutNonce(expected));
        });

        test('TLD replacement', async () => {
            const app = await getAppWithGlobalInterceptor({ policies: { 'script-src': ['*.yandex.%tld%'] } });

            await request(app.getHttpServer())
                .get('/simple')
                .set('Host', 'yandex.ua')
                .expect(200)
                .expect('Content-Security-Policy', 'script-src *.yandex.ua;');

            await request(app.getHttpServer())
                .get('/simple')
                .set('Host', 'yandex.com.am')
                .expect(200)
                .expect('Content-Security-Policy', 'script-src *.yandex.com.am;');
        });

        test('default report-uri', async () => {
            const app = await getAppWithGlobalInterceptor({
                policies: {
                    'script-src': [CSP.SELF],
                },
                useDefaultReportUri: true,
                serviceName: 'express-yandex-csp',
            });

            const expected =
                "script-src 'self'; report-uri https://csp.yandex.net/csp?from=express-yandex-csp&project=express-yandex-csp&yandex_login=yauser&yandexuid=1234567890;";

            await request(app.getHttpServer())
                .get('/simple')
                .set('Cookie', 'yandex_login=yauser;yandexuid=1234567890')
                .expect(200)
                .expect('Content-Security-Policy', expected);
        });

        test('should encode forbiden symbols', async () => {
            const app = await getAppWithGlobalInterceptor({
                policies: {
                    'script-src': ['myhost.com'],
                },
                useDefaultReportUri: true,
                serviceName: 'express-yandex-csp',
            });

            const expected =
                'script-src myhost.com; report-uri https://csp.yandex.net/csp?from=express-yandex-csp&project=express-yandex-csp&yandex_login=%1F&yandexuid=%C4%80;';

            await request(app.getHttpServer())
                .get('/simple')
                .set(
                    'Cookie',
                    `yandex_login=${encodeURI(String.fromCharCode(31))};yandexuid=${encodeURI(
                        String.fromCharCode(256),
                    )}`,
                )
                .expect(200)
                .expect('Content-Security-Policy', expected);
        });

        test('should set custom project', async () => {
            const app = await getAppWithGlobalInterceptor({
                policies: {
                    'script-src': ['myhost.com'],
                },
                useDefaultReportUri: true,
                serviceName: 'express-yandex-csp',
                project: 'custom-project',
            });

            const expected =
                'script-src myhost.com; report-uri https://csp.yandex.net/csp?from=express-yandex-csp&project=custom-project&yandex_login=yauser&yandexuid=1234567890;';

            await request(app.getHttpServer())
                .get('/simple')
                .set('Cookie', 'yandex_login=yauser;yandexuid=1234567890')
                .expect(200)
                .expect('Content-Security-Policy', expected);
        });
    });

    describe('interceptor functions', () => {
        test('should work as controller interceptor', async () => {
            const moduleRef = await Test.createTestingModule({
                imports: [CspInterceptorTestModule, AsyncStorageModule.forRoot()],
            }).compile();

            const app = moduleRef.createNestApplication();
            await app.init();

            const expected =
                "default-src 'none'; script-src 'self' 'unsafe-eval' 'unsafe-inline' yastatic.net mc.yandex.ru; style-src 'self' 'unsafe-inline' yastatic.net mc.yandex.ru; font-src 'self' yastatic.net; img-src 'self' data: avatars.yandex.net mc.yandex.ru yastatic.net; frame-src 'self'; child-src 'self'; connect-src 'self' mc.yandex.ru;";

            return request(app.getHttpServer()).get('/controller').expect(200).expect(cspWithoutNonce(expected));
        });

        test('should work as method interceptor', async () => {
            const moduleRef = await Test.createTestingModule({
                imports: [CspInterceptorTestModule, AsyncStorageModule.forRoot()],
            }).compile();

            const app = moduleRef.createNestApplication();
            await app.init();

            const expected =
                "default-src 'none'; script-src 'self' 'unsafe-eval' 'unsafe-inline' yastatic.net mc.yandex.ru; style-src 'self' 'unsafe-inline' yastatic.net mc.yandex.ru; font-src 'self' yastatic.net; img-src 'self' data: avatars.yandex.net mc.yandex.ru yastatic.net; frame-src 'self'; child-src 'self'; connect-src 'self' mc.yandex.ru;";

            return request(app.getHttpServer()).get('/method').expect(200).expect(cspWithoutNonce(expected));
        });
    });
});
