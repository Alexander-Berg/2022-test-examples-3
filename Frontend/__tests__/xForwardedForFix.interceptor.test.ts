/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';
import { Test } from '@nestjs/testing';
import request from 'supertest';

import { XForwardedForFixInterceptor } from '../xForwardedForFix.interceptor';
import { XForwardedForFixOptions } from '../types';

import { XForwardedForFixTestModule } from './module';
import { filteredIps } from './filteredIps';

const executionContext = {
    switchToHttp: jest.fn().mockReturnThis(),
    getRequest: jest.fn().mockReturnThis(),
};

const callHandler = {
    handle: () => of(1),
};

const runInterceptor = async (
    headers: Record<string, string | number | boolean | undefined>,
    options?: XForwardedForFixOptions,
) => {
    const interceptor = new XForwardedForFixInterceptor(options);

    (executionContext.switchToHttp().getRequest as jest.Mock<any, any>).mockReturnValueOnce({
        headers,
    });

    await interceptor.intercept(executionContext as any, callHandler);
};

describe('XForwardedForFixInterceptor', () => {
    test('exists', () => {
        const interceptor = new XForwardedForFixInterceptor();

        expect(interceptor).not.toBeFalsy();
    });

    test('should filter ips', async () => {
        for (const ip of filteredIps) {
            const headers = {
                'x-forwarded-for': ip,
            };

            await runInterceptor(headers);

            expect(headers['x-forwarded-for']).toBe(undefined);
        }
    });

    test('should filter joined ips', async () => {
        const headers = {
            'x-forwarded-for': filteredIps.join(','),
        };

        await runInterceptor(headers);
        expect(headers['x-forwarded-for']).toBe(undefined);
    });

    test('should set defaultIP, if all filtered', async () => {
        const headers = {
            'x-forwarded-for': filteredIps.join(','),
        };

        await runInterceptor(headers, { defaultIP: '127.0.0.1' });
        expect(headers['x-forwarded-for']).toBe('127.0.0.1');
    });

    test('should not set defaultIP, if not all filtered', async () => {
        const headers = {
            'x-forwarded-for': `${filteredIps.join(',')},87.250.248.136`,
        };

        await runInterceptor(headers, { defaultIP: '127.0.0.1' });
        expect(headers['x-forwarded-for']).toBe('87.250.248.136');
    });

    test('should work without x-forwarded-for header', async () => {
        await runInterceptor({});
    });

    test('should filter reserved IPv4 entries wrapped in IPv6', async () => {
        const headers = {
            'x-forwarded-for': '::ffff:192.168.0.1, 77.88.21.11',
        };

        await runInterceptor(headers, { defaultIP: '127.0.0.1' });
        expect(headers['x-forwarded-for']).toBe('77.88.21.11');
    });

    test('should replace x-forwarded-for with x-forwarded-for-y', async () => {
        const headers = {
            'x-forwarded-for': '::ffff:192.168.0.1, 77.88.21.11',
            'x-forwarded-for-y': '79.172.59.35',
        };

        await runInterceptor(headers);
        expect(headers['x-forwarded-for']).toBe('79.172.59.35');
    });

    test('should allow IP from allowedNetMasks', async () => {
        const headers = {
            'x-forwarded-for': filteredIps.join(',') + ',198.18.11.22',
        };

        await runInterceptor(headers, { allowedNetMasks: ['198.18.0.0/15'] });
        expect(headers['x-forwarded-for']).toBe('198.18.11.22');
    });

    test('should throw an error for wrong allowedNetMasks value', async () => {
        const headers = {
            'x-forwarded-for': filteredIps.join(','),
        };

        const thrower = async () => {
            await runInterceptor(headers, { allowedNetMasks: ['not a mask'] });
        };

        expect(thrower).rejects.toThrow();
    });

    describe('As interceptor', () => {
        test('working as global interceptor', async () => {
            const moduleRef = await Test.createTestingModule({
                imports: [XForwardedForFixTestModule],
            }).compile();

            const app = moduleRef.createNestApplication();
            app.useGlobalInterceptors(new XForwardedForFixInterceptor());
            await app.init();

            return request(app.getHttpServer())
                .get('/simple/header')
                .set('x-forwarded-for', filteredIps.join() + ',87.250.248.136')
                .expect(200)
                .expect({
                    header: '87.250.248.136',
                });
        });

        test('working as controller interceptor', async () => {
            const moduleRef = await Test.createTestingModule({
                imports: [XForwardedForFixTestModule],
            }).compile();

            const app = moduleRef.createNestApplication();
            await app.init();

            return request(app.getHttpServer())
                .get('/controller/header')
                .set('x-forwarded-for', filteredIps.join() + ',87.250.248.136')
                .expect(200)
                .expect({
                    header: '87.250.248.136',
                });
        });

        test('working as method interceptor', async () => {
            const moduleRef = await Test.createTestingModule({
                imports: [XForwardedForFixTestModule],
            }).compile();

            const app = moduleRef.createNestApplication();
            await app.init();

            return request(app.getHttpServer())
                .get('/method/header')
                .set('x-forwarded-for', filteredIps.join() + ',87.250.248.136')
                .expect(200)
                .expect({
                    header: '87.250.248.136',
                });
        });

        test('working as method interceptor instantiated by nest', async () => {
            const moduleRef = await Test.createTestingModule({
                imports: [XForwardedForFixTestModule],
            }).compile();

            const app = moduleRef.createNestApplication();
            await app.init();

            return request(app.getHttpServer())
                .get('/method/header2')
                .set('x-forwarded-for', filteredIps.join() + ',87.250.248.136')
                .expect(200)
                .expect({
                    header: '87.250.248.136',
                });
        });
    });
});
