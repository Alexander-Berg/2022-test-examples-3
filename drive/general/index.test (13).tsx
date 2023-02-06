/*eslint-disable no-magic-numbers*/
import { isObject, isValueExist, removeNullValues } from './utils';

describe('FormConstructor utils', () => {

    describe('Check if value is Object', () => {

        it('Check true boolean value', () => {
            expect(isObject(true)).toEqual(false);
        });

        it('Check false boolean value', () => {
            expect(isObject(false)).toEqual(false);
        });

        it('Check null value', () => {
            expect(isObject(null)).toEqual(false);
        });

        it('Check undefined value', () => {
            expect(isObject(undefined)).toEqual(false);
        });

        it('Check zero number value', () => {
            expect(isObject(0)).toEqual(false);
        });

        it('Check positive integer number value', () => {
            expect(isObject(1)).toEqual(false);
        });

        it('Check negative integer number value', () => {
            expect(isObject(-1)).toEqual(false);
        });

        it('Check positive float number value', () => {
            expect(isObject(1.23)).toEqual(false);
        });

        it('Check negative float number value', () => {
            expect(isObject(-1.23)).toEqual(false);
        });

        it('Check positive Infinity number value', () => {
            expect(isObject(Infinity)).toEqual(false);
        });

        it('Check negative Infinity number value', () => {
            expect(isObject(-Infinity)).toEqual(false);
        });

        it('Check NaN value', () => {
            expect(isObject(NaN)).toEqual(false);
        });

        it('Check empty string value', () => {
            expect(isObject('')).toEqual(false);
        });

        it('Check string value', () => {
            expect(isObject('string')).toEqual(false);
        });

        it('Check Symbol value', () => {
            expect(isObject(Symbol())).toEqual(false);
        });

        it('Check Function value', () => {
            expect(isObject(function () {
            })).toEqual(false);
        });

        it('Check Date value', () => {
            expect(isObject(new Date())).toEqual(false);
        });

        it('Check empty Array value', () => {
            expect(isObject([])).toEqual(false);
        });

        it('Check empty Array value', () => {
            expect(isObject([1, 1, 1])).toEqual(false);
        });

        it('Check RegExp value', () => {
            expect(isObject(/s/)).toEqual(false);
        });

        it('Check empty Object value', () => {
            expect(isObject({})).toEqual(true);
        });

        it('Check Object value', () => {
            expect(isObject({ a: 1 })).toEqual(true);
        });

    });

    describe('Remove null values in object', () => {

        it('Remove null value from empty object', () => {
            expect(removeNullValues({})).toEqual({});
        });

        it('Remove null value from simple object', () => {
            expect(removeNullValues({ a: null })).toEqual({});
        });

        it('Don\'t remove null value from object with true value', () => {
            const TEST_OBJECT = {
                trueValue: true,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with false value', () => {
            const TEST_OBJECT = {
                falseValue: false,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with undefined value', () => {
            const TEST_OBJECT = {
                undefinedValue: undefined,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with zero value', () => {
            const TEST_OBJECT = {
                zeroValue: 0,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with negative number value', () => {
            const TEST_OBJECT = {
                negativeNumberValue: -1,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with positive number value', () => {
            const TEST_OBJECT = {
                negativeNumberValue: 1,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with NaN value', () => {
            const TEST_OBJECT = {
                NaNValue: NaN,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with empty string value', () => {
            const TEST_OBJECT = {
                emptyStringValue: '',
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with string value', () => {
            const TEST_OBJECT = {
                stringValue: 'string',
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with empty array value', () => {
            const TEST_OBJECT = {
                emptyArrayValue: [],
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with array value', () => {
            const TEST_OBJECT = {
                emptyArrayValue: [1, 1, 1],
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with empty object value', () => {
            const TEST_OBJECT = {
                emptyObjectValue: {},
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with object value', () => {
            const TEST_OBJECT = {
                objectValue: { a: 1 },
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with symbol value', () => {
            const symbol = Symbol();

            const TEST_OBJECT = {
                symbolValue: symbol,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Don\'t remove null value from object with function value', () => {
            const func = function () {
            };

            const TEST_OBJECT = {
                functionValue: func,
            };

            expect(removeNullValues(TEST_OBJECT)).toEqual(TEST_OBJECT);
        });

        it('Remove null value from nested object', () => {
            expect(removeNullValues({ a: { b: { c: null } } })).toEqual({ a: { b: {} } });
        });

        it('Remove null value from array', () => {
            expect(removeNullValues({ a: ['a', null, 'b', null, 'c'] })).toEqual({ a: ['a', 'b', 'c'] });
        });
    });

    describe('Check if value exist', () => {

        it('Check true boolean value', () => {
            expect(isValueExist(true)).toEqual(true);
        });

        it('Check false boolean value', () => {
            expect(isValueExist(false)).toEqual(true);
        });

        it('Check null value', () => {
            expect(isValueExist(null)).toEqual(false);
        });

        it('Check undefined value', () => {
            expect(isValueExist(undefined)).toEqual(false);
        });

        it('Check zero number value', () => {
            expect(isValueExist(0)).toEqual(true);
        });

        it('Check positive integer number value', () => {
            expect(isValueExist(1)).toEqual(true);
        });

        it('Check negative integer number value', () => {
            expect(isValueExist(-1)).toEqual(true);
        });

        it('Check positive float number value', () => {
            expect(isValueExist(1.23)).toEqual(true);
        });

        it('Check negative float number value', () => {
            expect(isValueExist(-1.23)).toEqual(true);
        });

        it('Check positive Infinity number value', () => {
            expect(isValueExist(Infinity)).toEqual(true);
        });

        it('Check negative Infinity number value', () => {
            expect(isValueExist(-Infinity)).toEqual(true);
        });

        it('Check NaN value', () => {
            expect(isValueExist(NaN)).toEqual(false);
        });

        it('Check empty string value', () => {
            expect(isValueExist('')).toEqual(false);
        });

        it('Check string value', () => {
            expect(isValueExist('string')).toEqual(true);
        });

        it('Check Symbol value', () => {
            expect(isValueExist(Symbol())).toEqual(true);
        });

        it('Check Function value', () => {
            expect(isValueExist(function () {
            })).toEqual(true);
        });

        it('Check zero Date value', () => {
            expect(isValueExist(new Date())).toEqual(true);
        });

        it('Check Date value', () => {
            expect(isValueExist(new Date())).toEqual(true);
        });

        it('Check empty Array value', () => {
            expect(isValueExist([])).toEqual(false);
        });

        it('Check empty Array value', () => {
            expect(isValueExist([1, 1, 1])).toEqual(true);
        });

        it('Check RegExp value', () => {
            expect(isValueExist(/s/)).toEqual(true);
        });

        it('Check empty Object value', () => {
            expect(isValueExist({})).toEqual(false);
        });

        it('Check Object value', () => {
            expect(isValueExist({ a: 1 })).toEqual(true);
        });
    });
});
