/* eslint-disable no-unused-expressions */

import {intersection} from '..';

describe('intersection', () => {
    it('returns intersection of two arrays', () => {
        expect(intersection([1, 2], [2, 3])).toEqual([2]);
    });

    it('returns intersection of two arrays with carrying', () => {
        expect(intersection([1, 2])([2, 3])).toEqual([2]);
    });

    it('returns empty array if either array is empty', () => {
        expect(intersection([], [2, 3])).toEqual([]);
        expect(intersection([1, 2], [])).toEqual([]);
    });

    it('properly typed', () => {
        (intersection([1, 2], [2, 3]) as Array<number>);
        // @ts-expect-error
        (intersection([1, 2], [2, 3]) as Array<string>);
        // @ts-expect-error
        (intersection([1, 2], [2, 3], [3, 4]) as Array<number>);

        function intersectFirstIncorrectArg() {
            // @ts-expect-error
            (intersection('string', [1, 2]) as Array<number>);
        }

        function intersectSecondIncorrectArg() {
            // @ts-expect-error
            (intersection([1, 2], 'string') as Array<number>);
        }

        expect(intersectFirstIncorrectArg).toThrow();
        expect(intersectSecondIncorrectArg).toThrow();
    });
});
