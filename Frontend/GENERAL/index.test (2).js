const { assert } = require('chai');

const {
    isUndefined,
    isBoolean,
    isNumber,
    isString,
    isFunction,
    isObject,
} = require('./index');

describe('vh-sandbox-utils', function() {
    describe('isUndefined()', function() {
        it('должен вернуть false, если аргумент не равен undefined', function() {
            assert.isFalse(isUndefined(null));
        });

        it('должен вернуть true, если аргумент равен undefined', function() {
            assert.isTrue(isUndefined(undefined));
        });
    });

    describe('isBoolean()', function() {
        it('должен вернуть false, если аргумент не является булевым', function() {
            assert.isFalse(isBoolean(0));
        });

        it('должен вернуть true, если агрумент является булевым', function() {
            assert.isTrue(isBoolean(false));
        });
    });

    describe('isNumber()', function() {
        it('должен вернуть false, если аргумент не является числом', function() {
            assert.isFalse(isNumber('123'));
        });

        it('должен вернуть false, если аргумент имеет значение NaN', function() {
            assert.isFalse(isNumber(NaN));
        });

        it('должен вернуть true, если аргумент является числом', function() {
            assert.isTrue(isNumber(5));
        });

        it('должен вернуть true, если аргумент имеет значение Infinity', function() {
            assert.isTrue(isNumber(Infinity));
        });
    });

    describe('isString()', function() {
        it('должен вернуть false, если аргумент не является строкой', function() {
            assert.isFalse(isString(null));
        });

        it('должен вернуть true, если агрумент является строкой', function() {
            assert.isTrue(isString('str'));
        });
    });

    describe('isFunction()', function() {
        it('должен вернуть false, если аргумент не является функцией', function() {
            assert.isFalse(isFunction({}));
        });

        it('должен вернуть true, если агрумент является функцией', function() {
            assert.isTrue(isFunction(jest.fn()));
        });
    });

    describe('isObject()', function() {
        it('должен вернуть false, если агрумент не является объектом', function() {
            assert.isFalse(isObject(123));
        });

        it('должен вернуть false, если аргумент равен null', function() {
            assert.isFalse(isObject(null));
        });

        it('должен вернуть true, если аргумент является объектом', function() {
            assert.isTrue(isObject({}));
        });
    });
});
