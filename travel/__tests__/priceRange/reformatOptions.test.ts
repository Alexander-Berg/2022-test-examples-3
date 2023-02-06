import {ITrainsPriceRangeOption} from 'types/trains/search/filters/ITrainsFilters';

import {CurrencyType} from 'utilities/currency/CurrencyType';

import priceRange from '../../priceRange';

const options: ITrainsPriceRangeOption[] = [
    {
        min: 100,
        max: 200,
        count: 4,
        value: '100-200',
        currency: CurrencyType.RUB,
    },
    {
        min: 200,
        max: 300,
        count: 6,
        value: '200-300',
        currency: CurrencyType.RUB,
    },
];

describe('priceRange', () => {
    describe('reformatOptions', () => {
        it('Все опции валидны', () => {
            const result = priceRange.reformatOptions(
                ['100-200', '200-300'],
                options,
            );

            expect(result).toEqual(options);
        });

        it('Одна отформатированная опция не валидна', () => {
            const result = priceRange.reformatOptions(
                ['100-????', '200-300'],
                options,
            );

            expect(result).toEqual([
                {
                    min: 200,
                    max: 300,
                    count: 6,
                    value: '200-300',
                    currency: CurrencyType.RUB,
                },
            ]);
        });

        it('Передача опций из url', () => {
            const result = priceRange.reformatOptions(
                [
                    {
                        min: 100,
                        max: 200,
                    },
                    {
                        min: 200,
                        max: 300,
                    },
                ],
                options,
            );

            expect(result).toEqual(options);
        });
    });
});
