const request = require('supertest');
const nock = require('nock');
const config = require('yandex-cfg');

const app = require('app');
const cleanDb = require('tests/db/clean');

describe('Heartbeat controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    it('should return `ok` when all checks succeeded', async() => {
        nock(`http://${config.blackbox.api}/`)
            .get('/ping')
            .reply(200);
        nock('http://localhost:1/')
            .get('/ping')
            .reply(200);

        await request(app.listen())
            .get('/v1/heartbeat')
            .expect(200)
            .expect({ level: 'ok' });
    });

    it('should return `crit` when crit check failed', async() => {
        nock(`http://${config.blackbox.api}/`)
            .get('/ping')
            .reply(200);
        nock('http://localhost:1/')
            .get('/ping')
            .reply(500);

        await request(app.listen())
            .get('/v1/heartbeat')
            .expect(500)
            .expect({ level: 'crit' });
    });
});
