import isTransfer from '../../isTransfer';

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

describe('isTransfer', () => {
    it('Вернет false для обычного сегмента', () => {
        expect(isTransfer(segment)).toBe(false);
        expect(isTransfer(segmentFromApi)).toBe(false);
    });

    it('Вернет false для сегмента из пересадки', () => {
        expect(isTransfer(transferSegment)).toBe(false);
        expect(isTransfer(transferSegmentFromApi)).toBe(false);
    });

    it('Вернет false для подсегмента', () => {
        expect(isTransfer(subSegment)).toBe(false);
        expect(isTransfer(subSegmentFromApi)).toBe(false);
    });

    it('Вернет true для пересадки', () => {
        expect(isTransfer(transfer)).toBe(true);
        expect(isTransfer(transferFromApi)).toBe(true);
    });
});
