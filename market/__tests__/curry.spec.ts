import {curry} from '..';

describe('curry', () => {
    function fn(a: unknown, b: unknown, c: unknown, d: number): Array<unknown> {
        return [a, b, c, d];
    }

    it('принимает ровно один аргумент. Аргумент - функция', () => {
        // https://github.com/lodash/lodash/blob/912d6b04a1f6b732508a6da72a95ec4f96bda154/test/test-fp.js#L835
        expect(() => curry(fn)).not.toThrow();

        // @ts-expect-error
        expect(() => curry(fn, 2)).toThrow();

        // @ts-expect-error
        expect(() => curry(42)).toThrow();
    });

    it('каррирует функцию', () => {
        const curried = curry(fn);
        const expected = [1, 2, 3, 4];

        expect(curried(1)(2)(3)(4)).toEqual(expected);
        expect(curried(1, 2)(3, 4)).toEqual(expected);
        expect(curried(1, 2, 3, 4)).toEqual(expected);
    });

    it('у полученных функций `length` === `0`', () => {
        const curried = curry(fn);

        expect(curried.length).toEqual(0);
        expect(curried(1).length).toEqual(0);
        expect(curried(1, 2).length).toEqual(0);
    });

    it('для полученной `curried` функции `new curried` является инстансом `func`', () => {
        const object = {};

        function Foo(value) {
            return value && object;
        }

        const Curried = curry(Foo);

        // @ts-ignore
        expect(new Curried(false) instanceof Foo).toEqual(true);
        // @ts-ignore
        expect(new Curried(true)).toEqual(object);
    });

    it('наследуют `this`-байндинг от исходной функции', () => {
        // eslint-disable-next-line
        const fn = function (a: string, b: string, c: string) {
            const value = this || {};
            return [value[a], value[b], value[c]];
        };

        const object = {
            a: 1, b: 2, c: 3,
        };
        const expected = [1, 2, 3];

        // @ts-expect-error broken fn.bind typings
        expect(curry(fn.bind(object))('a')('b')('c')).toEqual(expected);
        // @ts-expect-error broken fn.bind typings
        expect(curry(fn.bind(object))('a', 'b')('c')).toEqual(expected);
        // @ts-expect-error broken fn.bind typings
        expect(curry(fn.bind(object))('a', 'b', 'c')).toEqual(expected);

        expect(curry(fn).bind(object)('a')('b')('c')).toEqual(Array(3));
        expect(curry(fn).bind(object)('a', 'b')('c')).toEqual(Array(3));
        expect(curry(fn).bind(object)('a', 'b', 'c')).toEqual(expected);

        const object2 = {...object, curried: curry(fn)};
        expect(object2.curried('a')('b')('c')).toEqual(Array(3));
        expect(object2.curried('a', 'b')('c')).toEqual(Array(3));
        expect(object2.curried('a', 'b', 'c')).toEqual(expected);
    });

    describe('правильно типизирована', () => {
        /* eslint-disable no-unused-expressions */
        it('арность 1', () => {
            (curry((a: number) => a) as (a: number) => number);

            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            (curry((a: number) => a) as (a: unknown) => number);
            // @ts-expect-error
            (curry((a: number) => a) as (a: number) => string);
        });

        it('арность 2', () => {
            const arity2 = (a: number, b: number) => a + b;
            (curry(arity2) as (a: number) => (a: number) => number);
            (curry(arity2) as (b: number, a: number) => number);

            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            (curry(arity2) as (a: any) => (a: number) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            (curry(arity2) as (a: number) => (a: any) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            (curry(arity2) as (b: any, a: any) => number);
        });

        it('арность 3', () => {
            const arity3 = (a: number, b: number, c: number) => a + b + c;

            (curry(arity3) as (a: number) => (a: number) => (a: number) => number);
            (curry(arity3) as (a: number) => (b: number, a: number) => number);
            (curry(arity3) as (b: number, a: number) => (a: number) => number);
            (curry(arity3) as (c: number, b: number, a: number) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it error?
            (curry(arity3) as (d: number, c: number, b: number, a: number) => number);
            // @ts-expect-error
            (curry(arity3) as (b: number, a: number) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it error?
            (curry(arity3) as (a: any) => (a: number) => (a: number) => number);
        });

        it('арность 4', () => {
            const arity4 = (a: number, b: number, c: number, d: number) => a + b + c + d;
            (curry(arity4) as (a: number) => (a: number) => (a: number) => (a: number) => number);
            (curry(arity4) as (b: number, a: number) => (a: number) => (a: number) => number);
            (curry(arity4) as (b: number, a: number) => (b: number, a: number) => number);

            (curry(arity4) as (c: number, b: number, a: number) => (a: number) => number);
            (curry(arity4) as (d: number, c: number, b: number, a: number) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it error?
            (curry(arity4) as (e: number, d: number, c: number, b: number, a: number) => number);
            // @ts-expect-error
            (curry(arity4) as (c: number, b: number, a: number) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it error?
            (curry(arity4) as (a: any) => (a: number) => (a: number) => (a: number) => number);
        });

        it('арность 5', () => {
            const arity5 = (a: number, b: number, c: number, d: number, e: number) => a + b + c + d + e;
            (curry(arity5) as (a: number) => (a: number) => (a: number) => (a: number) => (a: number) => number);
            (curry(arity5) as (a: number) => (a: number) => (a: number) => (b: number, a: number) => number);
            (curry(arity5) as (a: number) => (a: number) => (b: number, a: number) => (a: number) => number);
            (curry(arity5) as (a: number) => (b: number, a: number) => (a: number) => (a: number) => number);
            (curry(arity5) as (a: number) => (b: number, a: number) => (b: number, a: number) => number);
            (curry(arity5) as (a: number) => (d: number, c: number, b: number, a: number) => number);
            (curry(arity5) as (b: number, a: number) => (a: number) => (a: number) => (a: number) => number);
            (curry(arity5) as (c: number, b: number, a: number) => (a: number) => (a: number) => number);
            (curry(arity5) as (d: number, c: number, b: number, a: number) => (a: number) => number);
            (curry(arity5) as (e: number, d: number, c: number, b: number, a: number) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it error?
            (curry(arity5) as (f: number, e: number, d: number, c: number, b: number, a: number) => number);
            // @ts-expect-error
            (curry(arity5) as (d: number, c: number, b: number, a: number) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it error?
            (curry(arity5) as (a: any) => (a: number) => (a: number) => (a: number) => (a: number) => number);
        });

        it('арность 6', () => {
            const arity6 = (a: number, b: number, c: number, d: number, e: number, f: number) => a + b + c + d + e + f;
            (curry(arity6) as (a: number) =>
                (a: number) => (a: number) => (a: number) => (a: number) => (a: number) => number);
            (curry(arity6) as (b: number, a: number) =>
                (a: number) => (a: number) => (a: number) => (a: number) => number);
            (curry(arity6) as (c: number, b: number, a: number) => (a: number) => (a: number) => (a: number) => number);
            (curry(arity6) as (d: number, c: number, b: number, a: number) => (a: number) => (a: number) => number);
            (curry(arity6) as (e: number, d: number, c: number, b: number, a: number) => (a: number) => number);
            (curry(arity6) as (f: number, e: number, d: number, c: number, b: number, a: number) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it error?
            (curry(arity6) as (
                g: number,
                f: number,
                e: number,
                d: number,
                c: number,
                b: number,
                a: number
            ) => number);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it error?
            (curry(arity6) as (
                a: any) => (a: number) => (a: number) => (a: number) => (a: number) => (a: number) => number
            );
        });

        it('кроме арностей выше 6', () => {
            const arity7 = (a: number, b: number, c: number, d: number, e: number, f: number, g: number) => a + b + c + d + e + f + g;
            // fallback to `any` if arity is > than 6
            (curry(arity7) as (
                g: number,
                f: number,
                e: number,
                d: number,
                c: number,
                b: number,
                a: number
            ) => number);
        });
        /* eslint-enable no-unused-expressions */
    });
});
