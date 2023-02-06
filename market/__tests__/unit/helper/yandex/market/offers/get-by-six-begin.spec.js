'use strict';

const getBySixBegin = require('../../../../../../src/helper/get-by-six-begin');

const defaultOffers = [
    {
        price: {
            value: 1000
        }
    },
    {
        price: {
            value: 5000
        }
    },
    {
        price: {
            value: 8000
        }
    }
];

const defaultMaxPrice = 1700;
const defaultOffersLimit = 6;

describe('helper / getBySixBegin', () => {
    it('should return null if offers array has no values', () => {
        expect(getBySixBegin([], defaultMaxPrice, defaultOffersLimit)).toBeNull();
    });

    it('should return null if \'maxPrice\' is undefined or not a number', () => {
        expect(getBySixBegin(defaultOffers, undefined, defaultOffersLimit)).toBeNull();
    });

    it('should return correct value if \'offersLimit\' is omitted', () => {
        const expectedBeginValue = 0;
        expect(getBySixBegin(defaultOffers, defaultMaxPrice)).toBe(expectedBeginValue);
    });

    it('should return value of type \'number\' if all parameters correct', () => {
        // FIXME: after adding `jest`
        const returnType = typeof getBySixBegin(defaultOffers, defaultMaxPrice, defaultOffersLimit);
        const expectedReturnType = typeof 1;
        expect(returnType).toBe(expectedReturnType);
    });
});
