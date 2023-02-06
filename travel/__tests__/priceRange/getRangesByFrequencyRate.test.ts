import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import priceRange from '../../priceRange';

describe('priceRange', () => {
    describe('getRangesByFrequencyRate', () => {
        it('В случае если валюта не указана, в качестве минимального шага диапазона берется 100', () => {
            const segments = [
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                price: {
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
                                    value: 250,
                                },
                            },
                        },
                    },
                },
            ] as ITrainsTariffApiSegment[];
            const {totalCount, roundRanges} =
                priceRange.getRangesByFrequencyRate(segments);

            expect(totalCount).toBe(2);
            expect(roundRanges).toEqual(
                new Map([
                    ['100-200', {min: 100, max: 200, count: 1}],
                    ['200-300', {min: 200, max: 300, count: 1}],
                ]),
            );
        });
    });
});
