/* eslint-disable no-unused-expressions */

import {takeRight} from '..';

describe('takeRight', () => {
    it('should throw exception if first argument is not a number', () => {
        // @ts-expect-error
        expect(() => takeRight('', [1, 2])).toThrow();
        // @ts-expect-error
        expect(() => takeRight('')([1, 2])).toThrow();
    });

    it('should throw exception if second argument is not an array', () => {
        // @ts-expect-error
        expect(() => takeRight(1, '')).toThrow();
        // @ts-expect-error
        expect(() => takeRight(1)('')).toThrow();
    });

    it('should return first n values', () => {
        expect(takeRight(3, [1, 2, 3, 4, 5])).toEqual([3, 4, 5]);
        expect(takeRight(3)([1, 2, 3, 4, 5])).toEqual([3, 4, 5]);

        expect(takeRight(3, [1])).toEqual([1]);
        expect(takeRight(3)([1])).toEqual([1]);

        expect(takeRight(3, [1])).toEqual([1]);
        expect(takeRight(3)([1])).toEqual([1]);

        expect(takeRight(0, [1])).toEqual([]);
        expect(takeRight(0)([1])).toEqual([]);
    });

    it('should have right type for return value', () => {
        (takeRight(3, [1, 2, 3, 4, 5]) as number[]);
        // @ts-expect-error
        (takeRight(3, [1, 2, 3, 4, 5]) as string[]);

        (takeRight(3)([1, 2, 3, 4, 5]) as number[]);
        // @ts-expect-error
        (takeRight(3)([1, 2, 3, 4, 5]) as string[]);
    });
});
