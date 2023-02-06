import request from 'supertest';
import { createRiverbank } from '../riverbank-web-server/riverbank';

describe('ping', () => {
    it('pong', done => {
        request(createRiverbank())
            .get('/ping')
            .expect(200, 'pong', done);
    });
});
