const { getItems } = require('../../../../src/client/components/utils/get-pager-items.ts');

describe('getPagerItems', () => {
    it('должен возвращать корректный набор страниц для пейджера #1', () => {
        assert.deepEqual(getItems(3, 9, 'prev', 'next'), [
            { val: 'prev', name: '←', disabled: false },
            { val: 1, name: '1', disabled: false },
            { val: 2, name: '2', disabled: false },
            { val: 3, name: '3', disabled: false },
            { val: 4, name: '4', disabled: false },
            { val: 5, name: '5', disabled: false },
            { val: 7, name: '…', disabled: false },
            { val: 9, name: '9', disabled: false },
            { val: 'next', name: '→', disabled: false },
        ]);
    });

    it('должен возвращать корректный набор страниц для пейджера #2', () => {
        assert.deepEqual(getItems(1, 9, 'prev', 'next'), [
            { val: 'prev', name: '←', disabled: true },
            { val: 1, name: '1', disabled: false },
            { val: 2, name: '2', disabled: false },
            { val: 3, name: '3', disabled: false },
            { val: 4, name: '4', disabled: false },
            { val: 5, name: '5', disabled: false },
            { val: 7, name: '…', disabled: false },
            { val: 9, name: '9', disabled: false },
            { val: 'next', name: '→', disabled: false },
        ]);
    });

    it('должен возвращать корректный набор страниц для пейджера #3', () => {
        assert.deepEqual(getItems(5, 9, 'prev', 'next'), [
            { val: 'prev', name: '←', disabled: false },
            { val: 1, name: '1', disabled: false },
            { val: 2, name: '2', disabled: false },
            { val: 3, name: '3', disabled: false },
            { val: 4, name: '4', disabled: false },
            { val: 5, name: '5', disabled: false },
            { val: 6, name: '6', disabled: false },
            { val: 7, name: '7', disabled: false },
            { val: 8, name: '8', disabled: false },
            { val: 9, name: '9', disabled: false },
            { val: 'next', name: '→', disabled: false },
        ]);
    });

    it('должен возвращать корректный набор страниц для пейджера #4', () => {
        assert.deepEqual(getItems(10, 20, 'prev', 'next'), [
            { val: 'prev', name: '←', disabled: false },
            { val: 1, name: '1', disabled: false },
            { val: 4, name: '…', disabled: false },
            { val: 8, name: '8', disabled: false },
            { val: 9, name: '9', disabled: false },
            { val: 10, name: '10', disabled: false },
            { val: 11, name: '11', disabled: false },
            { val: 12, name: '12', disabled: false },
            { val: 13, name: '13', disabled: false },
            { val: 16, name: '…', disabled: false },
            { val: 20, name: '20', disabled: false },
            { val: 'next', name: '→', disabled: false },
        ]);
    });
});
