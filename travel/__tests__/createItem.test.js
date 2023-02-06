const createItem = require.requireActual('../utils').createItem;

const value = {
    from: {key: 1, title: 'abc'},
    to: {key: 2, title: 'qwerty'},
};

describe('searchHistory/utils', () => {
    it('createItem', () => {
        expect(createItem(value)).toEqual({
            value,
            priority: 0,
            favorite: false,
        });
    });
});
