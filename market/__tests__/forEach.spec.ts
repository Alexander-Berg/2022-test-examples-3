/* eslint-disable no-unused-expressions */

import {forEach} from '..';
import {expectType} from '../helpers';

describe('forEach', () => {
    it('has right types for arrays', () => new Promise<void>(done => {
        const arr = [1, 3, 5, 7];

        const cbArray = (val: number, index: number, collection: typeof arr) => {};
        forEach(cbArray, arr);
        // @ts-expect-error check value
        forEach((val: string, index: number, collection: typeof arr) => {}, arr);
        // @ts-expect-error check index
        forEach((val: number, index: string, collection: typeof arr) => {}, arr);
        // @ts-expect-error check collection
        forEach((val: number, index: number, collection: string[]) => {}, arr);

        expectType<typeof arr>(forEach(cbArray, arr));
        expectType<typeof arr>(forEach(cbArray)(arr));

        done();
    }));

    it('has right types for objects', () => new Promise<void>(done => {
        const obj = {foo: 42, bar: 17};

        const cbObject = (val: number, index: 'foo' | 'bar', collection: typeof obj) => {};
        forEach(cbObject, obj);
        // @ts-expect-error check value
        forEach((val: string, index: 'foo' | 'bar', collection: typeof obj) => {}, obj);
        // @ts-expect-error check index
        forEach((val: number, index: number, collection: typeof obj) => {}, obj);
        // @ts-expect-error check collection
        forEach((val: number, index: 'foo' | 'bar', collection: {abc: 333}) => {}, obj);

        expectType<typeof obj>(forEach(cbObject, obj));
        expectType<typeof obj>(forEach(cbObject)(obj));

        forEach(cbObject, obj);
        forEach(cbObject)(obj);

        done();
    }));

    it('should forEach through collection returning same collection', () => {
        const object = {a: 1, b: 2};
        const arr = ['a', 'b'];

        const accObject = [];
        expect(forEach((value, key) => accObject.push([value, key]), object)).toEqual(object);
        expect(accObject).toEqual([[1, 'a'], [2, 'b']]);

        const accArray = [];
        expect(forEach((value, index) => accArray.push([value, index]), arr)).toEqual(arr);
        expect(accArray).toEqual([['a', 0], ['b', 1]]);
    });

    it('should support early exit if returning value is false', () => {
        const arr = [1, 2, 3, 4, 5];
        const accArray = [];

        forEach(x => {
            accArray.push(x);

            return x < 3;
        }, arr);

        expect(accArray).toEqual([1, 2, 3]);
    });

    it('should not accept a falsey `collection`', () => {
        // eslint-disable-next-line
        const falsey = [, null, undefined, false, 0, NaN, ''];

        for (const falsy of falsey) {
            // @ts-expect-error
            expect(() => forEach(x => x, falsy)).toThrow();
        }
    });

    it('should not accept primitives', () => {
        // @ts-expect-error
        expect(() => forEach(x => x, 'abcde')).toThrow();
        // @ts-expect-error
        expect(() => forEach(x => x, 1)).toThrow();
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => forEach(x => x, null)).toThrow();
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => forEach(x => x, undefined)).toThrow();
    });
});
