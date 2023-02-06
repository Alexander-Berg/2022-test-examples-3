const { hasDups } = require('../../../../src/client/components/utils/utils');

describe('components/utils/utils', () => {
    describe('hasDups', () => {
        it('должен возвращать true, если в массиве есть дубликаты', () => {
            assert.isTrue(hasDups([1, 2, 3, 4, 5, 2]));
            assert.isTrue(hasDups(['1', '2', '3', '4', '1', '5']));
        });

        it('должен возвращать false, если в массиве нет дубликатов', () => {
            assert.isFalse(hasDups([1, 2, 3, 4, 5]));
            assert.isFalse(hasDups(['1', '2', '3', '4', '5']));
        });
    });
});
