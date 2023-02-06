'use strict';

const sinon = require('sinon');

const cache = require('../../../../../../src/helper/cache');
const Category = require('../../../../../../src/helper/Category');

describe('helper / yandex / market / category', function() {
    let consoleErrorStub;
    let cacheStub;
    let category;

    beforeAll(() => {
        consoleErrorStub = sinon.stub(console, 'error', () => undefined);
    });

    afterAll(() => {
        consoleErrorStub.restore();
    });

    beforeEach(() => {
        const _cache = {
            categories: {},
            xmlToMarketId: {},
        };

        // It is necessary to avoid cache filling
        cacheStub = {
            categories: {
                add: sinon.stub(cache.categories, 'add', (category) => undefined),
                get: sinon.stub(cache.categories, 'get', (marketId) => _cache.categories[marketId]),
            },
            xmlToMarketId: {
                add: sinon.stub(cache.xmlToMarketId, 'add', (xmlId, marketId) => undefined),
                get: sinon.stub(cache.xmlToMarketId, 'get', (xmlId) => _cache.xmlToMarketId[xmlId]),
            },
        };

        category = require('../../../../../../src/helper/category-helpers');

        cacheStub.categories.add.restore();
        cacheStub.categories.add = sinon.stub(cache.categories, 'add', (category) => {
            _cache.categories[category.marketId] = category;
            _cache.xmlToMarketId[category.xmlId] = category.marketId;
        });
    });

    afterEach(() => {
        cacheStub.categories.add.restore();
        cacheStub.categories.get.restore();
        cacheStub.xmlToMarketId.add.restore();
        cacheStub.xmlToMarketId.get.restore();
    });

    describe('cache', () => {
        test('should be able to add new category to empty cache', () => {
            const tmp = new Category(1, 2, 0, 'category name');
            cache.categories.add(tmp);
            const actual = cache.categories.get(1);

            expect(actual).toBeDefined();
        });
    });

    describe('#getCategoryName', () => {
        test('should return undefined if category in defined in cache', () => {
            expect(category.getCategoryName(1)).toBeUndefined();
        });

        test('should return correct name of added category', () => {
            const expectedCategoryName = 'category name';
            const tmp = new Category(1, 2, 0, expectedCategoryName);
            cache.categories.add(tmp);
            const actual = category.getCategoryName(1);

            expect(actual).toBe(expectedCategoryName);
        });
    });

    describe('#getCategoryLevel', () => {
        test('should return undefined if category is not defined in cache', () => {
            expect(category.getCategoryLevel(1)).toBeUndefined();
        });

        test('should return correct levels of added categories', () => {
            cache.categories.add(new Category(11, 1, 0, '11')); //     0
            cache.categories.add(new Category(15, 5, 2, '15')); //    /|
            cache.categories.add(new Category(12, 2, 0, '12')); //   1 2
            cache.categories.add(new Category(14, 4, 1, '14')); //  /| |
            cache.categories.add(new Category(13, 3, 1, '13')); // 3 4 5
            cache.categories.add(new Category(10, 0, undefined, '10'));

            expect(category.getCategoryLevel(10)).toBe(0);
            expect(category.getCategoryLevel(11)).toBe(1);
            expect(category.getCategoryLevel(12)).toBe(1);
            expect(category.getCategoryLevel(13)).toBe(2);
            expect(category.getCategoryLevel(14)).toBe(2);
            expect(category.getCategoryLevel(15)).toBe(2);
        });
    });

    describe('#getPathToRootCategory', () => {
        test('should return empty path if category is root', () => {
            cache.categories.add(new Category(0, 0, undefined, 'root'));

            const actual = category.getPathToRootCategory(0);

            expect(actual).toHaveLength(0);
        });

        test('should return empty path if category has parent id but parent category does not exist', () => {
            cache.categories.add(new Category(1, 1, 0, 'first'));

            const actual = category.getPathToRootCategory(1);

            expect(actual).toHaveLength(0);
        });

        test('should return correct path to root category', () => {
            cache.categories.add(new Category(11, 1, 0, '11')); //     0
            cache.categories.add(new Category(15, 5, 2, '15')); //    /|
            cache.categories.add(new Category(12, 2, 0, '12')); //   1 2
            cache.categories.add(new Category(14, 4, 1, '14')); //  /| |
            cache.categories.add(new Category(13, 3, 1, '13')); // 3 4 5
            cache.categories.add(new Category(10, 0, undefined, '10'));

            const actual = category.getPathToRootCategory(14, true);

            expect(actual).toEqual([14, 11, 10]);
        });
    });

    describe('#hasAncestry', () => {
        test('should return true if category is a progenitor', () => {
            cache.categories.add(new Category(11, 1, 0, '11')); //     0
            cache.categories.add(new Category(15, 5, 2, '15')); //    /|
            cache.categories.add(new Category(12, 2, 0, '12')); //   1 2
            cache.categories.add(new Category(14, 4, 1, '14')); //  /| |
            cache.categories.add(new Category(13, 3, 1, '13')); // 3 4 5
            cache.categories.add(new Category(10, 0, undefined, '10'));

            expect(category.hasAncestry(10, [10])).toBeTruthy();
            expect(category.hasAncestry(10, [12])).toBeFalsy();
            expect(category.hasAncestry(11, [11])).toBeTruthy();
            expect(category.hasAncestry(11, [12])).toBeFalsy();
        });

        test('should return true if category has parent at one level higher', () => {
            cache.categories.add(new Category(11, 1, 0, '11')); //     0
            cache.categories.add(new Category(15, 5, 2, '15')); //    /|
            cache.categories.add(new Category(12, 2, 0, '12')); //   1 2
            cache.categories.add(new Category(14, 4, 1, '14')); //  /| |
            cache.categories.add(new Category(13, 3, 1, '13')); // 3 4 5
            cache.categories.add(new Category(10, 0, undefined, '10'));

            expect(category.hasAncestry(11, [10])).toBeTruthy();
            expect(category.hasAncestry(13, [11])).toBeTruthy();
            expect(category.hasAncestry(15, [12])).toBeTruthy();
        });

        test('should return true if category has parent at several level higher', () => {
            cache.categories.add(new Category(11, 1, 0, '11')); //     0
            cache.categories.add(new Category(15, 5, 2, '15')); //    /|
            cache.categories.add(new Category(12, 2, 0, '12')); //   1 2
            cache.categories.add(new Category(14, 4, 1, '14')); //  /| |
            cache.categories.add(new Category(13, 3, 1, '13')); // 3 4 5
            cache.categories.add(new Category(10, 0, undefined, '10'));

            expect(category.hasAncestry(13, [10])).toBeTruthy();
            expect(category.hasAncestry(14, [10])).toBeTruthy();
            expect(category.hasAncestry(15, [10])).toBeTruthy();
        });

        test("should return false if category hasn't at least one of ancestry", () => {
            cache.categories.add(new Category(11, 1, 0, '11')); //     0
            cache.categories.add(new Category(15, 5, 2, '15')); //    /|
            cache.categories.add(new Category(12, 2, 0, '12')); //   1 2
            cache.categories.add(new Category(14, 4, 1, '14')); //  /| |
            cache.categories.add(new Category(13, 3, 1, '13')); // 3 4 5
            cache.categories.add(new Category(10, 0, undefined, '10'));

            expect(category.hasAncestry(10, [])).toBeFalsy();
            expect(category.hasAncestry(11, [12])).toBeFalsy();
            expect(category.hasAncestry(13, [12])).toBeFalsy();
            expect(category.hasAncestry(15, [13])).toBeFalsy();
        });

        test('should return false if category is not define in cache', () => {
            expect(category.hasAncestry(10, [10])).toBeFalsy();
        });
    });

    describe('#sortCategoriesByLevel', () => {
        test('should return empty array of categories id', () => {
            const actual = category.sortCategoriesByLevel([]);

            expect(actual).toHaveLength(0);
        });

        test('should return array which contains one category id', () => {
            cache.categories.add(new Category(1, 2, 3, 'name'));

            const actual = category.sortCategoriesByLevel([1]);

            expect(actual).toEqual([1]);
        });

        test('should return array of categories id according to their levels', () => {
            cache.categories.add(new Category(11, 1, 0, '11')); //     0
            cache.categories.add(new Category(15, 5, 2, '15')); //    /|
            cache.categories.add(new Category(12, 2, 0, '12')); //   1 2
            cache.categories.add(new Category(14, 4, 1, '14')); //  /| |
            cache.categories.add(new Category(13, 3, 1, '13')); // 3 4 5
            cache.categories.add(new Category(10, 0, undefined, '10'));

            const actual = category.sortCategoriesByLevel([12, 10, 13, 14, 15, 11]);

            expect(actual).toEqual([10, 12, 11, 13, 14, 15]);
        });
    });
});
