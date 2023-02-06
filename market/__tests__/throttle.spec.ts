import {InvariantViolation} from '@yandex-market/invariant';

import {throttle} from '..';

const assert = {
    notStrictEqual: (a, b) => expect(a).not.toStrictEqual(b),
    strictEqual: (a, b) => expect(a).toStrictEqual(b),
    deepEqual: (a, b) => expect(a).toEqual(b),
    notToBe: (a, b) => expect(a).not.toBe(b),
    notEqual: (a, b) => expect(a).not.toEqual(b),
    ok: a => expect(Boolean(a)).toEqual(true),
};

const identity = x => x;

describe('throttle', () => {
    it('should throttle a function', () => new Promise<void>(done => {
        let callCount = 0;
        const throttled = throttle<void, void>({wait: 32}, () => { callCount++; });

        throttled();
        throttled();
        throttled();

        const lastCount = callCount;
        assert.ok(callCount);

        setTimeout(() => {
            assert.ok(callCount > lastCount);
            done();
        }, 64);
    }));

    it('subsequent calls should return the result of the first call', () => new Promise<void>(done => {
        const throttled = throttle({wait: 32}, identity);
        const results = [throttled('a'), throttled('b')];

        assert.deepEqual(results, ['a', 'a']);

        setTimeout(() => {
            const resultsAfter = [throttled('c'), throttled('d')];
            assert.notEqual(resultsAfter[0], 'a');
            assert.notStrictEqual(resultsAfter[0], undefined);

            assert.notEqual(resultsAfter[1], 'd');
            assert.notStrictEqual(resultsAfter[1], undefined);
            done();
        }, 64);
    }));

    it('should clear timeout when `func` is called', () => new Promise<void>(done => {
        let callCount = 0;
        const throttled = throttle<void, void>({wait: 32}, () => { callCount++; });

        throttled();
        throttled();

        setTimeout(() => {
            assert.strictEqual(callCount, 2);
            done();
        }, 64);
    }));

    it('should not trigger a trailing call when invoked once', () => new Promise<void>(done => {
        let callCount = 0;
        const throttled = throttle<void, void>({wait: 32}, () => { callCount++; });

        throttled();
        assert.strictEqual(callCount, 1);

        setTimeout(() => {
            assert.strictEqual(callCount, 1);
            done();
        }, 64);
    }));

    [0, 1].forEach(index => {
        it(`should trigger a call when invoked repeatedly ${(index ? ' and "leading" is "false"' : '')}`, () => new Promise<void>(done => {
            let callCount = 0;
            const limit = 320;
            const throttled = throttle<void, void>({wait: 32, leading: !index}, () => { callCount++; });

            const start = +new Date();
            while ((new Date() as any - start) < limit) {
                throttled();
            }

            const actual = callCount > 1;

            setTimeout(() => {
                assert.ok(actual);
                done();
            }, 1);
        }));
    });

    it('should trigger a second throttled call as soon as possible', () => new Promise<void>(done => {
        let callCount = 0;

        const throttled = throttle<void, void>({wait: 128, leading: false}, () => { callCount++; });

        throttled();

        setTimeout(() => {
            assert.strictEqual(callCount, 1);
            throttled();
        }, 192);

        setTimeout(() => {
            assert.strictEqual(callCount, 1);
        }, 254);

        setTimeout(() => {
            assert.strictEqual(callCount, 2);
            done();
        }, 384);
    }));

    it('should apply default options', () => new Promise<void>(done => {
        let callCount = 0;
        const throttled = throttle<void, void>({wait: 32}, () => { callCount++; });

        throttled();
        throttled();
        assert.strictEqual(callCount, 1);

        setTimeout(() => {
            assert.strictEqual(callCount, 2);
            done();
        }, 128);
    }));

    it('should support a `leading` option', () => {
        const withLeading = throttle({wait: 32, leading: true}, identity);
        assert.strictEqual(withLeading('a'), 'a');

        const withoutLeading = throttle({wait: 32, leading: false}, identity);
        assert.strictEqual(withoutLeading('a'), undefined);
    });

    it('should support a `trailing` option', () => new Promise<void>(done => {
        let withCount = 0;
        let withoutCount = 0;

        const withTrailing = throttle({wait: 64, trailing: true}, value => {
            withCount++;
            return value;
        });

        const withoutTrailing = throttle({wait: 64, trailing: false}, value => {
            withoutCount++;
            return value;
        });

        assert.strictEqual(withTrailing('a'), 'a');
        assert.strictEqual(withTrailing('b'), 'a');

        assert.strictEqual(withoutTrailing('a'), 'a');
        assert.strictEqual(withoutTrailing('b'), 'a');

        setTimeout(() => {
            assert.strictEqual(withCount, 2);
            assert.strictEqual(withoutCount, 1);
            done();
        }, 256);
    }));

    it('should not update `lastCalled`, at the end of the timeout, when `trailing` is `false`', () => new Promise<void>(done => {
        let callCount = 0;
        const throttled = throttle<void, void>({wait: 64, trailing: false}, () => { callCount++; });

        throttled();
        throttled();

        setTimeout(() => {
            throttled();
            throttled();
        }, 96);

        setTimeout(() => {
            assert.ok(callCount > 1);
            done();
        }, 192);
    }));

    it('should has options as a first argument', () => {
        /* eslint-disable no-unused-vars */
        const throttledWithAllOptions = throttle({
            wait: 64,
            trailing: false,
            leading: true,
            maxWait: 500,
        }, identity);

        // @ts-expect-error
        const throttledWithoutOptions = throttle(123, identity);
        /* eslint-enable no-unused-vars */
    });

    it('should has function as a second argument and preserve original function signature', () => {
        /* eslint-disable no-unused-vars */
        /* eslint-disable no-unused-expressions */
        const throttleWithFunction = throttle({wait: 64}, identity);

        (throttleWithFunction as typeof identity);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (throttleWithFunction as () => {});

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        const throttledWithoutOptions = () => throttle({wait: 32}, null);
        expect(throttledWithoutOptions).toThrow(InvariantViolation);

        // up to 3 arguments function
        throttle({wait: 32}, (a: number) => ([a]));
        throttle({wait: 32}, (a: number, b: number) => ([a, b]));
        throttle({wait: 32}, (a: number, b: number, c: number) => ([a, b, c]));

        // @ts-expect-error
        throttle({wait: 32}, (a: number, b: number, c: number, d: number) => ([a, b, c, d]));

        /* eslint-enable no-unused-vars */
        /* eslint-enable no-unused-expressions */
    });
});
