/* eslint-disable no-unused-expressions */

import {compose, map, filter} from '..';

const inc: (a: number) => number = x => x + 1;
const isOdd = x => x % 2 === 1;

describe('compose', () => {
    it('is a variadic function', () => {
        expect(typeof compose).toEqual('function');
        expect(compose.length).toEqual(0);
    });

    it('performs right-to-left function composition', () => {
        const f = compose(map(inc), filter(isOdd));

        expect(f([1, 2, 3])).toEqual([2, 4]);
    });

    it('passes context to functions', () => {
        function x(val) {
            return this.x * val;
        }
        function y(val) {
            return this.y * val;
        }
        function z(val) {
            return this.z * val;
        }
        const context = {
            a: compose(x, y, z),
            x: 4,
            y: 2,
            z: 1,
        };
        expect(context.a(5)).toEqual(40);
    });

    it('throws if given no arguments', () => {
        function emptyCompose() {
            // @ts-expect-error
            compose();
        }

        expect(emptyCompose).toThrow();
    });

    it('throws error on incompatable function arguments', () => {
        const concatStrings = (a: string, b: string) => a + b;
        const addTen: (a: number) => number = a => a + 10;

        // @ts-expect-error
        compose(inc, concatStrings);

        const fn = compose(inc, addTen);

        (fn(10) as number);

        // @ts-expect-error
        (fn(10) as string);

        // @ts-expect-error
        fn('10');
    });

    it('throws error if given non-unary functions', () => {
        const add2 = (a: number, b: number) => a + b;
        const addTen: (a: number) => number = a => a + 10;

        compose(inc, addTen);

        // @ts-expect-error
        compose(add2, addTen);

        // todo: why it's an error? seems ok to me @fenruga
        compose(inc, add2);

        // @ts-expect-error
        compose(inc, add2, addTen);
    });
});
