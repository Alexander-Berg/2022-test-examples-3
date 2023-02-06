import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import stationFrom from '../../stationFrom';

// @ts-ignore
const segment = {
    stationFrom: {
        id: 1,
        title: 'Станция 9 и три четверти',
    },
} as ITrainsTariffApiSegment;

describe('stationFrom.apply', () => {
    it('should return true for default', () => {
        const defaultFilterValue = stationFrom.getDefaultValue();
        const result = stationFrom.apply(defaultFilterValue, segment);

        expect(result).toBe(true);
    });

    it('should return true if value contains station id', () => {
        expect(stationFrom.apply(['1'], segment)).toBe(true);
    });

    it('should return false if value does not contain station id', () => {
        expect(stationFrom.apply(['2'], segment)).toBe(false);
    });
});
