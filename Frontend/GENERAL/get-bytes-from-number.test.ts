import { getBytesFromNumber } from './get-bytes-from-number';

describe('utils/speaker/pairing-sound/getBytesFromNumber', () => {
    it('Должен получать байты для числа (1)', () => {
        expect(getBytesFromNumber(123)).toEqual([123, 0]);
    });

    it('Должен получать байты для числа (2)', () => {
        expect(getBytesFromNumber(12345)).toEqual([57, 48]);
    });

    it('Должен получать байты для числа (3)', () => {
        expect(getBytesFromNumber(0xFF)).toEqual([0xFF, 0]);
    });

    it('Должен получать байты для числа (4)', () => {
        expect(getBytesFromNumber(0x7FFF)).toEqual([0xFF, 127]);
    });
});
