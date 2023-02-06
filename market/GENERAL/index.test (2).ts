import {isEqualNumbers, toCurrencyRub} from '.';

describe('toCurrencyRub', () => {
    test.each`
        input           | output
        ${0}            | ${'0,00\u00a0₽'}
        ${0.1}          | ${'0,10\u00a0₽'}
        ${0.01}         | ${'0,01\u00a0₽'}
        ${0.001}        | ${'0,00\u00a0₽'}
        ${0.0000001}    | ${'0,00\u00a0₽'}
        ${0.004}        | ${'0,00\u00a0₽'}
        ${0.005}        | ${'0,01\u00a0₽'}
        ${0.009}        | ${'0,01\u00a0₽'}
        ${1}            | ${'1,00\u00a0₽'}
        ${1.1}          | ${'1,10\u00a0₽'}
        ${1.01}         | ${'1,01\u00a0₽'}
        ${1.001}        | ${'1,00\u00a0₽'}
        ${1.000001}     | ${'1,00\u00a0₽'}
        ${1.004}        | ${'1,00\u00a0₽'}
        ${1.005}        | ${'1,01\u00a0₽'}
        ${1.99}         | ${'1,99\u00a0₽'}
        ${1.99999}      | ${'2,00\u00a0₽'}
        ${1000}         | ${'1\u00a0000,00\u00a0₽'}
        ${10000}        | ${'10\u00a0000,00\u00a0₽'}
        ${100000}       | ${'100\u00a0000,00\u00a0₽'}
        ${1000000}      | ${'1\u00a0000\u00a0000,00\u00a0₽'}
        ${123456}       | ${'123\u00a0456,00\u00a0₽'}
        ${123456123}    | ${'123\u00a0456\u00a0123,00\u00a0₽'}
        ${123456123.12} | ${'123\u00a0456\u00a0123,12\u00a0₽'}
    `('format %d to %s', ({input, output}) => {
        expect(toCurrencyRub(input)).toBe(output);
    });
});

describe('isEqualNumbers', () => {
    it('should false if sign different', () => {
        const actual = isEqualNumbers(-1, 1);

        expect(actual).toBeFalsy();
    });

    it('should true if numbers are equal', () => {
        const result1 = isEqualNumbers(1, 1);
        const result2 = isEqualNumbers(0, 0);
        const result3 = isEqualNumbers(-1, -1);

        expect([result1, result2, result3]).toEqual([true, true, true]);
    });

    it('should true if float numbers are equal', () => {
        const result1 = isEqualNumbers(0.1, 0.1);
        const result2 = isEqualNumbers(-0.1, -0.1);

        expect([result1, result2]).toEqual([true, true]);
    });

    it('should true if equals numbers with float inaccuracy', () => {
        const actual = isEqualNumbers(0.2, 10.3 - 10.1);

        expect(actual).toBe(true);
    });
});
