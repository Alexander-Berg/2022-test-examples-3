import { Test } from '@nestjs/testing';
import request from 'supertest';
import { AsyncStorageModule } from '@yandex-int/nest-common';

import { CspServiceTestModule } from './module/cspServiceTest.module';

describe('CspService', () => {
    test('should return nonce', async () => {
        const moduleRef = await Test.createTestingModule({
            imports: [CspServiceTestModule, AsyncStorageModule.forRoot()],
        }).compile();

        const app = moduleRef.createNestApplication();
        await app.init();

        return request(app.getHttpServer())
            .get('/service')
            .expect(200)
            .expect((res) => expect(res.body.nonce).not.toBeUndefined());
    });
});
