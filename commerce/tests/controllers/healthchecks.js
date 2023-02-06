const request = require('supertest');

const app = require('../../app');

describe('Healthchecks controller', () => {
    describe('Ping check', () => {
        it('should return OK', async () => {
            await request(app)
                .get('/healthchecks/ping')
                .expect(200, 'OK');
        });
    });
});
