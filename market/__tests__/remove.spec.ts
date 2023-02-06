/* eslint-disable no-unused-expressions */

import {expectType} from '../helpers';
import {remove} from '..';

describe('remove', () => {
    it('has right types for arrays', () => new Promise<void>(done => {
        const arr = [1, 3, 5, 7];

        const cbArray = (val: number, index: number, collection: typeof arr) => true;
        remove(cbArray, arr);
        // @ts-expect-error Check value
        remove((val: string, index: number, collection: typeof arr) => true, arr);
        // @ts-expect-error Check index
        remove((val: number, index: string, collection: typeof arr) => true, arr);
        // @ts-expect-error Check collection
        remove((val: number, index: number, collection: string[]) => true, arr);

        expectType<number[]>(remove(cbArray, arr));
        // @ts-expect-error Expected type is number[]
        expectType<string[]>(remove(cbArray, arr));

        expectType<number[]>(remove(cbArray)(arr));
        // @ts-expect-error Expected type is number[]
        expectType<string[]>(remove(cbArray)(arr));

        done();
    }));

    it('has right types for objects', () => new Promise<void>(done => {
        const obj = {foo: 42, bar: 17};

        const cbObject = (val: number, index: 'foo' | 'bar', collection: typeof obj) => true;
        remove(cbObject, obj);
        // @ts-expect-error Check value
        remove((val: string, index: 'foo' | 'bar', collection: typeof obj) => true, obj);
        // @ts-expect-error Check index
        remove((val: number, index: number, collection: typeof obj) => true, obj);
        // @ts-expect-error Check collection
        remove((val: number, index: 'foo' | 'bar', collection: {abc: 333}) => true, obj);

        expectType<number[]>(remove(cbObject, obj));
        // @ts-expect-error Expected type is number[]
        expectType<string[]>(remove(cbObject, obj));
        expectType<number[]>(remove(cbObject)(obj));
        // @ts-expect-error Expected type is number[]
        expectType<string[]>(remove(cbObject)(obj));

        done();
    }));

    it('should remove items in array', () => {
        const arr = [1, 2, 3, 4, 5];

        expect(remove((x: number) => (x % 2) === 0, arr)).toEqual([1, 3, 5]);
        expect(remove((x: number) => (x % 2) === 0)(arr)).toEqual([1, 3, 5]);
    });

    it('should remove items in object', () => {
        const obj = {
            foo: 42,
            bar: 17,
            bazz: 33,
            oooooo: 3,
        };

        expect(remove((value, key) => value < 30 && key.length < 4, obj)).toEqual([42, 33, 3]);
        expect(remove((value, key) => value < 30 && key.length < 4)(obj)).toEqual([42, 33, 3]);
    });

    it('should not accept a falsey `collection`', () => {
        // eslint-disable-next-line
        const falsey = [, null, undefined, false, 0, NaN, ''];

        for (const falsy of falsey) {
            // @ts-expect-error
            expect(() => remove(x => Boolean(x), falsy)).toThrow();
        }
    });

    it('should not accept primitives', () => {
        // @ts-expect-error
        expect(() => remove(x => Boolean(x), 'abcde')).toThrow();
        // @ts-expect-error
        expect(() => remove(x => Boolean(x), 1)).toThrow();

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => remove(x => Boolean(x), null)).toThrow();
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => remove(x => Boolean(x), undefined)).toThrow();
    });
});
