import {
    COMPARTMENT,
    PLATZKARTE,
    SITTING,
    SUITE,
} from 'projects/trains/lib/segments/tariffs/constants';

import {
    ITrainsFilters,
    ITrainsPriceRangeOption,
} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import {CurrencyType} from 'utilities/currency/CurrencyType';

import priceRange from '../../priceRange';

const currency = CurrencyType.RUB;
const sitting = {
    [SITTING]: {
        price: {
            currency,
            value: 50,
        },
    },
};
const platzkarte = {
    [PLATZKARTE]: {
        price: {
            currency,
            value: 150,
        },
    },
};
const compartment = {
    [COMPARTMENT]: {
        price: {
            currency,
            value: 250,
        },
    },
};
const suite = {
    [SUITE]: {
        price: {
            currency,
            value: 350,
        },
    },
};
const segment = {
    tariffs: {
        classes: {
            ...sitting,
            ...platzkarte,
            ...compartment,
            ...suite,
        },
    },
};

describe('priceRange', () => {
    describe('updateOptionsForFilteredSegments', () => {
        it('Обновление доступного количества тарифов при фильтрации по типу вагона', () => {
            const segments = [
                segment,
                segment,
                segment,
                segment,
            ] as ITrainsTariffApiSegment[];
            const options = [
                {min: 0, max: 100, count: 4},
                {min: 100, max: 200, count: 4},
                {min: 200, max: 300, count: 4},
                {min: 300, max: 400, count: 4},
            ] as ITrainsPriceRangeOption[];
            const filtersData = {
                trainTariffClass: {
                    value: [SITTING, PLATZKARTE, SUITE],
                    filteredSegmentIndices: [true, true, true, true],
                },
            } as ITrainsFilters;
            const validNewOptions = [
                {min: 0, max: 100, count: 4},
                {min: 100, max: 200, count: 4},
                {min: 200, max: 300, count: 0},
                {min: 300, max: 400, count: 4},
            ];

            const newOptions = priceRange.updateOptionsForFilteredSegments({
                options,
                filtersData,
                segments,
            });

            expect(newOptions).toEqual(validNewOptions);
        });
    });
});
