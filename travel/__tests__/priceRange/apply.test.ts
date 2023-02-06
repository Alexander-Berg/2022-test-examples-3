import {ITrainsPriceRangeOption} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import {CURRENCY_RUB} from 'utilities/currency/codes';

import priceRange from '../../priceRange';

const segmentWithoutPrices = {} as ITrainsTariffApiSegment;

const segmentWithPrice = {
    tariffs: {
        classes: {
            compartment: {
                price: {
                    currency: CURRENCY_RUB,
                    value: 200,
                },
            },
        },
    },
} as ITrainsTariffApiSegment;

const segmentWithSeveralPrices = {
    tariffs: {
        classes: {
            platzkarte: {
                price: {
                    currency: CURRENCY_RUB,
                    value: 100,
                },
            },
            compartment: {
                price: {
                    currency: CURRENCY_RUB,
                    value: 200,
                },
            },
        },
    },
} as ITrainsTariffApiSegment;

describe('priceRange', () => {
    describe('apply', () => {
        it('Не показываем сегменты без цены, если они не входят в указанный диапазон', () => {
            const result = priceRange.apply(
                [{min: 100, max: 300}] as ITrainsPriceRangeOption[],
                segmentWithoutPrices,
            );

            expect(result).toBe(false);
        });

        it('Сегмент попадает в диапазон цен', () => {
            const result = priceRange.apply(
                [{min: 100, max: 300}] as ITrainsPriceRangeOption[],
                segmentWithPrice,
            );

            expect(result).toBe(true);
        });

        it('Сегиент с несколькими ценами попадает в дипазон цен', () => {
            const result = priceRange.apply(
                [{min: 100, max: 300}] as ITrainsPriceRangeOption[],
                segmentWithSeveralPrices,
            );

            expect(result).toBe(true);
        });

        it('Сегмент с нескоклькими ценами попадает в один из диапазонов цен', () => {
            const result = priceRange.apply(
                [
                    {min: 50, max: 80},
                    {min: 100, max: 150},
                ] as ITrainsPriceRangeOption[],
                segmentWithSeveralPrices,
            );

            expect(result).toBe(true);
        });

        it('Сегмент с несколькими ценами не попадает ни в один из диапазонов цен', () => {
            const result = priceRange.apply(
                [
                    {min: 50, max: 80},
                    {min: 110, max: 150},
                ] as ITrainsPriceRangeOption[],
                segmentWithSeveralPrices,
            );

            expect(result).toBe(false);
        });

        it(
            'Сегмент не попадает в диапазон, так как верхняя граница не учитывается ' +
                '(price >= rangeMin && price < rangeMax)',
            () => {
                const result = priceRange.apply(
                    [{min: 50, max: 100}] as ITrainsPriceRangeOption[],
                    segmentWithPrice,
                );

                expect(result).toBe(false);
            },
        );

        it(
            'Сегмент c несколькими ценами не попадает в диапазон, так как верхняя ' +
                'граница не учитывается (price >= rangeMin && price < rangeMax)',
            () => {
                const result = priceRange.apply(
                    [{min: 50, max: 100}] as ITrainsPriceRangeOption[],
                    segmentWithSeveralPrices,
                );

                expect(result).toBe(false);
            },
        );
    });
});
