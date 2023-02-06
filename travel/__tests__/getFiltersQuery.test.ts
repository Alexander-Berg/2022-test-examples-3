import {ITrainsFilters} from 'types/trains/search/filters/ITrainsFilters';
import {ETimeOfDay} from 'utilities/dateUtils/types';

import getFiltersQuery from '../getFiltersQuery';

describe('filters', () => {
    it('getFiltersQuery', () => {
        // @ts-ignore
        const filters = {
            arrival: {value: [ETimeOfDay.DAY]},
            departure: {value: [ETimeOfDay.NIGHT]},
            hideWithoutPrice: {value: false},
            highSpeedTrain: {value: []},
            priceRange: {value: []},
            stationFrom: {value: ['Курский вокзал']},
            stationTo: {value: []},
            trainTariffClass: {value: []},
        } as ITrainsFilters;

        const query = getFiltersQuery(filters);

        expect(query).toEqual({
            arrival: [ETimeOfDay.DAY],
            departure: [ETimeOfDay.NIGHT],
            seats: undefined,
            highSpeedTrain: [],
            priceRange: [],
            stationFrom: ['Курский вокзал'],
            stationTo: [],
            trainTariffClass: [],
        });
    });
});
