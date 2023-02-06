import isSubSegment from '../../isSubSegment';

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
        expect(isSubSegment(transfer)).toBe(false);
        expect(isSubSegment(transferFromApi)).toBe(false);
    });

    it('Вернет false для сегмента из пересадки', () => {
        expect(isSubSegment(transferSegment)).toBe(false);
        expect(isSubSegment(transferSegmentFromApi)).toBe(false);
    });

    it('Вернет false для обычного сегмента', () => {
        expect(isSubSegment(segment)).toBe(false);
        expect(isSubSegment(segmentFromApi)).toBe(false);
    });

    it('Вернет true для подсегмента', () => {
        expect(isSubSegment(subSegment)).toBe(true);
        expect(isSubSegment(subSegmentFromApi)).toBe(true);
    });
});
