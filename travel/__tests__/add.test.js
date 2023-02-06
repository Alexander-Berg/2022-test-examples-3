import {addItemToSearchHistory} from '../searchHistoryManager';
import {findItem, createItem} from '../utils';
import validate from '../validate';

jest.mock('../utils');
jest.mock('../validate');

describe('searchHistory', () => {
    describe('addItemToSearchHistory', () => {
        const value = 'added value';

        it('add invalid', () => {
            const items = ['item1', 'item2'];

            validate.mockReturnValue(false);

            const newItems = addItemToSearchHistory(items, value);

            expect(newItems).toBe(items);
            expect(validate).toBeCalledWith(value);
        });

        it('add new', () => {
            const item1 = 'item1';
            const item2 = 'item2';
            const items = [item1, item2];

            validate.mockReturnValue(true);
            findItem.mockReturnValue({
                index: -1,
                item: null,
            });

            const newItem = 'new item';

            createItem.mockReturnValue(newItem);

            const newItems = addItemToSearchHistory(items, value);

            expect(newItems).not.toBe(items);
            expect(newItems).toEqual([newItem, item1, item2]);

            expect(validate).toBeCalledWith(value);
            expect(findItem).toBeCalledWith(items, value);
        });

        it('add duplicate', () => {
            const item1 = 'item1';

            const item2 = {
                value,
                favorite: false,
                priority: 1,
            };

            const items = [item1, item2];

            validate.mockReturnValue(true);
            findItem.mockReturnValue({item: item2, index: 1});

            const newItems = addItemToSearchHistory(items, value);

            expect(newItems).not.toBe(items);
            expect(newItems).toEqual([
                item1,
                {
                    value,
                    favorite: false,
                    priority: 2,
                },
            ]);

            expect(validate).toBeCalledWith(value);
            expect(findItem).toBeCalledWith(items, value);
        });
    });
});
