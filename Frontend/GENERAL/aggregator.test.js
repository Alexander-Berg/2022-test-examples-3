'use strict';

const sinon = require('sinon');
const { assert } = require('chai');
const { Aggregator } = require('./aggregator');

sinon.assert.expose(assert, { prefix: '' });

describe('Aggregator.call', () => {
    const options = { aggregateTimeout: 200 };
    let clock;
    let originalFn;
    let wrappedFn;

    beforeEach(() => {
        clock = sinon.useFakeTimers();
        originalFn = sinon.stub().returns(Promise.resolve('test'));
        wrappedFn = Aggregator.wrap(originalFn, options);
    });

    afterEach(() => {
        clock.restore();
    });

    it('should proxy first call', async () => {
        assert.equal(await wrappedFn(), 'test');
        assert.called(originalFn);
    });

    it('should respond from cache if it is valid', async () => {
        await wrappedFn();

        assert.equal(await wrappedFn(), 'test');
        assert.calledOnce(originalFn);
    });

    it('should proxy if cache is outdated', async () => {
        await wrappedFn();

        clock.tick(options.aggregateTimeout * 2);

        assert.equal(await wrappedFn(), 'test');
        assert.calledTwice(originalFn);
    });

    it('should proxy only once', async () => {
        const results = await Promise.all([wrappedFn(), wrappedFn()]);

        assert.deepEqual(results, ['test', 'test']);
        assert.calledOnce(originalFn);
    });
});
