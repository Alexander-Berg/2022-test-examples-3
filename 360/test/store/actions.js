const assert = require('assert');
const expect = require('expect.js');

const actions = require('store/actions');

describe('actions', function() {

    describe('login fail', function() {
        const loginFail = actions.loginFail;

        it('should detect empty login', function() {
            const payload = {password: ''};
            expect(loginFail(payload).payload.status).to.be(actions.LOGIN_EMPTY);
        });

        it('should detect empty password', function() {
            const payload = {login: 'yo'};
            expect(loginFail(payload).payload.status).to.be(actions.PASSWORD_EMPTY);
        });

        it('should proxy status', function() {
            const st = 'password-incorrect';
            const payload = {login: 'yo', password: 'yoyo', status: st};
            expect(loginFail(payload).payload.status).to.be(st);
        });

    });

});
