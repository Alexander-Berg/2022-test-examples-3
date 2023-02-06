import { defaultBunkerData, requestAuth } from './utils';

describe('GET /v0/bunker/cat', function() {
    it('returns all data', function(done) {
        requestAuth
            .get('/v0/bunker/cat')
            .expect(200, defaultBunkerData, done);
    });

    it('returns child', function(done) {
        requestAuth
            .get('/v0/bunker/cat?node=child')
            .expect(200, defaultBunkerData.child, done);
    });

    it('returns no found', function(done) {
        requestAuth
            .get('/v0/bunker/cat?node=child2')
            .expect(404, { message: 'No data' }, done);
    });
});
