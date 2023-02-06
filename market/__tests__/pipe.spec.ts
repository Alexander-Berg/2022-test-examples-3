/* eslint-disable no-unused-expressions */

import {pipe, map, filter, flow} from '..';

const inc: (a: number) => number = x => x + 1;
const isOdd = x => x % 2 === 1;

describe('pipe', () => {
    it('is a variadic function', () => {
        expect(typeof pipe).toEqual('function');
        expect(pipe.length).toEqual(0);
    });

    it('performs right-to-left function composition', () => {
        const f = pipe(map(inc), filter(isOdd));

        expect(f([0, 1, 2, 3])).toEqual([1, 3]);
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
            a: pipe(x, y, z),
            x: 4,
            y: 2,
            z: 1,
        };
        expect(context.a(5)).toEqual(40);
    });

    it('throws if given no arguments', () => {
        function emptyPipe() {
            // @ts-expect-error
            pipe();
        }

        expect(emptyPipe).toThrow();
    });

    it('throws error on incompatable function arguments', () => {
        const concatStrings = (a: string, b: string) => a + b;
        const addTen: (a: number) => number = a => a + 10;

        // @ts-expect-error string and number are incompatable
        pipe(inc, concatStrings);

        const fn = pipe(inc, addTen);

        (fn(10) as number);

        // @ts-expect-error
        (fn(10) as string);

        // @ts-expect-error
        fn('10');
    });

    it('throws error if given non-unary functions', () => {
        const add2 = (a: number, b: number) => a + b;
        const addTen: (a: number) => number = a => a + 10;

        pipe(inc, addTen);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error seems ok, 1st can be non-unary
        pipe(add2, addTen);

        // @ts-expect-error
        pipe(inc, add2);
    });

    it('has alias called `flow`', () => {
        expect(flow).toEqual(pipe);
    });
});
