'use strict';

const formatPrice = require('../../../utils/format-price');

describe('formatPrice', () => {
    it('should format number properly', () => {
        expect(formatPrice(9999)).toBe('9 999');
        expect(formatPrice(1146)).toBe('1 146');
        expect(formatPrice(11567)).toBe('11 567');
        expect(formatPrice(134655)).toBe('134 655');
        expect(formatPrice(1888888)).toBe('1 888 888');
        expect(formatPrice(9999.99)).toBe('9 999.99');
    });

    it('should format string properly', () => {
        expect(formatPrice('9999')).toBe('9 999');
        expect(formatPrice('1146')).toBe('1 146');
        expect(formatPrice('11567')).toBe('11 567');
        expect(formatPrice('134655')).toBe('134 655');
        expect(formatPrice('1888888')).toBe('1 888 888');
        expect(formatPrice('9999.99')).toBe('9 999.99');
    });
});
