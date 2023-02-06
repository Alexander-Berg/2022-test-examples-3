import isSegment from '../../isSegment';

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
        expect(isSegment(transfer)).toBe(false);
        expect(isSegment(transferFromApi)).toBe(false);
    });

    it('Вернет false для сегмента из пересадки', () => {
        expect(isSegment(transferSegment)).toBe(false);
        expect(isSegment(transferSegmentFromApi)).toBe(false);
    });

    it('Вернет false для подсегмента', () => {
        expect(isSegment(subSegment)).toBe(false);
        expect(isSegment(subSegmentFromApi)).toBe(false);
    });

    it('Вернет true для обычного сегмента', () => {
        expect(isSegment(segment)).toBe(true);
        expect(isSegment(segmentFromApi)).toBe(true);
    });
});
