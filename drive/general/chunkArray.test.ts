import { chunkArray } from 'shared/helpers/chunkArray/chunkArray';

/*eslint-disable @typescript-eslint/no-magic-numbers*/

describe('chunkArray', function () {
    it('works with empty array', function () {
        expect(chunkArray([], 2)).toEqual([]);
    });

    it('works with filled array', function () {
        expect(chunkArray([1, 2], 2)).toEqual([[1, 2]]);
        expect(chunkArray([1, 2, 3], 2)).toEqual([[1, 2], [3]]);
        expect(chunkArray([1, 2, 3, 4], 2)).toEqual([
            [1, 2],
            [3, 4],
        ]);
    });
});
