import { getMaxValue } from '../getMaxValue';

describe('getMaxValue', () => {
    it('Должен вернуть максимальное число', () => {
        expect(getMaxValue(0, 2)).toBe(2);
    });

    it('undefined конвертируется в 0', () => {
        expect(getMaxValue(undefined, undefined)).toBe(0);
        expect(getMaxValue(4, undefined)).toBe(4);
        expect(getMaxValue(-4, undefined)).toBe(0);
        expect(getMaxValue(undefined, 4)).toBe(4);
        expect(getMaxValue(undefined, -4)).toBe(0);
    });
});
