import {initializeSearchHistoryItems} from '../searchHistoryManager';
import validate from '../validate';

jest.mock('../validate');

describe('searchHistory', () => {
    it('initializeSearchHistoryItems', () => {
        const favoriteItem1 = {
            value: {from: {key: 1}, to: {key: 2}},
            priority: 1,
            favorite: true,
        };

        const favoriteItem2 = {
            value: {from: {key: 3}, to: {key: 4}},
            priority: 2,
            favorite: true,
        };

        const unfavoriteItem1 = {
            value: {from: {key: 5}, to: {key: 6}},
            priority: 3,
            favorite: false,
        };

        const unfavoriteItem2 = {
            value: {from: {key: 7}, to: {key: 8}},
            priority: 4,
            favorite: false,
        };

        const invalidItem = {
            value: 'invalid',
        };

        const items = [
            invalidItem,
            unfavoriteItem1,
            favoriteItem1,
            unfavoriteItem2,
            favoriteItem2,
        ];

        validate.mockImplementation(value => value !== invalidItem.value);

        const newItems = initializeSearchHistoryItems(items);

        expect(newItems).toEqual([
            favoriteItem2,
            favoriteItem1,
            unfavoriteItem2,
            unfavoriteItem1,
        ]);
    });
});
