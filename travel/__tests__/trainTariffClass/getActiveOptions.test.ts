import {
    COMPARTMENT,
    PLATZKARTE,
    SITTING,
    SUITE,
} from 'projects/trains/lib/segments/tariffs/constants';

import {ITrainsFilters} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import {CURRENCY_RUB} from 'utilities/currency/codes';

import trainTariffClass from '../../trainTariffClass';

const currency = CURRENCY_RUB;
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
            value: 100,
        },
    },
};
const compartment = {
    [COMPARTMENT]: {
        price: {
            currency,
            value: 200,
        },
    },
};
const suite = {
    [SUITE]: {
        price: {
            currency,
            value: 10000,
        },
    },
};

describe('trainTariffClass', () => {
    describe('getActiveOptions', () => {
        it('Получение списка доступных опций', () => {
            const segments = [
                {
                    tariffs: {
                        classes: {
                            ...sitting,
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            ...compartment,
                            ...suite,
                        },
                    },
                },
            ] as ITrainsTariffApiSegment[];
            const filtersData = {} as ITrainsFilters;
            const newOptionsForSegments = [SITTING, COMPARTMENT, SUITE];
            const result = trainTariffClass.getActiveOptions({
                filtersData,
                segments,
            });

            expect(result.sort()).toEqual(newOptionsForSegments.sort());
        });

        it('Получение списка доступных опций. Случай, когда выбран диапазон цен.', () => {
            const segments = [
                {
                    tariffs: {
                        classes: {
                            ...sitting,
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            ...sitting,
                            ...platzkarte,
                        },
                    },
                },
            ] as ITrainsTariffApiSegment[];
            const filtersData = {
                priceRange: {
                    value: [
                        {
                            min: 10,
                            max: 70,
                        },
                    ],
                    filteredSegmentIndices: [true, true],
                },
            } as ITrainsFilters;
            const newOptionsForSegments = [SITTING];
            const result = trainTariffClass.getActiveOptions({
                filtersData,
                segments,
            });

            expect(result.sort()).toEqual(newOptionsForSegments.sort());
        });
    });
});
