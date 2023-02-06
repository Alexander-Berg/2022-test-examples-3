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
    describe('formatOptions', () => {
        it('Без функции преобразования свойства text', () => {
            const result = priceRange.formatOptions(options);

            expect(result).toEqual([
                {
                    value: '100-200',
                    text: '100 - 200',
                },
                {
                    value: '200-300',
                    text: '200 - 300',
                },
            ]);
        });

        it('С функцией преобразования свойства text', () => {
            const formatText = (option: ITrainsPriceRangeOption): string =>
                `${option.min}p - ${option.max}p`;
            const result = priceRange.formatOptions(options, formatText);

            expect(result).toEqual([
                {
                    value: '100-200',
                    text: '100p - 200p',
                },
                {
                    value: '200-300',
                    text: '200p - 300p',
                },
            ]);
        });
    });
});
