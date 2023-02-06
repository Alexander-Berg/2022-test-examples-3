import {ITrainsPriceRangeOption} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import {CurrencyType} from 'utilities/currency/CurrencyType';

import priceRange from '../../priceRange';

const currency = CurrencyType.RUB;

const segmentWithoutPrices = {} as ITrainsTariffApiSegment;

const segmentWithPrice = {
    tariffs: {
        classes: {
            compartment: {
                price: {
                    currency,
                    value: 200,
                },
            },
        },
    },
} as ITrainsTariffApiSegment;

// создаем массив с ценами, которые кратны минимальному шагу диапазонов цен (100)
const segmentsWithPricesOnLimit = [
    {
        tariffs: {
            classes: {
                compartment: {
                    price: {
                        currency,
                        value: 100,
                    },
                },
            },
        },
    },
    {
        tariffs: {
            classes: {
                compartment: {
                    price: {
                        currency,
                        value: 200,
                    },
                },
            },
        },
    },
    {
        tariffs: {
            classes: {
                compartment: {
                    price: {
                        currency,
                        value: 300,
                    },
                },
            },
        },
    },
    {
        tariffs: {
            classes: {
                compartment: {
                    price: {
                        currency,
                        value: 400,
                    },
                },
            },
        },
    },
] as ITrainsTariffApiSegment[];

// создаем массив с ценами, которые находятся внутри минимального шага диапазонов цен (100)
const segmentsWithPricesInRange = [
    {
        tariffs: {
            classes: {
                compartment: {
                    price: {
                        currency,
                        value: 150,
                    },
                },
            },
        },
    },
    {
        tariffs: {
            classes: {
                compartment: {
                    price: {
                        currency,
                        value: 250,
                    },
                },
            },
        },
    },
    {
        tariffs: {
            classes: {
                compartment: {
                    price: {
                        currency,
                        value: 350,
                    },
                },
            },
        },
    },
    {
        tariffs: {
            classes: {
                compartment: {
                    price: {
                        currency,
                        value: 450,
                    },
                },
            },
        },
    },
] as ITrainsTariffApiSegment[];

const optionsForSegmentsWithPrices = [
    {
        count: 1,
        min: 100,
        max: 200,
        currency,
        value: '100-200',
    },
    {
        count: 1,
        min: 200,
        max: 300,
        currency,
        value: '200-300',
    },
    {
        count: 1,
        min: 300,
        max: 400,
        currency,
        value: '300-400',
    },
    {
        count: 1,
        min: 400,
        max: 500,
        currency,
        value: '400-500',
    },
];

describe('priceRange', () => {
    describe('updateOptions', () => {
        it(
            'updateOptions возвращает те же опции, что и были переданы. Это связано с тем, ' +
                'что для получения списка опций нужно сначала просканировать все сегменты',
            () => {
                const options = [
                    {value: '100-200'},
                    {value: '200-300'},
                ] as ITrainsPriceRangeOption[];
                const result = priceRange.updateOptions(options);

                expect(result).toEqual(options);
            },
        );
    });

    describe('getOptions', () => {
        it('Список доступных цен для сегментов без цен вернет пустой массив', () => {
            const options = priceRange.getOptions([segmentWithoutPrices]);

            expect(options).toEqual([]);
        });

        it('Если не удается сформировать как минимум 2 диапазона с ценами, то вернется пустой массив', () => {
            const options = priceRange.getOptions([segmentWithPrice]);

            expect(options).toEqual([]);
        });

        it('Должно быть два диапазона с ценами', () => {
            const options = priceRange.getOptions(
                segmentsWithPricesInRange.slice(0, 2),
            );

            expect(options).toEqual(optionsForSegmentsWithPrices.slice(0, 2));
        });

        it('Должно быть четыре диапазона с ценами. Цены кратны минимальному шагу диапазона', () => {
            const options = priceRange.getOptions(segmentsWithPricesOnLimit);

            expect(options).toEqual(optionsForSegmentsWithPrices);
        });

        it('Должно быть четыре диапазона с ценами. Цены находятся внутри диапазонов', () => {
            const options = priceRange.getOptions(segmentsWithPricesInRange);

            expect(options).toEqual(optionsForSegmentsWithPrices);
        });
    });
});
