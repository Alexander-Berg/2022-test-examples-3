/* eslint-disable no-unused-expressions */

import {InvariantViolation} from '@yandex-market/invariant';

import {concat} from '..';
import {expectType} from '../helpers';

const eq = (a, b) => expect(a).toEqual(b);

describe('concat', () => {
    it('adds combines the elements of the two lists', () => {
        eq(concat(['a', 'b'], ['c', 'd']), ['a', 'b', 'c', 'd']);

        const result = concat([] as const, ['c', 'd'] as ['c', 'd']);
        eq(result, ['c', 'd']);
        // Preservers type of tuples
        expectType<['c', 'd']>(result);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 still fair @ts-expect-error
        expectType<string[]>(result);

        const result2 = concat(['a', 'b', 'c'] as const, ['d', 'e'] as const);
        expectType<['a', 'b', 'c', 'd', 'e']>(result2);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 still fair @ts-expect-error
        expectType<string[]>(result2);

        const stringArray: string[] = ['abc'];
        const stringArraySecond: string[] = ['wba', 'amc'];

        const stringArrayResult = concat(stringArray, stringArraySecond);

        expectType<string[]>(stringArrayResult);
        // @ts-expect-error
        expectType<number[]>(stringArrayResult);

        const stringArrayResultCurried = concat(stringArray)(stringArraySecond);

        expectType<string[]>(stringArrayResultCurried);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error curry
        expectType<number[]>(stringArrayResultCurried);
    });

    it('works on strings', () => {
        const fooBarResult = concat('foo', 'bar');

        expectType<string>(fooBarResult);

        // @ts-expect-error
        expectType<number>(fooBarResult);

        eq(fooBarResult, 'foobar');

        eq(concat('x', ''), 'x');
        eq(concat('', 'x'), 'x');
        eq(concat('', ''), '');
    });

    it('throws if attempting to combine an array with a non-array', () => {
        // @ts-expect-error
        const concatWithNumber = () => concat([1], 2);

        expect(concatWithNumber).toThrow(InvariantViolation);
    });

    it('throws if not an array, String, or object with a concat method', () => {
        // @ts-expect-error
        const concatWithObject = () => concat({}, {});

        expect(concatWithObject).toThrow(InvariantViolation);
    });
});
