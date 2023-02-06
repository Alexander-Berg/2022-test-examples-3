const assert = require('assert');
const sinon = require('sinon');

const middlewareIdmResponse = require('.')({ logger: console });

describe('IDM response middleware', () => {
    beforeEach(() => {
        sinon.stub(console, 'error');
    });
    afterEach(() => {
        console.error.restore();
    });

    it('should add zero code in success case', async() => {
        const ctx = { body: {} };
        const expected = { data: 'some data', code: 0 };

        await middlewareIdmResponse(ctx, () => Promise.resolve({ data: 'some data' }));

        assert.deepStrictEqual(ctx.body, expected);
    });

    it('should return error with level `fatal`', async() => {
        const ctx = {};
        const error = { statusCode: 404, message: 'some error message' };
        const expected = { code: 404, fatal: 'some error message' };

        await middlewareIdmResponse(ctx, () => Promise.reject(error));

        // eslint-disable-next-line max-len
        sinon.assert.calledWithExactly(console.error, { statusCode: 404, message: 'some error message' });
        assert.deepStrictEqual(ctx.body, expected);
    });

    it('should return error with level `error`', async() => {
        const ctx = {};
        const error = { message: 'internal error' };
        const expected = { code: 1, error: 'internal error' };

        await middlewareIdmResponse(ctx, () => Promise.reject(error));

        sinon.assert.calledWithExactly(console.error, { message: 'internal error' });
        assert.deepStrictEqual(ctx.body, expected);
    });
});
