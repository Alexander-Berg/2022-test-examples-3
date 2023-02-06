import {InvariantViolation} from '@yandex-market/invariant';

import {debounce} from '..';

const assert = {
    notStrictEqual: (a, b) => expect(a).not.toStrictEqual(b),
    strictEqual: (a, b) => expect(a).toStrictEqual(b),
    deepEqual: (a, b) => expect(a).toEqual(b),
    notToBe: (a, b) => expect(a).not.toBe(b),
    notEqual: (a, b) => expect(a).not.toEqual(b),
};

const identity = x => x;

describe('функция debounce', () => {
    it('should debounce a function', () => new Promise<void>(done => {
        let callCount = 0;

        const debounced = debounce({wait: 32}, value => {
            ++callCount;
            return value;
        });

        const results = [debounced('a'), debounced('b'), debounced('c')];
        assert.deepEqual(results, [undefined, undefined, undefined]);
        assert.strictEqual(callCount, 0);

        setTimeout(() => {
            assert.strictEqual(callCount, 1);

            const _results = [debounced('d'), debounced('e'), debounced('f')];
            assert.deepEqual(_results, ['c', 'c', 'c']);
            assert.strictEqual(callCount, 1);
        }, 128);

        setTimeout(() => {
            assert.strictEqual(callCount, 2);
            done();
        }, 256);
    }));

    it('subsequent debounced calls return the last `func` result', () => new Promise<void>(done => {
        const debounced = debounce({wait: 32}, x => x);
        debounced('a');

        setTimeout(() => {
            assert.notEqual(debounced('b'), 'b');
        }, 64);

        setTimeout(() => {
            assert.notEqual(debounced('c'), 'c');
            done();
        }, 128);
    }));

    it('should not immediately call `func` when `wait` is `0`', () => new Promise<void>(done => {
        let callCount = 0;
        const debounced = debounce<void, any>({wait: 0}, () => { ++callCount; });

        debounced();
        debounced();
        assert.strictEqual(callCount, 0);

        setTimeout(() => {
            assert.strictEqual(callCount, 1);
            done();
        }, 5);
    }));

    it('should apply default options', () => new Promise<void>(done => {
        let callCount = 0;
        const debounced = debounce<void, any>({wait: 32}, () => { callCount++; });

        debounced();
        assert.strictEqual(callCount, 0);

        setTimeout(() => {
            assert.strictEqual(callCount, 1);
            done();
        }, 64);
    }));

    it('should invoke the trailing call with the correct arguments and `this` binding', () => new Promise<void>(done => {
        let actual;
        let callCount = 0;
        const object = {};

        const debounced = debounce({wait: 32, leading: true, maxWait: 64}, function (...args) {
            actual = [this];
            Array.prototype.push.apply(actual, args);
            ++callCount;
            return callCount !== 2;
        });

        debounced.call(object, 'a');
        debounced.call(object, 'a');

        setTimeout(() => {
            assert.strictEqual(callCount, 2);
            assert.deepEqual(actual, [object, 'a']);
            done();
        }, 64);
    }));

    it('should support a `leading` option', () => new Promise<void>(done => {
        const callCounts = [0, 0];

        const withLeading = debounce<void, any>({leading: true, wait: 32}, () => callCounts[0]++);
        const withLeadingAndTrailing = debounce<void, any>({leading: true, wait: 32}, () => callCounts[1]++);

        withLeading();
        assert.strictEqual(callCounts[0], 1);

        withLeadingAndTrailing();
        withLeadingAndTrailing();
        assert.strictEqual(callCounts[1], 1);

        setTimeout(() => {
            assert.deepEqual(callCounts, [1, 2]);

            withLeading();
            assert.strictEqual(callCounts[0], 2);

            done();
        }, 64);
    }));

    it('subsequent leading debounced calls return the last `func` result', () => new Promise<void>(done => {
        const debounced = debounce({wait: 32, leading: true, trailing: false}, identity);
        const results = [debounced('a'), debounced('b')];

        assert.deepEqual(results, ['a', 'a']);

        setTimeout(() => {
            const resultsAfter = [debounced('c'), debounced('d')];
            assert.deepEqual(resultsAfter, ['c', 'c']);
            done();
        }, 64);
    }));

    it('should support a `trailing` option', () => new Promise<void>(done => {
        let withCount = 0;
        let withoutCount = 0;

        const withTrailing = debounce<void, any>({wait: 32, trailing: true}, () => withCount++);
        const withoutTrailing = debounce<void, any>({wait: 32, trailing: false}, () => withoutCount++);

        withTrailing();
        assert.strictEqual(withCount, 0);

        withoutTrailing();
        assert.strictEqual(withoutCount, 0);

        setTimeout(() => {
            assert.strictEqual(withCount, 1);
            assert.strictEqual(withoutCount, 0);
            done();
        }, 64);
    }));

    it('should support a `maxWait` option', () => new Promise<void>(done => {
        let callCount = 0;

        const debounced = debounce<void, any>({maxWait: 64, wait: 32}, value => {
            ++callCount;
            return value;
        });

        debounced();
        debounced();
        assert.strictEqual(callCount, 0);

        setTimeout(() => {
            assert.strictEqual(callCount, 1);
            debounced();
            debounced();
            assert.strictEqual(callCount, 1);
        }, 128);

        setTimeout(() => {
            assert.strictEqual(callCount, 2);
            done();
        }, 256);
    }));

    it('should support `maxWait` in a tight loop', () => new Promise<void>(done => {
        const limit = 200;
        let withCount = 0;
        let withoutCount = 0;

        const withMaxWait = debounce<void, any>({wait: 64, maxWait: 128}, () => withCount++);

        const withoutMaxWait = debounce<void, any>({wait: 64}, () => withoutCount++);

        const start = +new Date();
        while ((+new Date() - start) < limit) {
            withMaxWait();
            withoutMaxWait();
        }
        const actual = [Boolean(withoutCount), Boolean(withCount)];
        setTimeout(() => {
            assert.deepEqual(actual, [false, true]);
            done();
        }, 1);
    }));

    it('should queue a trailing call for subsequent debounced calls after `maxWait`', () => new Promise<void>(done => {
        let callCount = 0;

        const debounced = debounce<void, any>({wait: 200, maxWait: 200}, () => ++callCount);

        debounced();

        setTimeout(debounced, 190);
        setTimeout(debounced, 200);
        setTimeout(debounced, 210);

        setTimeout(() => {
            assert.strictEqual(callCount, 2);
            done();
        }, 500);
    }));

    it('should cancel `maxDelayed` when `delayed` is invoked', () => new Promise<void>(done => {
        let callCount = 0;

        const debounced = debounce<void, any>({wait: 32, maxWait: 64}, () => callCount++);

        debounced();

        setTimeout(() => {
            debounced();
            assert.strictEqual(callCount, 1);
        }, 128);

        setTimeout(() => {
            assert.strictEqual(callCount, 2);
            done();
        }, 192);
    }));

    it('should has options as a first argument', () => {
        /* eslint-disable no-unused-vars */
        const debouncedWithAllOptions = debounce({
            wait: 64,
            trailing: false,
            leading: true,
            maxWait: 500,
        }, identity);

        // @ts-expect-error
        const debouncedWithoutOptions = debounce(123, identity);
        /* eslint-enable no-unused-vars */
    });

    it('should has function as a second argument and preserve original function signature', () => {
        /* eslint-disable no-unused-vars */
        /* eslint-disable no-unused-expressions */
        const debounceWithFunction = debounce({wait: 64}, identity);

        (debounceWithFunction as typeof identity);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (debounceWithFunction as () => {});

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        const debouncedWithoutOptions = () => debounce({wait: 32}, null);
        expect(debouncedWithoutOptions).toThrow(InvariantViolation);

        // up to 3 arguments function
        debounce({wait: 32}, (a: number) => ([a]));
        debounce({wait: 32}, (a: number, b: number) => ([a, b]));
        debounce({wait: 32}, (a: number, b: number, c: number) => ([a, b, c]));

        // @ts-expect-error
        debounce({wait: 32}, (a: number, b: number, c: number, d: number) => ([a, b, c, d]));

        /* eslint-enable no-unused-vars */
        /* eslint-enable no-unused-expressions */
    });
});
