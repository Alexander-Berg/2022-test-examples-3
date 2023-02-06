/* eslint-disable no-unused-expressions */

import {times} from '..';

describe('times', () => {
    it('should throw exception if first argument is not a function', () => {
        // @ts-expect-error
        expect(() => times('', 5)).toThrow();
        // @ts-expect-error
        expect(() => times('')(5)).toThrow();
    });

    it('should throw exception if second argument is not a number', () => {
        // @ts-expect-error
        expect(() => times(() => {}, '')).toThrow();
        // @ts-expect-error
        expect(() => times(() => {})('')).toThrow();
    });

    it('should return array of iteratee results', () => {
        expect(times(String, 5)).toEqual(['0', '1', '2', '3', '4']);
        expect(times(String)(5)).toEqual(['0', '1', '2', '3', '4']);
    });

    it('should have right type for return value', () => {
        const iteratee = (index: number) => {
            (index as number);
            // @ts-expect-error
            (index as string);

            return String(index * 2);
        };

        const res = times(iteratee, 3);

        (res as string[]);
        // @ts-expect-error
        (res as number[]);
        // @ts-expect-error
        (res as boolean[]);

        const resCurried = times(iteratee)(3);

        (resCurried as string[]);
        // @ts-expect-error
        (resCurried as number[]);
        // @ts-expect-error
        (resCurried as boolean[]);
    });
});
