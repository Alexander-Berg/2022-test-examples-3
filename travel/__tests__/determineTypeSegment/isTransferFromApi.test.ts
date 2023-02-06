import isTransferFromApi from '../../isTransferFromApi';

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

describe('isTransferFromApi', () => {
    it('Вернет false для обычного сегмента', () => {
        expect(isTransferFromApi(segment)).toBe(false);
        expect(isTransferFromApi(segmentFromApi)).toBe(false);
    });

    it('Вернет false для сегмента из пересадки', () => {
        expect(isTransferFromApi(transferSegment)).toBe(false);
        expect(isTransferFromApi(transferSegmentFromApi)).toBe(false);
    });

    it('Вернет false для подсегмента', () => {
        expect(isTransferFromApi(subSegment)).toBe(false);
        expect(isTransferFromApi(subSegmentFromApi)).toBe(false);
    });

    it('Вернет true для пересадки', () => {
        expect(isTransferFromApi(transfer)).toBe(true);
        expect(isTransferFromApi(transferFromApi)).toBe(true);
    });
});
