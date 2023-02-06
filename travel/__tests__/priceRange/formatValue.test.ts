import {ITrainsPriceRangeOption} from 'types/trains/search/filters/ITrainsFilters';

import priceRange from '../../priceRange';

describe('priceRange', () => {
    describe('formatValue', () => {
        it('Преобразование value', () => {
            const result = priceRange.formatValue([
                {min: 100, max: 200, value: '100-200'},
            ] as ITrainsPriceRangeOption[]);

            expect(result).toEqual(['100-200']);
        });

        it('Преобразование value. Элементы без value игнорируются', () => {
            const result = priceRange.formatValue([
                {min: 100, max: 200, value: '100-200'},
                {min: 200, max: 300},
            ] as ITrainsPriceRangeOption[]);

            expect(result).toEqual(['100-200']);
        });
    });
});
