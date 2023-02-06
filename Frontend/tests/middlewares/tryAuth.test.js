const assert = require('assert');
const sinon = require('sinon');
const nock = require('nock');
const config = require('yandex-cfg');

const { nockTvmtool } = require('tests/mocks');

const logger = require('lib/logger');
const tryAuth = require('middlewares/tryAuth');

describe('Try auth middleware', () => {
    afterEach(nock.cleanAll);

    it('should request to blackbox in success case', async() => {
        nock(`http://${config.blackbox.api}/`)
            .get('/blackbox')
            .query(data => data.method === 'sessionid')
            .reply(200, { login: 'yuu-mao', status: { id: 0 } });
        nockTvmtool();

        const ctx = {
            cookies: { get: () => 'data' },
            header: { host: 'GEN_QLOUD_HOSTNAME' },
            state: {},
        };
        const next = sinon.spy();

        await tryAuth(ctx, next);

        assert.equal(ctx.state.user.login, 'yuu-mao');
        assert(next.calledOnce);
    });

    it('should return default user in fail case', async() => {
        const ctx = { logger, state: {} };
        const next = sinon.spy();

        await tryAuth(ctx, next);

        assert.strictEqual(ctx.state.user.login, undefined);
        assert(next.calledOnce);
    });
});
