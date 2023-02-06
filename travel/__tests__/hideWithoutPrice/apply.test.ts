import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import hideWithoutPrice from '../../hideWithoutPrice';

// @ts-ignore
const segmentWithPrice = {
    tariffs: {
        classes: {
            platzkarte: '1000 roubles',
        },
    },
} as ITrainsTariffApiSegment;

const segmentWithoutPrice = {} as ITrainsTariffApiSegment;

describe('hideWithoutPrice.apply', () => {
    it('show segments with price only, segment with price', () => {
        const result = hideWithoutPrice.apply(true, segmentWithPrice);

        expect(result).toBe(true);
    });

    it('show segments with price only, segment without price', () => {
        const result = hideWithoutPrice.apply(true, segmentWithoutPrice);

        expect(result).toBe(false);
    });

    it('show all segments, segment with price', () => {
        const result = hideWithoutPrice.apply(false, segmentWithPrice);

        expect(result).toBe(true);
    });

    it('show all segments, segment without price', () => {
        const result = hideWithoutPrice.apply(false, segmentWithoutPrice);

        expect(result).toBe(true);
    });
});
