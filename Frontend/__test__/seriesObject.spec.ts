import { computeOffset } from '../series';

describe('Функция computeOffset', () => {
    it('должна правильно правильно переводить номер эпизода в офсет карусели', () => {
        expect(computeOffset(1, 4, 2)).toBe(0);
        expect(computeOffset(8, 4, 2)).toBe(0);
        expect(computeOffset(9, 4, 2)).toBe(2);
        expect(computeOffset(15, 4, 2)).toBe(2);
        expect(computeOffset(16, 4, 2)).toBe(2);
        expect(computeOffset(17, 4, 2)).toBe(4);
    });
});
