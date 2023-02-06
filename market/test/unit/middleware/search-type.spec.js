'use strict';

const searchType = require('./../../../middleware/search-type');

describe('search type', () => {
    test('should return correct type', () => {
        const dictionary = {
            avia: 'avia',
            isbn: 'market',
            url: 'market',
            text: 'market',
            model: 'market',
            'filter-category': 'market',
            redirect_filters: 'market',
            'search-result-by-text': 'market'
        };

        Object.keys(dictionary).forEach((key) => {
            expect(searchType.whatTypeIsIt(key)).toBe(dictionary[key]);
        });
    });

    test('should return null if search type is not correct', () => {
        const notCorrectSearchTypes = ['meh', () => ',', 'whatever'];

        notCorrectSearchTypes.forEach((key) => {
            expect(searchType.whatTypeIsIt(key)).toBeNull();
        });
    });
});
