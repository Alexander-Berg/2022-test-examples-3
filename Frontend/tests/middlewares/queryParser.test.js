const assert = require('assert');
const sinon = require('sinon');

const logger = require('lib/logger');
const queryParser = require('middlewares/queryParser');

describe('Query parser middleware', () => {
    beforeEach(() => sinon.spy(logger, 'error'));
    afterEach(() => logger.error.restore());

    it('should convert "field[]": [a, b] to "field": [a, b]', async() => {
        const fieldValues = [1, 'test'];
        const ctx = { query: { 'field[]': fieldValues } };
        const next = sinon.spy();

        await queryParser(ctx, next);

        assert.deepEqual(ctx.query.field, fieldValues);
    });
});
