import nock from 'nock';
import { Test } from '@nestjs/testing';
import supertest from 'supertest';
import { AsyncStorageModule } from '@yandex-int/nest-common';

import { TvmModule, TvmService } from '../../tvm';
import { BlackboxOptions } from '../types';
import { BlackboxModule } from '../blackbox.module';

import { BlackboxServiceTestModule } from './module/blackboxServiceTest.module';
import { TestConfigModule, TestConfigService } from '../../testUtils/configModule';
import { nockBlackbox, tvmServiceMock } from './utils';

const blackboxUrl = 'http://mockbox.yandex.ru';

describe('BlackboxModule', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('forRootAsync should work', async () => {
        const moduleRef = await Test.createTestingModule({
            imports: [
                TvmModule.forRoot({ clientId: 'test', destinations: [], serverUrl: 'url', token: 'asd' }),
                BlackboxModule.forRootAsync({
                    imports: [TestConfigModule.register({ api: blackboxUrl })],
                    inject: [TestConfigService],
                    useFactory: (configService: TestConfigService<BlackboxOptions>) => configService.config
                }),
                AsyncStorageModule.forRoot(),
                BlackboxServiceTestModule,
            ],
        })
            .overrideProvider(TvmService)
            .useValue(tvmServiceMock)
            .compile();

        const app = moduleRef.createNestApplication();
        await app.init();

        const sessionId = String(Math.random());

        const blackboxData = {
            error: 'OK',
            display_name: {
                name: 'Звездный Лорд',
            },
            attributes: {
                '1007': 'Блекбоксович Экспресс',
                '1008': 'express-blackbox',
            },
        };

        nockBlackbox(blackboxUrl, 200, blackboxData);

        await supertest(app.getHttpServer())
            .get('/blackbox')
            .set('X-Forwarded-For', '127.0.0.1')
            .set('Cookie', `Session_id=${sessionId}`)
            .set('Host', 'yandex.ru')
            .expect(200)
            .expect((res) => {
                expect(res.body).toMatchObject({
                    blackbox: {
                        error: 'OK',
                        fio: blackboxData.attributes['1007'],
                        login: blackboxData.attributes['1008'],
                        displayName: blackboxData.display_name.name,
                    },
                });
            });
    })
});
