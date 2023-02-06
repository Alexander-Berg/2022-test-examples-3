import {length} from '..';

describe('length', () => {
    const array = [1, 2, 3];
    const MAX_SAFE_INTEGER = 9007199254740991;
    // eslint-disable-next-line
    const falsey = [, null, undefined, false, 0, NaN];

    it('has right type', () => new Promise<void>(done => {
        /* eslint-disable */
        (length([]) as number);
        (length([1, 2, 3, 4]) as number);
        (length({}) as number);
        (length('') as number);
        /* eslint-enable */
        done();
    }));

    it('should not accept a falsey `object`', () => {
        for (const falsy of falsey) {
            // @ts-expect-error
            expect(() => length(falsy)).toThrow();
        }
    });

    it('should not change type of argument object', () => new Promise<void>(done => {
        /* eslint-disable */
        let obj: {
            foo: number
        } & {
            bar: string
        } = {foo: 42, bar: ''};

        if (length(obj)) {
            (obj.foo as number);
        }

        // @ts-expect-error
        (obj.bar as number);

        /* eslint-enable */
        done();
    }));

    it('should return the number of own enumerable string keyed properties of an object', () => {
        expect(length({one: 1, two: 2, three: 3})).toBe(3);
    });

    it('should return the length of an array', () => {
        expect(length(array)).toBe(3);
    });

    it('should work with `arguments` objects', () => {
        /* eslint-disable */
        (function (...args: number[]) {
            expect(length(arguments)).toBe(3);
        }(...array));
        /* eslint-enable */
    });

    it('should work with maps', () => {
        const map = new Map();
        map.set('a', 1);
        map.set('b', 2);

        expect(length(map)).toBe(2);
    });

    it('should work with sets', () => {
        const set = new Set();
        set.add(1);
        set.add(2);

        expect(length(set)).toBe(2);
    });

    it('should not treat objects with negative lengths as array-like', () => {
        expect(length({length: -1})).toBe(1);
    });

    it('should not treat objects with lengths larger than `MAX_SAFE_INTEGER` as array-like', () => {
        expect(length({length: MAX_SAFE_INTEGER + 1})).toBe(1);
    });

    it('should not treat objects with non-number lengths as array-like', () => {
        expect(length({length: '0'})).toBe(1);
    });
});
