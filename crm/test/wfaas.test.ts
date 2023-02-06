import { requestAnonymous } from './utils';
import { wfaas } from '../src/routes/wfaas';

describe('wfaas feature', () => {
    describe('POST /wfaas/cors/.get_bundle', function() {
        it('responds with OK', function(done) {
            requestAnonymous
                .post('/wfaas/cors/.get_bundle')
                .expect(200, wfaas, done);
        });
    });

    describe('OPTION /wfaas/cors/.get_bundle', function() {
        it('responds with OK', function(done) {
            requestAnonymous
                .options('/wfaas/cors/.get_bundle')
                .expect('Access-Control-Allow-Methods', 'GET,HEAD,PUT,PATCH,POST,DELETE')
                .expect(204, done);
        });
    });
});
