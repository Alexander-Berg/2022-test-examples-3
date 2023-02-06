import 'mocha';
import fs from 'fs';

import request from 'supertest';

import app from 'server/app';

describe('Index', () => {
    describe('ping', () => {
        it('should respond with `pong`', async () => {
            await request(app)
                .get('/ping')
                .expect(200, 'pong');
        });
    });

    describe('no content', () => {
        it('should respond with 204', async () => {
            await request(app)
                .get('/')
                .expect(204);
        });
    });

    describe('adroll', () => {
        const adrollPage = fs.readFileSync('./static/adroll/adroll.hbs');

        it('should return adroll page', async () => {
            await request(app)
                .get('/adroll')
                .expect(200, page => page.replace(/nonce=(\S+) /, '') === adrollPage);
        });
    });

    describe('comdi', () => {
        const comdiUrl = 'https://yastatic.net/s3/vda/static/comdi/comdi.js';
        const event = 'test-event';
        const comdiPage = fs.readFileSync('./static/comdi.hbs')
            .toString()
            .replace(/{{url}}/, comdiUrl)
            .replace(/{{event}}/, event);

        it('should render empty page with comdi script', async () => {
            await request(app)
                .get('/comdi')
                .query({ event })
                .expect(200, comdiPage);
        });

        it('should throw 400 when event is not specified', async () => {
            await request(app)
                .get('/comdi')
                .expect(400);
        });
    });
});
