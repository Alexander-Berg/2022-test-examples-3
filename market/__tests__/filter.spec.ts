/* eslint-disable no-unused-expressions */

import {expectType} from '../helpers';
import {filter} from '..';

describe('filter', () => {
    it('has right types for arrays', () => new Promise<void>(done => {
        const arr = [1, 3, 5, 7];

        const cbArray = (val: number, index: number, collection: typeof arr) => true;
        filter(cbArray, arr);
        // @ts-expect-error Check value
        filter((val: string, index: number, collection: typeof arr) => true, arr);
        // @ts-expect-error Check index
        filter((val: number, index: string, collection: typeof arr) => true, arr);
        // @ts-expect-error Check collection
        filter((val: number, index: number, collection: string[]) => true, arr);

        expectType<number[]>(filter(cbArray, arr));
        // @ts-expect-error Expected type is number[]
        expectType<string[]>(filter(cbArray, arr));

        expectType<number[]>(filter(cbArray)(arr));
        // @ts-expect-error Expected type is number[]
        expectType<string[]>(filter(cbArray)(arr));

        done();
    }));

    it('has right types for objects', () => new Promise<void>(done => {
        const obj = {foo: 42, bar: 17};

        const cbObject = (val: number, index: 'foo' | 'bar', collection: typeof obj) => true;
        filter(cbObject, obj);
        // @ts-expect-error Check value
        filter((val: string, index: 'foo' | 'bar', collection: typeof obj) => true, obj);
        // @ts-expect-error Check index
        filter((val: number, index: number, collection: typeof obj) => true, obj);
        // @ts-expect-error Check collection
        filter((val: number, index: 'foo' | 'bar', collection: {abc: 333}) => true, obj);

        expectType<number[]>(filter(cbObject, obj));
        // @ts-expect-error Expected type is number[]
        expectType<string[]>(filter(cbObject, obj));
        expectType<number[]>(filter(cbObject)(obj));
        // @ts-expect-error Expected type is number[]
        expectType<string[]>(filter(cbObject)(obj));

        done();
    }));

    it('should filter through array', () => {
        const arr = [1, 2, 3, 4, 5];

        expect(filter((x: number) => (x % 2) === 0, arr)).toEqual([2, 4]);
        expect(filter((x: number) => (x % 2) === 0)(arr)).toEqual([2, 4]);
    });

    it('should filter through object', () => {
        const obj = {
            foo: 42,
            bar: 17,
            bazz: 33,
            oooooo: 3,
        };

        expect(filter((value, key) => value < 30 && key.length < 4, obj)).toEqual([17]);
        expect(filter((value, key) => value < 30 && key.length < 4)(obj)).toEqual([17]);
    });

    it('should not accept a falsey `collection`', () => {
        // eslint-disable-next-line
        const falsey = [, null, undefined, false, 0, NaN, ''];

        for (const falsy of falsey) {
            // @ts-expect-error
            expect(() => filter(x => Boolean(x), falsy)).toThrow();
        }
    });

    it('should not accept primitives', () => {
        // @ts-expect-error
        expect(() => filter(x => Boolean(x), 'abcde')).toThrow();
        // @ts-expect-error
        expect(() => filter(x => Boolean(x), 1)).toThrow();

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => filter(x => Boolean(x), null)).toThrow();
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => filter(x => Boolean(x), undefined)).toThrow();
    });
});
