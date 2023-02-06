const request = require('supertest');
const app = require('../app');

const { mockedPage } = require('puppeteer');
const { BACKEND_SERVICE_TICKET } = require('../helpers/mock-tvm');

describe('/v2/conferences/:conferenceUrl/broadcast/:broadcastUrl/start', () => {
    it('Успешный запуск', async() => {
        const response = await request(app)
            // eslint-disable-next-line max-len
            .put('/v2/conferences/https%3A%2F%2Ftelemost.dst.yandex.ru%2Fj%2F08832703366128/broadcast/https%3A%2F%2Ftelemost.dst.yandex.ru%2Flive%2Fc6a692d7aa574baebd22d3b7b74a7641/start?stream_key=STREAM_KEY&translator_token=TRANSLATOR_TOKEN&resolution=1080')
            .set('X-Ya-Service-Ticket', BACKEND_SERVICE_TICKET);

        expect(response.status).toBe(200);
        expect(response.text).toBe('{"status":"started"}');
        expect(response.headers['content-type']).toBe('application/json; charset=utf-8');

        // eslint-disable-next-line max-len
        expect(mockedPage.goto).toBeCalledWith('https://telemost.dst.yandex.ru/j/08832703366128?translator_token=TRANSLATOR_TOKEN&mic=off&camera=off&auto_join=1&guest_name=Broadcaster');
    });
});
