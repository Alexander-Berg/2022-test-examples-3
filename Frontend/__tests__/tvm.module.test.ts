import nock from 'nock';
import { Test } from '@nestjs/testing';
import request from 'supertest';

import { TestConfigModule, TestConfigService } from '../../testUtils/configModule';
import { TvmOptions } from '../types';
import { TvmModule } from '../tvm.module';

import { TvmServiceTestModule } from './module/tvmServiceTest.module';
import { nockTvmServer } from './utils';

const serverUrl = 'http://localhost:1/';

describe('TvmModule', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('forRootAsync should work', async () => {
        const moduleRef = await Test.createTestingModule({
            imports: [
                TvmModule.forRootAsync({
                    useFactory: (configService: TestConfigService<TvmOptions>) => {
                        return configService.config;
                    },
                    inject: [TestConfigService],
                    imports: [
                        TestConfigModule.register({
                            serverUrl,
                            clientId: 'test',
                            destinations: ['blackbox', 'geobase'],
                            token: 'tvmtool-development-access-token',
                        }),
                    ],
                }),
                TvmServiceTestModule,
            ],
        }).compile();

        const app = moduleRef.createNestApplication();
        await app.init();

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
    })
});
