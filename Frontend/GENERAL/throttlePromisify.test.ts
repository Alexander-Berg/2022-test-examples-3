import { SinonStub, stub, useFakeTimers } from 'sinon';
import { assert } from 'chai';

import { PromisifyFn, throttlePromisify } from './throttlePromisify';

const THROTTLE_WAIT = 200;

describe('throttlePromisify', () => {
    let clock: ReturnType<typeof useFakeTimers>;
    let originalFn: SinonStub<[], Promise<string>>;
    let wrappedFn: PromisifyFn<string>;

    beforeEach(() => {
        clock = useFakeTimers();
        originalFn = stub<[], Promise<string>>().returns(Promise.resolve('test'));
        wrappedFn = throttlePromisify(originalFn, THROTTLE_WAIT);
    });

    afterEach(() => {
        clock.restore();
    });

    it('should proxy first call', async() => {
        assert.equal(await wrappedFn(), 'test');
        assert.isTrue(originalFn.called);
    });

    it('should respond from cache if it is valid', async() => {
        await wrappedFn();

        assert.equal(await wrappedFn(), 'test');
        assert.isTrue(originalFn.calledOnce);
    });

    it('should proxy if cache is outdated', async() => {
        await wrappedFn();

        clock.tick(THROTTLE_WAIT * 2);

        assert.equal(await wrappedFn(), 'test');
        assert.isTrue(originalFn.calledTwice);
    });

    it('should proxy only once', async() => {
        const results = await Promise.all([wrappedFn(), wrappedFn()]);

        assert.deepEqual(results, ['test', 'test']);
        assert.isTrue(originalFn.calledOnce);
    });
});
