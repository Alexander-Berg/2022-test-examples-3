import { ensureArray, filterEmpty, getUrlWithReplacedQuery } from './queryHelper';

describe('queryHelper', function() {
    describe('ensureArray', function() {
        it('should convert given fields into arrays', function() {
            const sourceQuery = {
                array: [1, 2],
                shouldBeArray: 3,
                notArray: 4,
            };

            const resultQuery = {
                array: [1, 2],
                shouldBeArray: [3],
                notArray: 4,
            };

            expect(ensureArray(sourceQuery, ['array', 'shouldBeArray'])).toEqual(resultQuery);
        });
    });
    describe('filterEmpty', function() {
        it('should filter empty value', function() {
            const sourceQuery = {
                emptyOne: '',
                emptyTwo: [],
                emptyThree: undefined,
                notEmptyOne: 0,
                notEmptyTwo: null,
                notEmptyThree: [1],
            };

            const resultQuery = {
                notEmptyOne: 0,
                notEmptyTwo: null,
                notEmptyThree: [1],
            };

            expect(filterEmpty(sourceQuery)).toEqual(resultQuery);
        });
    });
    describe('getUrlWithReplacedQuery', function() {
        it('should replace query in url', function() {
            const sourceUrl = 'https://one.day/?one=1&two=2&three=3';
            const resultUrl = 'https://one.day/?one=11&two=22&three=3';
            const query = {
                one: '11',
                two: '22',
            };

            expect(getUrlWithReplacedQuery(sourceUrl, query)).toEqual(resultUrl);
        });
        it('should delete undefined values', function() {
            const sourceUrl = 'https://one.day/?one=1&two=2&three=3';
            const resultUrl = 'https://one.day/?two=2&three=3';
            const query = {
                one: undefined,
            };

            expect(getUrlWithReplacedQuery(sourceUrl, query)).toEqual(resultUrl);
        });
    });
});
