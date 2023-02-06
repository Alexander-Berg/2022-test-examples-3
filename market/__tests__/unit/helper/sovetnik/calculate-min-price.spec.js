'use strict';

const calculateMinPrice = require('../../../../src/helper/calculate-min-price');

describe('helper / sovetnik', () => {
    describe('calculation of minimum price', () => {
        test('should return zero if basic price is zero too', () => {
            const actual = calculateMinPrice(0);
            const expected = 0;

            expect(typeof actual).toBe('number');
            expect(actual).toBe(expected);
        });

        test('should return correct result if type of basic price is \'string\'', () => {
            const actual = calculateMinPrice('44');
            const expected = 22;

            expect(typeof actual).toBe('number');
            expect(actual).toBe(expected);
        });
    });
});
