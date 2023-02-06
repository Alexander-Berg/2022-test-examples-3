const nock = require('nock');

const httpChecker = require('models/heartbeat/check/http');

describe('Http checker', () => {
    afterEach(nock.cleanAll);

    const options = {
        protocol: 'https:',
        host: 'my.custom.service.yandex-team.ru',
    };

    it('should request to endpoint with default path', async() => {
        const request = nock('https://my.custom.service.yandex-team.ru')
            .get('/ping')
            .reply(200);

        await httpChecker(options);

        request.done();
    });

    it('should request to endpoint with custom path', async() => {
        const request = nock('https://my.custom.service.yandex-team.ru')
            .get('/custom/ping')
            .reply(200);

        await httpChecker(options, '/custom/ping');

        request.done();
    });

    it('should throw error when request failed', async() => {
        const request = nock('https://my.custom.service.yandex-team.ru')
            .get('/ping')
            .reply(500);

        try {
            await httpChecker(options, '/ping');
        } catch (error) {
            request.done();

            return;
        }

        throw new Error('Request should throw error');
    });

    it('should throw error when request timed out', async() => {
        nock('https://my.custom.service.yandex-team.ru').get('/ping');

        try {
            await httpChecker(options, '/ping');
        } catch (error) {
            return;
        }

        throw new Error('Request should throw error');
    });
});
