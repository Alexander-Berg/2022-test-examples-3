import { requestAuth } from './utils';

describe('GET /cleanup', function() {
    it('responds with OK', function(done) {
        requestAuth
            .get('/cleanup')
            .expect('Content-Type', 'text/html; charset=utf-8')
            .expect(200, done);
    });
});
