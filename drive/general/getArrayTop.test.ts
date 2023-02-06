/* eslint-disable @typescript-eslint/no-magic-numbers */
import { getArrayTop } from 'shared/helpers/getArrayTop/getArrayTop';

describe('getArrayTop', () => {
    it('should get top', () => {
        expect(getArrayTop([1, 2, 3, 4, 5], 2)).toEqual([1, 2]);
    });

    it('should get bottom', () => {
        expect(getArrayTop([1, 2, 3, 4, 5], 2, true)).toEqual([5, 4]);
    });
});
