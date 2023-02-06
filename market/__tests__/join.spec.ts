/* eslint-disable no-unused-expressions */

import {InvariantViolation} from '@yandex-market/invariant';

import {join} from '..';

describe('join', () => {
    it('should join string by given delimeter', () => {
        expect(join(',', ['a', 'b', 'c'])).toEqual('a,b,c');
        expect(join(',')(['a', 'b', 'c'])).toEqual('a,b,c');
    });

    it('should be properly typed', () => {
        (join(',', ['a', 'b', 'c']) as string);
        // @ts-expect-error
        (join(',', ['a', 'b', 'c']) as number);

        (join(',')(['a', 'b', 'c']) as string);
        // @ts-expect-error
        (join(',')(['a', 'b', 'c']) as number);
    });

    it('should throw exception if delimiter is not a string', () => {
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => join(null, ['a', 'b'])).toThrow(InvariantViolation);
    });

    it('should throw exception if source is not an array', () => {
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => join(',', null)).toThrow(InvariantViolation);
    });
});
