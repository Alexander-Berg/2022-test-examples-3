import isTransferSegment from '../../isTransferSegment';

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
        expect(isTransferSegment(segment)).toBe(false);
        expect(isTransferSegment(segmentFromApi)).toBe(false);
    });

    it('Вернет false для пересадки', () => {
        expect(isTransferSegment(transfer)).toBe(false);
        expect(isTransferSegment(transferFromApi)).toBe(false);
    });

    it('Вернет false для подсегмента', () => {
        expect(isTransferSegment(subSegment)).toBe(false);
        expect(isTransferSegment(subSegmentFromApi)).toBe(false);
    });

    it('Вернет true для сегмента из пересадки', () => {
        expect(isTransferSegment(transferSegment)).toBe(true);
        expect(isTransferSegment(transferSegmentFromApi)).toBe(true);
    });
});
