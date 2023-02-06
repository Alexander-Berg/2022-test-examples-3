import { calcProductSkeletonsCount } from '../utils/calcProductSkeletonsCount';

describe('calcProductSkeletonsCount', () => {
    it('корректно вычисляет количество без totalItems', () => {
        expect(calcProductSkeletonsCount(4, 0)).toBe(0);
        expect(calcProductSkeletonsCount(11, 1)).toBe(0);
        expect(calcProductSkeletonsCount(25, 2)).toBe(0);
    });

    it('корректно вычисляет количество totalItems', () => {
        expect(calcProductSkeletonsCount(10, 0, 12)).toBe(2);
        expect(calcProductSkeletonsCount(30, 2, 33)).toBe(3);
    });

    it('не должен возвращать значение меньше 0', () => {
        expect(calcProductSkeletonsCount(30, 2, 13)).toBeGreaterThanOrEqual(0);
        expect(calcProductSkeletonsCount(10, 0, 0)).toBeGreaterThanOrEqual(0);
    });

    it('не должен возвращать значение больше, чем DEFAULT_VALUE', () => {
        const DEFAULT_VALUE = 4;
        expect(calcProductSkeletonsCount(10, 0, 12)).toBeLessThanOrEqual(DEFAULT_VALUE);
        expect(calcProductSkeletonsCount(10, 0, 100)).toBeLessThanOrEqual(DEFAULT_VALUE);
        expect(calcProductSkeletonsCount(30, 2, 300)).toBeLessThanOrEqual(DEFAULT_VALUE);
    });
});
