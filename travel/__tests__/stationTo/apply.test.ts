import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import stationTo from '../../stationTo';

// @ts-ignore
const segment = {
    stationTo: {
        id: 2,
        title: 'Хогвартс',
    },
} as ITrainsTariffApiSegment;

describe('stationTo.apply', () => {
    it('should return true for default', () => {
        const defaultFilterValue = stationTo.getDefaultValue();
        const result = stationTo.apply(defaultFilterValue, segment);

        expect(result).toBe(true);
    });

    it('should return true if value contains station id', () => {
        expect(stationTo.apply(['2'], segment)).toBe(true);
    });

    it('should return false if value does not contain station id', () => {
        expect(stationTo.apply(['1'], segment)).toBe(false);
    });
});
