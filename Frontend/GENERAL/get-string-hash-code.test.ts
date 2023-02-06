import { getStringHashCode } from './get-string-hash-code';

describe('utils/speaker/pairing-sound/getStringHashCode', () => {
    it('Должен генерировать хэш строки (латиница)', () => {
        expect(getStringHashCode('abcdefgh')).toEqual(1259673732);
    });

    it('Должен генерировать хэш строки (кириллица)', () => {
        expect(getStringHashCode('абвгдежз')).toEqual(499462148);
    });

    it('Должен генерировать хэш строки (числа)', () => {
        expect(getStringHashCode('0987654321')).toEqual(1189310725);
    });

    it('Должен генерировать хэш строки (эмодзи)', () => {
        expect(getStringHashCode('ssid \uD83D\uDE00 emoji')).toEqual(1123574350);
        expect(getStringHashCode('ssid 😀 emoji')).toEqual(1123574350);
    });
});
