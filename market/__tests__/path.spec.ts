/* eslint-disable no-unused-expressions */
/* eslint-disable no-unused-vars */

import {InvariantViolation} from '@yandex-market/invariant';

import {path} from '..';

describe('path', () => {
    it('takes a path and an object and returns the val at the path', () => {
        const obj = {
            a: {
                b: {
                    c: 100,
                    d: 200,
                },
                e: {
                    f: [100, 101, 102],
                    g: 'G',
                },
                h: 'H',
                f: [100, 101, 102],
            },
            i: 'I',
            j: ['J'],
        };

        expect(path(['a', 'e', 'f', 1], obj)).toEqual(101);
        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258 curry
        expect(path(['a', 'e', 'f', 1])(obj)).toEqual(101);
        expect(path(['a', 'b', 'c'], obj)).toEqual(100);
        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258 curry
        expect(path(['a', 'b', 'c'])(obj)).toEqual(100);
        expect(path(['j', 0], obj)).toEqual('J');
        expect(path(['j', 1], obj)).toEqual(undefined);
    });

    it('may throw an exception if any property does not exist', () => {
        function noSuchProp() {
            // @ts-expect-error
            path(['a', 'b'], {b: {a: 333}});
        }

        expect(noSuchProp).toThrow();
    });

    it('throws an exception if first argument is not array', () => {
        function stringPath() {
            // @ts-expect-error
            path('b.a', {b: {a: 333}});
        }
        function objectPath() {
            // @ts-expect-error
            path({b: 'a'}, {b: {a: 333}});
        }
        function nullPath() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            path(null, {b: {a: 333}});
        }

        expect(stringPath).toThrow();
        expect(objectPath).toThrow();
        expect(nullPath).toThrow();
    });

    it('takes first prop type and works with different objects', () => {
        /**
         * testing here twice with different objects because of special bug in flown implementaion
         * @see {@link https://github.yandex-team.ru/market/rainbow/pull/50/files#diff-eb49b3dc322bf081befac4e8f043dec1R6}
         */
        const obj = {a: 123};
        const obj2 = {a: 333};

        (path(['a'], obj) as 123);
        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258 curry
        (path(['a'])(obj) as 123);

        (path(['a'], obj2) as 333);
        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258 curry
        (path(['a'])(obj2) as 333);

        // @ts-expect-error
        (path(['b'], obj));
        // @ts-expect-error
        (path(['b'])(obj));

        // @ts-expect-error
        path(['b'], obj2);
        // @ts-expect-error
        path(['b'])(obj2);
    });

    it('takes second prop type', () => {
        const obj = {a: {a: 333}};

        (path(['a', 'a'], obj) as 333);
        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258 curry
        (path(['a', 'a'])(obj) as 333);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error 222 is number
        (path(['a', 'a'], obj) as 222);
        // @ts-expect-error curry
        (path(['a', 'a'])(obj) as 222);

        // @ts-expect-error
        path(['a', 'b'], obj);
        // @ts-expect-error
        path(['a', 'b'])(obj);
    });

    it('takes third prop type', () => {
        const obj = {a: {a: {a: 333}}};

        (path(['a', 'a', 'a'], obj) as 333);
        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258 curry
        (path(['a', 'a', 'a'])(obj) as 333);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error 222 is number
        (path(['a', 'a', 'a'], obj) as 222);
        // @ts-expect-error curry
        (path(['a', 'a', 'a'])(obj) as 222);

        // @ts-expect-error
        path(['a', 'a', 'b'], obj);
        // @ts-expect-error
        path(['a', 'a', 'b'])(obj);
    });

    it('takes fourth prop type', () => {
        const obj = {a: {a: {a: {a: 333}}}};

        (path(['a', 'a', 'a', 'a'], obj) as 333);
        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258 curry
        (path(['a', 'a', 'a', 'a'])(obj) as 333);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error 222 is number
        (path(['a', 'a', 'a', 'a'], obj) as 222);
        // @ts-expect-error curry
        (path(['a', 'a', 'a', 'a'])(obj) as 222);

        // @ts-expect-error
        path(['a', 'a', 'a', 'b'], obj);
        // @ts-expect-error
        path(['a', 'a', 'a', 'b'])(obj);
    });

    it('takes fifth prop type', () => {
        const obj = {a: {a: {a: {a: {a: 333}}}}};

        (path(['a', 'a', 'a', 'a', 'a'], obj) as 333);
        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258 curry
        (path(['a', 'a', 'a', 'a', 'a'])(obj) as 333);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error 222 is number
        (path(['a', 'a', 'a', 'a', 'a'], obj) as 222);
        // @ts-expect-error curry
        (path(['a', 'a', 'a', 'a', 'a'])(obj) as 222);

        // @ts-expect-error
        path(['a', 'a', 'a', 'a', 'b'], obj);
        // @ts-expect-error curry
        path(['a', 'a', 'a', 'a', 'b'])(obj);
    });
});
