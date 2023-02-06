import path from 'path';

import { INestApplication } from '@nestjs/common';
import { RenderModule } from 'nest-next';
import { Test, TestingModule } from '@nestjs/testing';
import next from 'next';
import request from 'supertest';

import { AppModule } from '../src/app.module';

const clientDir = path.join(__dirname, '..', '..', 'client');

describe('AppController (integration)', () => {
  let nestApp: INestApplication;

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    const nextApp = next({ dev: false, dir: clientDir });
    await nextApp.prepare();

    nestApp = moduleFixture.createNestApplication();

    const renderer = nestApp.get(RenderModule);
    renderer.register(nestApp, nextApp, { viewsDir: null });

    await nestApp.init();
  });

  it('/ (GET)', () => {
    return request(nestApp.getHttpServer())
      .get('/')
      .expect(200);
  });
});
