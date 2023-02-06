const { expect } = require('chai');
const nock = require('nock');
const sinon = require('sinon');
const log = require('logger');
const nockTvm = require('tests/helpers/nockTvm');

const getWebUser = require('middleware/webUser');

describe('User state test', () => {

    beforeEach(function *() {
        yield require('tests/helpers/clear').clear();
        sinon.spy(log, 'warn');
    });

    afterEach(() => {
        log.warn.restore();
    });

    it('should request to blackbox', function *() {
        const userState = getWebUser({
            connection: { api: 'blackbox.test.host' },
            attributes: '27,28,1008'
        });

        nock('http://blackbox.test.host')
            .get('/blackbox')
            .query({
                method: 'sessionid',
                format: 'json',
                sessionid: 'my-session-id',
                sslsessionid: 'my-sessionid2',
                host: 'localhost',
                userip: 'my-userip',
                attributes: '27,28,1008',
                emails: 'getdefault'
            })
            .reply(200, { message: 'blackbox sessionId response' });

        const cookies = {
            'Session_id': 'my-session-id',
            sessionid2: 'my-sessionid2'
        };
        const context = {
            header: { userhost: 'localhost:8081', userip: 'my-userip' },
            cookies: { get: key => cookies[key] },
            state: {}
        };

        yield userState.call(context, {});

        const expected = { message: 'blackbox sessionId response' };

        expect(context.state.user).to.deep.equal(expected);
        expect(context.state.authType).to.deep.equal('web');
        expect(log.warn.called).to.be.false;
    });

    it('should request to blackbox with tvm ticket', function *() {
        const userState = getWebUser({
            connection: { api: 'blackbox.test.host' },
            attributes: '27,28,1008',
            tvmName: 'yandexTeamBlackbox'
        });

        nock('http://blackbox.test.host', {
            reqheaders: { 'x-ya-service-ticket': 'someTicket' }
        })
            .get('/blackbox')
            .query({
                method: 'sessionid',
                format: 'json',
                sessionid: 'my-session-id',
                sslsessionid: 'my-sessionid2',
                host: 'localhost',
                userip: 'my-userip',
                attributes: '27,28,1008',
                emails: 'getdefault'
            })
            .reply(200, { message: 'blackbox sessionId response' });

        nockTvm.getTicket({
            'blackbox-ya-team': { ticket: 'someTicket' }
        });

        const cookies = {
            'Session_id': 'my-session-id',
            sessionid2: 'my-sessionid2'
        };
        const context = {
            header: {
                userhost: 'localhost:8081',
                userip: 'my-userip',
                'force-tvm-check': 1
            },
            cookies: { get: key => cookies[key] },
            state: {}
        };

        yield userState.call(context, {});

        const expected = { message: 'blackbox sessionId response' };

        expect(context.state.user).to.deep.equal(expected);
        expect(context.state.authType).to.deep.equal('web');
        expect(log.warn.called).to.be.false;
    });

    it('should return invalid when cookie is empty', function *() {
        const context = {
            header: { userhost: 'localhost:8081', userip: 'my-userip' },
            cookies: { get: () => '' },
            state: {}
        };

        const userState = getWebUser({
            connection: { api: 'blackbox.test.host' },
            attributes: '27,28,1008'
        });

        yield userState.call(context, {});

        const expected = { status: { value: 'INVALID', id: 0 } };

        expect(context.state.user).to.deep.equal(expected);
        expect(log.warn.called).to.be.false;
    });

    it('should log error and call `next`', function *(done) {
        const userState = getWebUser({});

        yield userState(function *() {
            done();
            yield {};
        });

        expect(log.warn.calledOnce).to.be.true;
    });
});
