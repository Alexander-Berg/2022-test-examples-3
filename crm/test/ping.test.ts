import { requestAnonymous } from './utils';

const pingRoutes = ['/ping', '/healthcheck'];

pingRoutes.forEach(route => {
    describe(`GET ${route}`, function() {
        it('responds with OK', function(done) {
            requestAnonymous
                .get(route)
                .expect(200, 'OK', done);
        });

        it('responds with Access-Control-Allow-Origin: *', function(done) {
            requestAnonymous
                .get(route)
                .expect('Access-Control-Allow-Origin', '*')
                .expect(200, done);
        });
    });
});
