/* eslint-disable no-unused-expressions */

import {reduce} from '..';
import {expectType} from '../helpers';

type entityId = string;

describe('reduce', () => {
    const add = (a: number, b: number): number => a + b;
    const mult = (a: number, b: number): number => a * b;
    const concat = (a: number[], b: number): number[] => a.concat(b);

    it('folds simple functions over arrays with the supplied accumulator', () => {
        expect<number>(reduce(add, 1, [1, 2])).toEqual(4);
        expect<number>(reduce(mult, 1, [2, 3])).toEqual(6);
        expect<number[]>(reduce(concat, [1], [2, 3])).toEqual([1, 2, 3]);
    });

    it('folds simple functions over objects with the supplied accumulator', () => {
        expect<number>(reduce(add, 1, {a: 1, b: 2, c: 3})).toEqual(7);

        const concatKeys = (acc, value, key) => acc + key;
        expect<string>(reduce(concatKeys, '', {a: 1, b: 2, c: 3})).toEqual('abc');
    });

    it('returns the accumulator for an empty array', () => {
        expect<number>(reduce(add, 1, [])).toEqual(1);
        expect<number>(reduce(add, 1, [])).toEqual(1);
    });

    it('is curried and has required typings', () => {
        expect<number>(reduce(add)(1, [1, 2])).toEqual(4);
        expect<number>(reduce(add)(1)([1, 2])).toEqual(4);
        expect<number>(reduce(add, 1)([1, 2])).toEqual(4);
    });

    it('has correct typings for objects', () => {
        const source: {
            a: 'A',
            b: 'B',
            c: 'C',
        } = {a: 'A', b: 'B', c: 'C'};

        const iterator = (acc, value: 'A' | 'B' | 'C', key: 'a' | 'b' | 'c', collection: typeof source) => acc;
        reduce(iterator, {}, source);
        // @ts-expect-error Wrong type of value
        reduce((acc, value: number, key: 'a' | 'b' | 'c', collection: typeof source) => acc, {}, source);
        // @ts-expect-error Wrong type of key
        reduce((acc, value: 'A' | 'B' | 'C', key: number, collection: typeof source) => acc, {}, source);
        // @ts-expect-error Wrong type of collection
        reduce((acc, value: 'A' | 'B' | 'C', key: 'a' | 'b' | 'c', collection: {
            a: number,
            b: number,
            c: number,
        }) => acc, {}, source);

        const iteratorMixed = (
            acc: {A: string, B: string, C: string},
            value: 'A' | 'B' | 'C',
            key: string,
            collection: typeof source,
        ) => acc;
        // @ts-expect-error Wrong type of value
        reduce((acc, value: number, key: string, collection: typeof source) => acc, {}, source);
        // @ts-expect-error Wrong type of key
        reduce((acc, value: 'A' | 'B' | 'C', key: number, collection: typeof source) => acc, {}, source);
        // @ts-expect-error Wrong type of collection
        reduce((acc, value: 'A' | 'B' | 'C', key: string, collection: {
            a: number,
            b: number,
            c: number,
        }) => acc, {}, source);

        expectType<{
            A: string,
            B: string,
            C: string
        }>(reduce(iterator, {}, source));
        expectType<{
            A: number,
            B: string,
            C: string
        }>(reduce(iterator, {}, source));

        expectType<{
            A: string,
            B: string,
            C: string
        }>(reduce(iteratorMixed, {} as unknown as {A: string, B: string, C: string}, source));
        expectType<{
            A: number,
            B: string,
            C: string
            // @ts-expect-error
        }>(reduce(iteratorMixed, {} as unknown as {A: string, B: string, C: string}, source));
        expectType<{
            A: string,
            B: string,
            C: number
            // @ts-expect-error
        }>(reduce(iteratorMixed, {} as unknown as {A: string, B: string, C: string})(source));
    });

    it('has correct typings for tuples', () => {
        const source = ['a', 'bb', 'ccc'] as const;
        type Accumulator = {a: number, bb: number, ccc: number};

        const iterator = (acc: Accumulator, value: 'a' | 'bb' | 'ccc', index: number, collection) => {
            acc[value] = value.length + index;
            return acc;
        };

        reduce(iterator, {} as Accumulator, source);
        // @ts-expect-error Wrong type of value
        reduce((acc, value: number, index: number, collection) => acc, {}, source);
        // @ts-expect-error Wrong type of index
        reduce((acc, value, index: string, collection) => acc, {}, source);
        // @ts-expect-error Wrong type of collection
        reduce((acc, value, index: number, collection: number[]) => acc, {}, source);

        expectType<{
            a: number,
            bb: number,
            ccc: number
        }>(reduce(iterator, {} as Accumulator, source));

        expectType<{
            a: string,
            bb: number,
            ccc: number
            // @ts-expect-error
        }>(reduce(iterator, {} as Accumulator, source));

        expectType<{
            a: number,
            bb: number,
            ccc: number
        }>(reduce(iterator, {} as Accumulator)(source));

        expectType<{
            a: string,
            bb: number,
            ccc: number
            // @ts-expect-error
        }>(reduce(iterator, {} as Accumulator)(source));

        expectType<{
            a: number,
            bb: number,
            ccc: number
        }>(reduce(iterator)({} as Accumulator, source));

        expectType<{
            a: string,
            bb: number,
            ccc: number
            // @ts-expect-error
        }>(reduce(iterator)({} as Accumulator, source));

        expectType<{
            a: number,
            bb: number,
            ccc: number
        }>(reduce(iterator)({} as Accumulator)(source));

        expectType<{
            a: string,
            bb: number,
            ccc: number
            // @ts-expect-error
        }>(reduce(iterator)({} as Accumulator)(source));
    });

    it('has correct typings for arrays', () => {
        const source: entityId[] = ['a', 'bb', 'ccc'];

        reduce((acc: number[], value: entityId, index: number, collection: typeof source) => acc, [], source);
        // @ts-expect-error Wrong type of value
        reduce((acc: number[], value: number, index: number, collection: typeof source) => acc, [], source);
        // @ts-expect-error Wrong type of index
        reduce((acc: number[], value: entityId, index: string, collection: typeof source) => acc, [], source);
        // @ts-expect-error Wrong type of collection
        reduce((acc: number[], value: entityId, index: number, collection: number[]) => acc, [], source);

        const iterator = (acc: number[], value: entityId, index: number, collection: typeof source) => {
            acc.push(value.length + index);
            return acc;
        };

        expectType<number[]>(reduce(iterator, [])(source));
        // @ts-expect-error
        expectType<string[]>(reduce(iterator, [])(source));

        expectType<number[]>(reduce(iterator)([])(source));
        // @ts-expect-error
        expectType<string[]>(reduce(iterator)([])(source));

        expectType<number[]>(reduce(iterator, [], source));
        // @ts-expect-error
        expectType<string[]>(reduce(iterator, [], source));

        expectType<number[]>(reduce(iterator)([], source));
        // @ts-expect-error
        expectType<string[]>(reduce(iterator)([], source));
    });
});
