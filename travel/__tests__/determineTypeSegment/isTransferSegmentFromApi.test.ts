import isTransferSegmentFromApi from '../../isTransferSegmentFromApi';

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

describe('isTransferSegmentFromApi', () => {
    it('Вернет false для обычного сегмента', () => {
        expect(isTransferSegmentFromApi(segment)).toBe(false);
        expect(isTransferSegmentFromApi(segmentFromApi)).toBe(false);
    });

    it('Вернет false для пересадки', () => {
        expect(isTransferSegmentFromApi(transfer)).toBe(false);
        expect(isTransferSegmentFromApi(transferFromApi)).toBe(false);
    });

    it('Вернет false для подсегмента', () => {
        expect(isTransferSegmentFromApi(subSegment)).toBe(false);
        expect(isTransferSegmentFromApi(subSegmentFromApi)).toBe(false);
    });

    it('Вернет true для сегмента из пересадки', () => {
        expect(isTransferSegmentFromApi(transferSegment)).toBe(true);
        expect(isTransferSegmentFromApi(transferSegmentFromApi)).toBe(true);
    });
});
