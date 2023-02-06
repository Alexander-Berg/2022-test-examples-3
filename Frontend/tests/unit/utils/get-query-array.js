const getQueryArray = require('../../../src/server/utils/get-query-array');

describe('utils/getQueryArray', () => {
    it('если свойства с заданным именем нет в заданному объекте, то должна вернуть null', () => {
        assert.equal(getQueryArray('param', { a: 1, b: 2 }), null);
    });

    it('если значение искомого свойства в переданном объектe является массивом, то должна вернуть его без изменений', () => {
        const value = [1, 2];
        assert.deepEqual(getQueryArray('param', { param: value, b: 2 }), value);
    });

    it('если значение искомого свойства в переданном объектe не является массивом, то должна обернуть его в массив', () => {
        assert.deepEqual(getQueryArray('param', { param: 1, b: 2 }), [1]);
    });
});
