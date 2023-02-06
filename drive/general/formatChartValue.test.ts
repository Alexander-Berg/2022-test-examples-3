import { formatChartValue } from 'shared/helpers/formatChartValue/formatChartValue';

describe('formatChartValue', function () {
    it('works with empty params', function () {
        expect(formatChartValue({ value: 0 })).toBe('0');
    });

    it('works with prefix params', function () {
        expect(formatChartValue({ value: 9000, prefix: '$' })).toBe('$ 9,000');
    });

    it('works with postfix params', function () {
        expect(formatChartValue({ value: 0.912, postfix: '%' })).toBe('0.912 %');
        expect(formatChartValue({ value: 0.912, postfix: '%', fraction: 1 })).toBe('0.9 %');
    });
});
