'use strict';

const blacklistCategories = require('./../../../yandex-content-api/blacklist-categories');

describe('blacklist categories', () => {
    describe('isBook function', () => {
        test('should correct handle expected results', () => {
            const expectedResults = require('./books-categories-expected.json');
            expectedResults.forEach((expectedResult) => {
                for (const key in expectedResult) {
                    if (expectedResult.hasOwnProperty(key) && key !== 'meta') {
                        const result = blacklistCategories.isBook(key);

                        expect(result).toBe(expectedResult[key]);
                    }
                }
            });
        });
    });

    describe('isClothesCategory function', () => {
        test('should correct handle expected results', () => {
            const expectedResults = require('./clothes-categories-expected.json');
            expectedResults.forEach((expectedResult) => {
                for (const key in expectedResult) {
                    if (expectedResult.hasOwnProperty(key) && key !== 'meta') {
                        const result = blacklistCategories.isClothesCategory(key);

                        expect(result).toBe(expectedResult[key]);
                    }
                }
            });
        });
    });

    describe('isBlacklist function', () => {
        test('should correct handle expected results', () => {
            const expectedResults = require('./blacklisted-categories-expected.json');
            expectedResults.forEach((expectedResult) => {
                for (const key in expectedResult) {
                    if (expectedResult.hasOwnProperty(key)) {
                        const result = blacklistCategories.isBlacklisted(key, 'text');

                        expect(result).toBe(expectedResult[key]);
                    }
                }
            });
        });

        test('should correct handle 90401', () => {
            expect(blacklistCategories.isBlacklisted(90401)).toBeTruthy();
        });

        test('should correct handle category with parentMarketId 90401', () => {
            expect(blacklistCategories.isBlacklisted(91282)).toBeFalsy();
        });
    });
});
