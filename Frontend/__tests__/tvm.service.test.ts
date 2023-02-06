import nock from 'nock';
import { Test } from '@nestjs/testing';
import request from 'supertest';

import { TvmOptions } from '../types';
import { TvmModule } from '../tvm.module';

import { TvmServiceTestModule } from './module/tvmServiceTest.module';
import { nockTvmServer } from './utils';

const serverUrl = 'http://localhost:1/';

const getApp = async (options: TvmOptions) => {
    const moduleRef = await Test.createTestingModule({
        imports: [TvmModule.forRoot(options), TvmServiceTestModule],
    }).compile();

    const app = moduleRef.createNestApplication();
    await app.init();

    return app;
};

describe('TvmService', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('it just works', async () => {
        const sessionId = String(Math.random());

        const tickets = {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224,
            },
            geobase: {
                ticket: 'geobase-ticket',
                tvm_id: 225,
            },
        };

        nockTvmServer(serverUrl, 200, tickets);

        const app = await getApp({
            serverUrl,
            clientId: 'test',
            destinations: ['blackbox', 'geobase'],
            token: 'tvmtool-development-access-token',
        });

        await request(app.getHttpServer())
            .get('/tvm')
            .set('X-Forwarded-For', '127.0.0.1')
            .set('Cookie', `Session_id=${sessionId}`)
            .set('Host', 'yandex.ru')
            .expect(200)
            .expect((res) => {
                expect(res.body).toMatchObject({
                    tvm: {
                        tickets,
                    },
                });
            });
    });

    test('get ticket should works', async () => {
        const sessionId = String(Math.random());

        const bbTicket = {
            ticket: 'blackbox-ticket',
            tvm_id: 224,
        };

        const tickets = {
            blackbox: bbTicket,
            geobase: {
                ticket: 'geobase-ticket',
                tvm_id: 225,
            },
        };

        nockTvmServer(serverUrl, 200, tickets);

        const app = await getApp({
            serverUrl,
            clientId: 'test',
            destinations: ['blackbox', 'geobase'],
            token: 'tvmtool-development-access-token',
        });

        await request(app.getHttpServer())
            .get('/tvm/getTicket')
            .set('X-Forwarded-For', '127.0.0.1')
            .set('Cookie', `Session_id=${sessionId}`)
            .set('Host', 'yandex.ru')
            .expect(200)
            .expect((res) => {
                expect(res.body).toMatchObject({
                    ticket: bbTicket,
                });
            });
    });

    test('should cache results according to the cacheMaxAge option', async () => {
        const tickets = {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224,
            },
            geobase: {
                ticket: 'geobase-ticket',
                tvm_id: 225,
            },
        };

        const spyFunction = jest.fn();

        nockTvmServer(serverUrl, 200, tickets, spyFunction);

        const app = await getApp({
            serverUrl,
            clientId: 'test',
            destinations: ['blackbox', 'geobase'],
            token: 'tvmtool-development-access-token',
            cacheMaxAge: 1000,
        });

        jest.useFakeTimers();

        const httpServer = app.getHttpServer();

        await request(httpServer).get('/tvm').expect(200);

        await request(httpServer).get('/tvm').expect(200);

        jest.advanceTimersByTime(1001);

        nockTvmServer(serverUrl, 200, tickets, spyFunction);

        await request(httpServer).get('/tvm').expect(200);

        expect(spyFunction).toHaveBeenCalledTimes(2);
    });
});
