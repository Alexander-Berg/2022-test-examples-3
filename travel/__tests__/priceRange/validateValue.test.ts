import {ITrainsPriceRangeOption} from 'types/trains/search/filters/ITrainsFilters';

import priceRange from '../../priceRange';

const options = [
    {min: 100, max: 200, count: 4},
    {min: 200, max: 300, count: 6},
] as ITrainsPriceRangeOption[];

describe('priceRange', () => {
    describe('validateValue', () => {
        it('Валидные значения', () => {
            const result = priceRange.validateValue(
                [
                    {min: 100, max: 200, value: '100-200'},
                ] as ITrainsPriceRangeOption[],
                options,
            );

            expect(result).toEqual([options[0]]);
        });

        it('Не валидные значения', () => {
            // @ts-ignore
            let result = priceRange.validateValue(['100-400'], options);

            expect(result).toEqual([]);

            result = priceRange.validateValue(
                [{min: 200}] as ITrainsPriceRangeOption[],
                options,
            );
            expect(result).toEqual([]);
        });

        it('Часть значений не валидна', () => {
            const result = priceRange.validateValue(
                [
                    {min: 200, max: 300},
                    {min: 200, max: 400},
                ] as ITrainsPriceRangeOption[],
                options,
            );

            expect(result).toEqual([options[1]]);
        });

        it('Текущее значение не является массивом', () => {
            const result = priceRange.validateValue(
                // @ts-ignore
                {value: '100-200'},
                options,
            );

            expect(result).toEqual([]);
        });
    });
});
