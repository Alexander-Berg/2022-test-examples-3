import {favoriteSearchHistoryItem} from '../searchHistoryManager';
import {findItem} from '../utils';

jest.mock('../utils');

const value = {
    from: {key: 1},
    to: {key: 2},
};

describe('searchHistory', () => {
    describe('favoriteSearchHistoryItem', () => {
        it('not found', () => {
            findItem.mockReturnValue({
                index: -1,
                item: null,
            });

            const items = ['item1', 'item2'];

            const newItems = favoriteSearchHistoryItem(items, value, true);

            expect(newItems).toBe(items);
            expect(findItem).toBeCalledWith(items, value);
        });

        it('make favorite', () => {
            const item1 = 'item1';
            const item2 = {
                value,
                favorite: false,
                priority: 0,
            };
            const items = [item1, item2];

            findItem.mockReturnValue({
                index: 1,
                item: item2,
            });

            const newItems = favoriteSearchHistoryItem(items, value, true);

            expect(newItems).not.toBe(items);
            expect(newItems).toEqual([
                item1,
                {
                    value,
                    favorite: true,
                    priority: 0,
                },
            ]);

            expect(findItem).toBeCalledWith(items, value);
        });

        it('make unfavorite', () => {
            const item1 = {
                value,
                favorite: true,
                priority: 0,
            };
            const item2 = 'item2';

            const items = [item1, item2];

            findItem.mockReturnValue({
                index: 0,
                item: item1,
            });

            const newItems = favoriteSearchHistoryItem(items, value, false);

            expect(newItems).not.toBe(items);
            expect(newItems).toEqual([
                {
                    value,
                    favorite: false,
                    priority: 0,
                },
                item2,
            ]);

            expect(findItem).toBeCalledWith(items, value);
        });
    });
});
