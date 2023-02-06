import { getChecksumBytes } from './get-checksum-bytes';

describe('utils/speaker/pairing-sound/getChecksumBytes', () => {
    it('Должен считать контрольную сумму (1)', () => {
        expect(getChecksumBytes([1, 2, 3, 4, 5, 6])).toEqual([36, 119]);
    });

    it('Должен считать контрольную сумму (2)', () => {
        expect(getChecksumBytes([-12, 1, -127, 80, 0, 13])).toEqual([79, 80]);
    });

    it('Должен считать контрольную сумму (3)', () => {
        expect(getChecksumBytes([244, 1, 129, 80, 0, 13])).toEqual([79, 80]);
    });

    it('Должен считать контрольную сумму (4)', () => {
        expect(getChecksumBytes([2, 8, 97, 98, 99, 100, 101, 102, 103, 104, 8, 49, 50, 51, 52, 53, 54, 55, 56, 10, 48, 57, 56, 55, 54, 53, 52, 51, 50, 49])).toEqual([243, 38]);
    });
});
