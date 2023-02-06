import {ITrainsPriceRangeOption} from 'types/trains/search/filters/ITrainsFilters';

import priceRange from '../../priceRange';

describe('priceRange', () => {
    describe('serializeToQuery', () => {
        it('Есть отмеченные опции', () => {
            const result = priceRange.serializeToQuery([
                {value: '100-200'},
            ] as ITrainsPriceRangeOption[]);

            expect(result).toEqual({priceRange: ['100-200']});
        });

        it('Есть отмеченные опции. Несколько штук', () => {
            const result = priceRange.serializeToQuery([
                {value: '100-200'},
                {value: '200-300'},
            ] as ITrainsPriceRangeOption[]);

            expect(result).toEqual({priceRange: ['100-200', '200-300']});
        });

        it('Нет отмеченных опций', () => {
            const result = priceRange.serializeToQuery([]);

            expect(result).toEqual({
                priceRange: [],
            });
        });
    });
});
