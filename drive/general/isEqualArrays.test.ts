import { isEqualArrays } from 'shared/helpers/isEqualArrays/isEqualArrays';

describe('isEqualArrays', function () {
    it('works with empty params', function () {
        expect(isEqualArrays([], [])).toBeTruthy();
        expect(isEqualArrays([''], [''])).toBeTruthy();
        expect(isEqualArrays([undefined], [undefined])).toBeTruthy();
        expect(isEqualArrays([undefined], [undefined])).toBeTruthy();
    });

    it('works with filled params', function () {
        expect(isEqualArrays(['a', 1, null], ['a', 1, null])).toBeTruthy();
        expect(isEqualArrays(['a', 2, null], ['a', 1, undefined])).toBeFalsy();
        expect(isEqualArrays(['a', 2, null], ['a', 2, 'null'])).toBeFalsy();
    });
});
