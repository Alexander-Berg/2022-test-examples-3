import { filterNonUnique } from '../../../../src/helper/array-filters';

describe('Filter all values appearing more that once', () => {
    const testCases = [
        {
            inputList: [1, 2, 3, 4, 5],
            expected: [1, 2, 3, 4, 5],
        },
        {
            inputList: [1, 2, 2, 3, 4, 5, 5, 5],
            expected: [1, 3, 4],
        },
        {
            inputList: [],
            expected: [],
        },
        {
            inputList: [1, 1, 1],
            expected: [],
        },
    ];

    testCases.forEach(({ inputList, expected }) => {
        test(`Filtering list: [${inputList}] => ${expected}`, () => {
            expect(filterNonUnique(inputList)).toStrictEqual(expected);
        });
    });
});
