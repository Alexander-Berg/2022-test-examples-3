import {ITrainsSimpleFilterOption} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import stationTo from '../../stationTo';

// @ts-ignore
const segment = {
    stationTo: {
        id: 2,
        title: 'Хогвартс',
    },
} as ITrainsTariffApiSegment;

describe('stationTo', () => {
    describe('updateOptions', () => {
        it('should return updated options', () => {
            const options: ITrainsSimpleFilterOption[] = [];
            const result = stationTo.updateOptions(options, segment);

            expect(result).toEqual([
                {
                    value: '2',
                    text: 'Хогвартс',
                },
            ]);
        });

        it('should return options without changes', () => {
            const options = [
                {
                    value: '2',
                    text: 'Хогвартс',
                },
            ];
            const result = stationTo.updateOptions(options, segment);

            expect(result).toEqual(options);
        });
    });
});
