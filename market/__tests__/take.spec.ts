/* eslint-disable no-unused-expressions */

import {take} from '..';

describe('take', () => {
    it('should throw exception if first argument is not a number', () => {
        // @ts-expect-error
        expect(() => take('', [1, 2])).toThrow();
        // @ts-expect-error
        expect(() => take('')([1, 2])).toThrow();
    });

    it('should throw exception if second argument is not an array', () => {
        // @ts-expect-error
        expect(() => take(1, '')).toThrow();
        // @ts-expect-error
        expect(() => take(1)('')).toThrow();
    });

    it('should return first n values', () => {
        expect(take(3, [1, 2, 3, 4, 5])).toEqual([1, 2, 3]);
        expect(take(3)([1, 2, 3, 4, 5])).toEqual([1, 2, 3]);

        expect(take(3, [1])).toEqual([1]);
        expect(take(3)([1])).toEqual([1]);

        expect(take(3, [1])).toEqual([1]);
        expect(take(3)([1])).toEqual([1]);

        expect(take(0, [1])).toEqual([]);
        expect(take(0)([1])).toEqual([]);
    });

    it('should have right type for return value', () => {
        (take(3, [1, 2, 3, 4, 5]) as number[]);
        // @ts-expect-error
        (take(3, [1, 2, 3, 4, 5]) as string[]);

        (take(3)([1, 2, 3, 4, 5]) as number[]);
        // @ts-expect-error
        (take(3)([1, 2, 3, 4, 5]) as string[]);
    });
});
