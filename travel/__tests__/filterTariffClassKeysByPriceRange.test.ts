import {TRAIN_COACH_TYPE} from 'projects/trains/constants/coachType';

import {ITrainsFilters} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import {CurrencyType} from 'utilities/currency/CurrencyType';

import filterTariffClassKeysByPriceRange from '../filterTariffClassKeysByPriceRange';

describe('filterTariffClassKeysByPriceRange', () => {
    it('Показываем только compartment', () => {
        const segment = {
            tariffs: {
                classes: {
                    [TRAIN_COACH_TYPE.COMPARTMENT]: {
                        price: {
                            currency: CurrencyType.RUB,
                            value: 200,
                        },
                    },
                    [TRAIN_COACH_TYPE.PLATZKARTE]: {
                        price: {
                            currency: CurrencyType.RUB,
                            value: 100,
                        },
                    },
                },
            },
        } as ITrainsTariffApiSegment;
        const filtersData = {
            priceRange: {
                value: [
                    {
                        min: 200,
                        max: 300,
                    },
                ],
            },
        } as ITrainsFilters;
        const result = filterTariffClassKeysByPriceRange(
            [TRAIN_COACH_TYPE.COMPARTMENT, TRAIN_COACH_TYPE.SUITE],
            segment,
            filtersData,
        );

        expect(result).toEqual([TRAIN_COACH_TYPE.COMPARTMENT]);
    });
});
