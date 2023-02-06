/* eslint-disable no-unused-expressions */

import {uniq} from '..';

const identity = x => x;
const add = (x, y) => x + y;

const eq = (a, b) => expect(a).toEqual(b);

describe('uniq', () => {
    it('returns a set from any array (i.e. purges duplicate elements)', () => {
        const list = [1, 2, 3, 1, 2, 3, 1, 2, 3];
        const uniqList = uniq(list);
        eq(uniqList, [1, 2, 3]);

        (uniqList as typeof list);

        // @ts-expect-error
        (uniqList as string[]);
    });

    it('keeps elements from the left', () => {
        eq(uniq([1, 2, 3, 4, 1]), [1, 2, 3, 4]);
    });

    it('returns an empty array for an empty array', () => {
        eq(uniq([]), []);
        const emptyArr = uniq([]);

        (emptyArr as Array<void>);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error is it an error?
        (emptyArr as Array<number>);
    });

    it('compares elements using SameZeroValue semantic', () => {
        eq(uniq([-0, -0]).length, 1);
        eq(uniq([0, -0]).length, 1);
        eq(uniq([NaN, NaN]).length, 1);
        eq(uniq([[1], [1]]).length, 2);

        const someArray = [1];
        eq(uniq([someArray, someArray]).length, 1);
    });

    it('handles null and undefined elements', () => {
        eq(uniq([undefined, null, undefined, null]), [undefined, null]);
    });

    it('uses reference equality for functions', () => {
        eq(uniq([add, identity, add, identity, add, identity]).length, 2);
    });
});
