const assert = require('assert');
const sinon = require('sinon');
const nock = require('nock');

const logger = require('lib/logger');
const auth = require('middlewares/auth');
const { nockTvmCheckTicket } = require('tests/mocks');

describe('Auth middleware', () => {
    afterEach(nock.cleanAll);

    it('should auth by tvm header x-ya-service-ticket', async() => {
        const nockInstance = nockTvmCheckTicket();

        const ctx = { logger, state: {}, header: { 'x-ya-service-ticket': '123' } };
        const next = sinon.spy();
        const authTvm = auth.tvm();

        await authTvm(ctx, next);

        assert.ok(next.calledOnce);
        assert.ok(nockInstance.isDone());
    });
});
