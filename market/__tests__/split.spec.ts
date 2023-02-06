/* eslint-disable no-unused-expressions */

import {InvariantViolation} from '@yandex-market/invariant';

import {split} from '..';

describe('split', () => {
    it('should split string by given delimeter', () => {
        expect(split(',', 'a,b,c')).toEqual(['a', 'b', 'c']);
        expect(split(',')('a,b,c')).toEqual(['a', 'b', 'c']);
    });

    it('should be properly typed', () => {
        (split(',', 'a,b,c') as string[]);
        // @ts-expect-error
        (split(',', 'a,b,c') as number[]);

        (split(',')('a,b,c') as string[]);
        // @ts-expect-error
        (split(',')('a,b,c') as number[]);
    });

    it('should throw exception if delimiter is not a string', () => {
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => split(null, 'a,b,c')).toThrow(InvariantViolation);
    });

    it('should throw exception if source is not a string', () => {
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => split('a,b,c', null)).toThrow(InvariantViolation);
    });
});
