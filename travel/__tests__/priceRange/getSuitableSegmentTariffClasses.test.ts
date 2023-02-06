import {ITrainsPriceRangeOption} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import {CurrencyType} from 'utilities/currency/CurrencyType';

import priceRange from '../../priceRange';

const currency = CurrencyType.RUB;

describe('priceRange', () => {
    describe('getSuitableSegmentTariffClasses', () => {
        it('вернет только те тарифы, которые входят в диапазон', () => {
            const value = [
                {
                    min: 100,
                    max: 200,
                },
            ] as ITrainsPriceRangeOption[];
            const segment = {
                tariffs: {
                    classes: {
                        compartment: {
                            price: {
                                currency,
                                value: 150,
                            },
                        },
                        platzkarte: {
                            price: {
                                currency,
                                value: 50,
                            },
                        },
                    },
                },
            } as ITrainsTariffApiSegment;
            const result = priceRange.getSuitableSegmentTariffClasses(
                value,
                segment,
            );

            expect(result).toEqual(['compartment']);
        });
    });
});
