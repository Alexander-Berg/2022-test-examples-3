import { requestAuth, requestAnonymous, requestNeedReset } from './utils';

describe('GET /whoami', function() {
    describe('auth user', () => {
        it('returns login', function(done) {
            requestAuth
                .get('/whoami')
                .expect(200, { login: 'login' }, done);
        });
    });

    describe('need reset cookie', () => {
        it('returns header for reset trigger', function(done) {
            requestNeedReset
                .get('/whoami')
                .expect('x-blackbox-need-reset', 'true')
                .expect(200, { login: 'login' }, done);
        });
    });

    describe('anonymous user', () => {
        describe('when get /whoami', () => {
            it('returns 302', function(done) {
                requestAnonymous
                    .get('/whoami')
                    .expect('Location', /passport\.yandex-team\.ru/)
                    .expect(302, done);
            });
        });

        describe('when get /whoami by ajax', () => {
            it('returns 401', function(done) {
                requestAnonymous
                    .get('/whoami')
                    .set('X-Requested-With', 'XMLHttpRequest')
                    .expect(401, { redirectTo: 'https://passport.yandex-team.ru/auth?retpath=http://crm.app' }, done);
            });
        });
    });
});
