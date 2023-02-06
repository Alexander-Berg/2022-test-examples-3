const assert = require('assert');
const sinon = require('sinon');
const httpAssertValue = require('http-assert-value');

const logger = require('lib/logger');
const catchError = require('middlewares/catchError');

describe('Catch error middleware', () => {
    beforeEach(() => sinon.spy(logger, 'error'));
    afterEach(() => logger.error.restore());

    it('should do nothing when `next` succeeded', async() => {
        const ctx = {};
        const next = sinon.spy();

        await catchError(ctx, next);

        assert(next.calledOnce, 'Next should be called once');
        assert.deepStrictEqual(ctx, {});
    });

    it('should define status and body with default value', async() => {
        const ctx = { logger };
        const next = sinon.spy(() => {
            throw new Error('My error');
        });

        await catchError(ctx, next);

        assert(next.calledOnce, 'Next should be called once');
        assert.strictEqual(ctx.status, 500);
        assert.deepStrictEqual(ctx.body, { message: 'My error' });
    });

    it('should define status and body with custom value', async() => {
        const ctx = { logger };
        const next = sinon.spy(() => httpAssertValue.identity('inv@l!d'));

        await catchError(ctx, next);

        assert(next.calledOnce, 'Next should be called once');
        assert.strictEqual(ctx.status, 400);
        assert.deepStrictEqual(ctx.body, {
            message: 'Identity is invalid',
            internalCode: '400_III',
            value: 'inv@l!d',
        });
    });
});
