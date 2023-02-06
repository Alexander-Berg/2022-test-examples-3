import { Test } from '@nestjs/testing';
import supertest from 'supertest';

import { HttpExceptionFilterTestModule } from './module/httpExceptionFilterTest.module';

const getApp = async () => {
    const moduleRef = await Test.createTestingModule({
        imports: [HttpExceptionFilterTestModule],
    }).compile();

    const app = moduleRef.createNestApplication();
    await app.init();

    return app;
};

describe('HttpExceptionFilter', () => {
    test('catch throwed Error', async () => {
        const app = await getApp();

        await supertest(app.getHttpServer())
            .get('/throw')
            .expect(500)
            .expect((res) => res.body.statusCode === 500);
    });

    test('catch HttpException', async () => {
        const app = await getApp();

        await supertest(app.getHttpServer())
            .get('/forbidden')
            .expect(403)
            .expect((res) => res.body.statusCode === 403);
    });

    test('catch Observable error', async () => {
        const app = await getApp();

        await supertest(app.getHttpServer())
            .get('/observableError')
            .expect(500)
            .expect((res) => res.body.statusCode === 500);
    });

    test('catch Observable exception', async () => {
        const app = await getApp();

        await supertest(app.getHttpServer())
            .get('/observableException')
            .expect(403)
            .expect((res) => res.body.statusCode === 403);
    });
});
