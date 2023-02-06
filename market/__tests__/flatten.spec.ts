/* eslint-disable no-unused-expressions, no-unused-vars */

import {flatten} from '..';
import {expectType} from '../helpers';

describe('flatten', () => {
    it('flattens one-level arrays', () => {
        const exampleArray = [1, 'a', {}, {a: 'a'}, null, undefined, [], ['WOW'], [['DEEP']]];
        const flattenedArray = flatten(exampleArray);

        expect(flattenedArray).toEqual([1, 'a', {}, {a: 'a'}, null, undefined, 'WOW', ['DEEP']]);
    });

    it('tries to flatten array values with correct typings', () => {
        const exampleArray = [1, ['a']];
        const flattenedArray = flatten(exampleArray);

        expectType<Array<number | string>>(flattenedArray);

        // @ts-expect-error
        expectType<Array<string>>(flattenedArray);
        // @ts-expect-error
        expectType<Array<number>>(flattenedArray);
    });

    it('tries to flatten readonly array values with correct typings', () => {
        const exampleArray = [1, ['a']] as const;
        const flattenedArray = flatten(exampleArray);

        expectType<readonly [1, 'a']>(flattenedArray);
        expectType<ReadonlyArray<string | number>>(flattenedArray);

        // @ts-expect-error
        expectType<[1, 'a']>(flattenedArray);
        // @ts-expect-error
        expectType<Array<string | number>>(flattenedArray);
        // @ts-expect-error
        expectType<Array<string>>(flattenedArray);
        // @ts-expect-error
        expectType<ReadonlyArray<string>>(flattenedArray);
    });

    it('tries to flatten arrays with literals with correct typings', () => {
        const exampleArray: (1 | ['a'])[] = [1, ['a'], 1];
        const flattenedArray = flatten(exampleArray);

        expectType<(1 | 'a')[]>(flattenedArray);
        expectType<Array<string | number>>(flattenedArray);

        // @ts-expect-error
        expectType<Array<string>>(flattenedArray);
    });

    it('flattens only one-level depth', () => {
        const exampleArray = [1, [['a']]];
        const flattenedArray = flatten(exampleArray);

        expectType<Array<number | string[]>>(flattenedArray);

        // @ts-expect-error
        expectType<Array<string[]>>(flattenedArray);
        // @ts-expect-error
        expectType<Array<number>>(flattenedArray);
    });

    it('throws error if argument is not array', () => {
        function flattenWithObject() {
            // @ts-expect-error
            flatten({});
        }
        function flattenWithNull() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            flatten(null);
        }
        function flattenWithUndefined() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            flatten(undefined);
        }
        function flattenWithString() {
            // @ts-expect-error
            flatten('abc');
        }

        expect(flattenWithObject).toThrow();
        expect(flattenWithNull).toThrow();
        expect(flattenWithUndefined).toThrow();
        expect(flattenWithString).toThrow();
    });
});
