import {CurrencyType} from 'utilities/currency/CurrencyType';
import priceRange from 'projects/trains/lib/filters/managers/priceRange';

describe('priceRange', () => {
    describe('getFrequencyRate', () => {
        it('Дефолтное значение 100', () => {
            expect(priceRange.getFrequencyRate()).toBe(100);
        });

        it('Значение для RUB', () => {
            expect(priceRange.getFrequencyRate(CurrencyType.RUB)).toBe(100);
        });
    });
});
