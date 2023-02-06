import { arrangeBits } from './arrange-bits';
import { BITS } from './constants';
import { encodeReedSolomon } from './encode-reed-solomon';

describe('utils/speaker/pairing-sound/encodeReedSolomon', () => {
    it('Должен кодировать алгоритмом Рида-Соломона', () => {
        const bytes = [2, 7, 97, 98, 99, 100, 101, 102, 103, 8, 49, 50, 51, 52, 53, 54, 55, 56, 10, 48, 57, 56, 55, 54, 53, 52, 51, 50, 49, 70, 60];
        const bitGroups = arrangeBits(bytes, BITS);

        expect(encodeReedSolomon(bitGroups)).toEqual([0, 2, 0, 7, 6, 1, 6, 2, 6, 3, 6, 14, 3, 8, 14, 4, 6, 5, 6, 6, 6, 7, 0, 8, 3, 1, 2, 3, 8, 1, 3, 2, 3, 3, 3, 4, 3, 5, 3, 6, 3, 0, 8, 1, 10, 7, 3, 8, 0, 10, 3, 0, 3, 9, 3, 8, 7, 1, 13, 8, 3, 7, 3, 6, 3, 5, 3, 4, 3, 3, 3, 5, 15, 11, 15, 2, 3, 1, 4, 6, 3, 12, 4, 4, 0, 10]);
    });
});
