import isSegmentFromApi from '../../isSegmentFromApi';

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

describe('isSegmentFromApi', () => {
    it('Вернет false для пересадки', () => {
        expect(isSegmentFromApi(transfer)).toBe(false);
        expect(isSegmentFromApi(transferFromApi)).toBe(false);
    });

    it('Вернет false для сегмента из пересадки', () => {
        expect(isSegmentFromApi(transferSegment)).toBe(false);
        expect(isSegmentFromApi(transferSegmentFromApi)).toBe(false);
    });

    it('Вернет false для подсегмента', () => {
        expect(isSegmentFromApi(subSegment)).toBe(false);
        expect(isSegmentFromApi(subSegmentFromApi)).toBe(false);
    });

    it('Вернет true для обычного сегмента', () => {
        expect(isSegmentFromApi(segment)).toBe(true);
        expect(isSegmentFromApi(segmentFromApi)).toBe(true);
    });
});
