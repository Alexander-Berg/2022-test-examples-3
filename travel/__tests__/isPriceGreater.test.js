const isPriceGreater = require.requireActual('../isPriceGreater').default;

describe('isPriceGreater', () => {
    it('should return true if first price is greater and false otherwise', () => {
        const priceA = {value: 2000, currency: 'RUR'};
        const priceB = {value: 1000, currency: 'RUR'};

        expect(isPriceGreater(priceA, priceB)).toBe(true);
        expect(isPriceGreater(priceB, priceA)).toBe(false);
    });

    it('should return false if prices are equal', () => {
        const price = {value: 1000, currency: 'RUR'};

        expect(isPriceGreater(price, price)).toBe(false);
    });
});
