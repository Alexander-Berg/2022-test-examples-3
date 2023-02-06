import isSubSegmentFromApi from '../../isSubSegmentFromApi';

import {
    transferFromApi,
    transfer,
    transferSegmentFromApi,
    transferSegment,
    segmentFromApi,
    segment,
    subSegment,
    subSegmentFromApi,
} from './segments';

describe('isSegment', () => {
    it('Вернет false для пересадки', () => {
        expect(isSubSegmentFromApi(transfer)).toBe(false);
        expect(isSubSegmentFromApi(transferFromApi)).toBe(false);
    });

    it('Вернет false для сегмента из пересадки', () => {
        expect(isSubSegmentFromApi(transferSegment)).toBe(false);
        expect(isSubSegmentFromApi(transferSegmentFromApi)).toBe(false);
    });

    it('Вернет false для обычного сегмента', () => {
        expect(isSubSegmentFromApi(segment)).toBe(false);
        expect(isSubSegmentFromApi(segmentFromApi)).toBe(false);
    });

    it('Вернет true для подсегмента', () => {
        expect(isSubSegmentFromApi(subSegment)).toBe(true);
        expect(isSubSegmentFromApi(subSegmentFromApi)).toBe(true);
    });
});
