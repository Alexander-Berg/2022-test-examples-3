import { formatNumber } from './stringUtils';

describe('formatNumber', function() {
    it('should format number', function() {
        expect(formatNumber(12345678)).toBe('12 345 678');
        expect(formatNumber(NaN)).toBe('NaN');
        expect(formatNumber(Infinity)).toBe('Infinity');
    });
});
