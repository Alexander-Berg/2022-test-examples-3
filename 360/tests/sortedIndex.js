const format = require('../lib/sortedIndex');
const expect = require('expect');

describe('sortedIndex', () => {
    const runTest = (array, item, comparator, expectedIndex) => {
        const testName = `${item} should be placed at index ${expectedIndex}`;
        it(testName, () => {
            const index = format.sortedIndex(array, item, comparator);
            expect(index).toEqual(expectedIndex);
        });
    };

    runTest([1, 3], 2, (a, b) => a - b, 1);
    runTest([1, 2, 3, 7], 5, (a, b) => a - b, 3);
    runTest([3, 7], 1, (a, b) => a - b, 0);
    runTest([1, 2, 3, 5], 7, (a, b) => a - b, 4);
    runTest([], 4, (a, b) => a - b, 0);
    runTest([7], 5, (a, b) => a - b, 0);
    runTest([2, 2, 2], 2, (a, b) => a - b, 0);
    runTest([1, 2, 2, 2, 3], 2, (a, b) => a - b, 1);
    runTest([1, 2, 3], -1, (a, b) => a - b, 0);
});

describe('sortedFind', () => {
    const runTest = (array, item, comparator, expectedIndex) => {
        const testName = `${item} should be found at index ${expectedIndex}`;
        it(testName, () => {
            const index = format.sortedFind(array, item, comparator);
            expect(index).toEqual(expectedIndex);
        });
    };

    runTest([], 1, (a, b) => a - b, -1);
    runTest([1, 3], 2, (a, b) => a - b, -1);
    runTest([1, 2, 2, 2, 3], 2, (a, b) => a - b, 1);
    runTest([1, 2, 2, 2, 3], 3, (a, b) => a - b, 4);
    runTest([3, 2, 1], 3, (a, b) => b - a, 0);
    runTest([3, 3, 3, 2, 1], 3, (a, b) => b - a, 0);
    runTest([3, 3, 3, 2, 1], 2, (a, b) => b - a, 3);
});
