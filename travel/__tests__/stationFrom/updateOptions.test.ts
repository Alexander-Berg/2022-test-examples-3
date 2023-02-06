import {ITrainsSimpleFilterOption} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import stationFrom from '../../stationFrom';

// @ts-ignore
const segment = {
    stationFrom: {
        id: 1,
        title: 'Станция 9 и три четверти',
    },
} as ITrainsTariffApiSegment;

describe('stationFrom', () => {
    describe('updateOptions', () => {
        it('should return updated options', () => {
            const options: ITrainsSimpleFilterOption[] = [];
            const result = stationFrom.updateOptions(options, segment);

            expect(result).toEqual([
                {
                    value: '1',
                    text: 'Станция 9 и три четверти',
                },
            ]);
        });

        it('should return options without changes', () => {
            const options = [
                {
                    value: '1',
                    text: 'Станция 9 и три четверти',
                },
            ];
            const result = stationFrom.updateOptions(options, segment);

            expect(result).toEqual(options);
        });
    });
});
