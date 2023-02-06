/* eslint-disable no-unused-expressions */
/* eslint-disable no-unused-vars */

import {InvariantViolation} from '@yandex-market/invariant';

import {keyBy} from '..';
import {expectType} from '../helpers';

const assert = {
    notStrictEqual: (a, b) => expect(a).not.toStrictEqual(b),
    strictEqual: (a, b) => expect(a).toStrictEqual(b),
    deepEqual: (a, b) => expect(a).toEqual(b),
    notToBe: (a, b) => expect(a).not.toBe(b),
};

const identity = x => x;

describe('keyBy', () => {
    const array = [
        {dir: 'left', code: 97},
        {dir: 'right', code: 100},
    ];
    const testObject = {left: {code: 97, key: 'a'}, right: {code: 100, key: 'd'}};

    it('should transform keys by `iteratee`', () => {
        const expected = {a: {dir: 'left', code: 97}, d: {dir: 'right', code: 100}};

        const result = keyBy(object => String.fromCharCode(object.code), array);

        assert.deepEqual(result, expected);
    });

    it('should keep typings for arrays', () => {
        const result = keyBy(object => object.code, array);
        const resultCurried = keyBy((object: {dir: 'left' | 'right', code: number}): number => object.code)(array);

        expectType<{
            [x: number]: {
                code: number,
                dir: string
            }
        }>(result);
        expectType<{
            [x: number]: {
                code: number,
                dir: string
            }
        }>(resultCurried);

        expectType<{
            [x: number]: {
                code: string,
                dir: number
            }
        // @ts-expect-error
        }>(result);
        expectType<{
            [x: number]: {
                code: string,
                dir: number
            }
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        }>(resultCurried);

        keyBy((val: {code: number, dir: string}, key: number, coll: typeof array) => val.code, array);
        // @ts-expect-error
        keyBy((val: {code: string, dir: string}, key: number, coll: typeof array) => val.code, array);
        // @ts-expect-error
        keyBy((val: {code: number, dir: string}, key: string, coll: typeof array) => val.code, array);
        // @ts-expect-error
        keyBy((val: {code: number, dir: string}, key: number, coll: {a: 42}[]) => val.code, array);

        keyBy((val: {code: number, dir: string}, key: number, coll: typeof array) => val.code)(array);
        // @ts-expect-error
        keyBy((val: {code: string, dir: string}, key: number, coll: typeof array) => val.code)(array);
        // @ts-expect-error
        keyBy((val: {code: number, dir: string}, key: string, coll: typeof array) => val.code)(array);
        // @ts-expect-error
        keyBy((val: {code: number, dir: string}, key: number, coll: {a: 42}[]) => val.code)(array);
    });

    it('should only add values to own, not inherited, properties', () => {
        const result = keyBy(n => (Math.floor(n) > 4 ? 'hasOwnProperty' : 'constructor'), [6.1, 4.2, 6.3]);

        assert.deepEqual(result.constructor, 4.2);
        assert.deepEqual(result.hasOwnProperty, 6.3);
    });

    it('should work with an object for `collection`', () => {
        const result = keyBy(Math.floor, {a: 6.1, b: 4.2, c: 6.3});
        const resultCurried = keyBy(Math.floor)({a: 6.1, b: 4.2, c: 6.3});
        // eslint-disable-next-line quote-props
        const expected = {'4': 4.2, '6': 6.3};
        assert.deepEqual(result, expected);
        assert.deepEqual(resultCurried, expected);
    });

    it('should keep typings for objects', () => {
        const result = keyBy(object => object.key, testObject);
        const resultCurried = keyBy(object => object.key)(testObject);

        expectType<{
            [x: string]: {
                code: number,
                key: string
            }
        }>(result);
        expectType<{
            [x: string]: {
                code: number,
                key: string
            }
        }>(resultCurried);

        expectType<{
            [x: string]: {
                code: string,
                key: number
            }
            // @ts-expect-error
        }>(result);
        expectType<{
            [x: string]: {
                code: string,
                key: number
            }
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        }>(resultCurried);

        keyBy((val: {code: number, key: string}, key: number, coll: typeof testObject) => val.code, testObject);
        // @ts-expect-error
        keyBy((val: null, key: number, coll: typeof testObject) => val.code, testObject);
        // @ts-expect-error
        keyBy((val: {code: number, key: string}, key: string, coll: typeof testObject) => val.code, testObject);
        // @ts-expect-error
        keyBy((val: {code: number, key: string}, key: number, coll: {[x: string]: number}) => val.code, testObject);
    });

    it('throws exception, if first argument is not a function', () => {
        function keyByWithObject() {
            // @ts-expect-error
            keyBy({}, [1, 2]);
        }
        function keyByWithNull() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            keyBy(null, [1, 2]);
        }

        expect(keyByWithObject).toThrow(InvariantViolation);
        expect(keyByWithNull).toThrow(InvariantViolation);
    });

    it('throws exception, if second argument is not an object or an array', () => {
        function keyByWithNull() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            keyBy(identity, null);
        }
        function keyByWithString() {
            // @ts-expect-error
            keyBy(identity, 'string');
        }

        expect(keyByWithNull).toThrow();
        expect(keyByWithString).toThrow();
    });
});
