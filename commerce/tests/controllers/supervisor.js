const request = require('supertest');

const app = require('../../app');

describe('Supervisor controller', () => {
    describe('script', () => {
        const supervisorUrl = 'https://yastatic.net/s3/expert/static/supervisor/development/supervisor.js';
        const expectedHTML = `<html><head><link rel="shortcut icon" href="//yastatic.net/iconostasis/_/8lFaTHLDzmsEZz-5XaQg9iTWZGE.png"><script type="text/javascript" src="${supervisorUrl}"></script><script id="pro" data-pro-url="https://yandex-dev.proctoring.online" src="../js/script.js"></script></head></html>`;

        it('should render empty page with supervisor scripts', async () => {
            await request(app)
                .get('/supervisor')
                .expect(200, expectedHTML);
        });
    });

    describe('hasAccess', () => {
        it('should send 200 when origin has access', async () => {
            await request(app)
                .post('/access')
                .send({
                    origin: `https://localhost`
                })
                .expect(200);
        });

        it('should send 400 when origin has no access', async () => {
            await request(app)
                .post('/access')
                .send({
                    origin: 'https://evil-origin.com'
                })
                .expect(400);
        });
    });
});
