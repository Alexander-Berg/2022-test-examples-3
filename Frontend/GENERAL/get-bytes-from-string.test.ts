import { getBytesFromString } from './get-bytes-from-string';

describe('utils/speaker/pairing-sound/getBytesFromString', () => {
    it('Должен получать байты для строки (латиница)', () => {
        expect(getBytesFromString('abcdefgh')).toEqual([97, 98, 99, 100, 101, 102, 103, 104]);
    });

    it('Должен получать байты для строки (кириллица)', () => {
        expect(getBytesFromString('абвгдежз')).toEqual([208, 176, 208, 177, 208, 178, 208, 179, 208, 180, 208, 181, 208, 182, 208, 183]);
    });

    it('Должен получать байты для строки(числа)', () => {
        expect(getBytesFromString('0987654321')).toEqual([48, 57, 56, 55, 54, 53, 52, 51, 50, 49]);
    });

    it('Должен получать байты для строки (эмодзи)', () => {
        const BYTES = [115, 115, 105, 100, 32, 240, 159, 152, 128, 32, 101, 109, 111, 106, 105];

        expect(getBytesFromString('ssid \uD83D\uDE00 emoji')).toEqual(BYTES);
        expect(getBytesFromString('ssid 😀 emoji')).toEqual(BYTES);
    });
});
