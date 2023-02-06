const findItem = require.requireActual('../utils').findItem;

const value = {
    from: {key: 1, title: 'abc'},
    to: {key: 2, title: 'qwerty'},
};

describe('searchHistory/utils', () => {
    describe('findItem', () => {
        it('no items', () => {
            const items = [];

            expect(findItem(items, value)).toEqual({
                index: -1,
                item: null,
            });
        });

        it('single item', () => {
            const item = {
                value,
                priority: 0,
                favorite: false,
            };

            const items = [item];

            expect(findItem(items, value)).toEqual({
                index: 0,
                item,
            });
        });

        it('single item, different titles', () => {
            const item = {
                value: {
                    from: {key: 1, title: 'abc222'},
                    to: {key: 2, title: 'qwerty222'},
                },
                priority: 0,
                favorite: false,
            };

            const items = [item];

            expect(findItem(items, value)).toEqual({
                index: 0,
                item,
            });
        });

        it('single item, different keys', () => {
            const item = {
                value: {
                    from: {key: 11, title: 'abc'},
                    to: {key: 22, title: 'qwerty'},
                },
                priority: 0,
                favorite: false,
            };
            const items = [item];

            expect(findItem(items, value)).toEqual({
                index: -1,
                item: null,
            });
        });

        it('two items', () => {
            const item1 = {
                value: {
                    from: {key: 11},
                    to: {key: 22},
                },
                priority: 0,
                favorite: false,
            };

            const item2 = {
                value: {
                    from: {key: 1},
                    to: {key: 2},
                },
                priority: 0,
                favorite: false,
            };

            const items = [item1, item2];

            expect(findItem(items, value)).toEqual({
                index: 1,
                item: item2,
            });
        });
    });
});
