import roundToInRange from 'utilities/numbers/roundToInRange';

describe('roundToInRange(range, step, x)', () => {
    it('Должен оставлять исходное число при шаге 1', () => {
        const range = [0, 25];
        const step = 1;

        expect(roundToInRange(range, step, 5)).toBe(5);
        expect(roundToInRange(range, step, 15)).toBe(15);
        expect(roundToInRange(range, step, 25)).toBe(25);
        expect(roundToInRange(range, step, 30)).toBe(25);
        expect(roundToInRange(range, step, -10)).toBe(0);
    });

    it('Должен корректно работать при шаге в 10', () => {
        const range = [0, 110];
        const step = 10;

        expect(roundToInRange(range, step, 5)).toBe(10);
        expect(roundToInRange(range, step, 10)).toBe(10);
        expect(roundToInRange(range, step, 2)).toBe(0);
        expect(roundToInRange(range, step, 23)).toBe(20);
        expect(roundToInRange(range, step, 77)).toBe(80);
        expect(roundToInRange(range, step, 99)).toBe(100);
        expect(roundToInRange(range, step, -1)).toBe(0);
        expect(roundToInRange(range, step, 111)).toBe(110);
    });

    it('Должен корректно работать при шаге 50', () => {
        const range = [0, 500];
        const step = 50;

        expect(roundToInRange(range, step, 5)).toBe(0);
        expect(roundToInRange(range, step, 45)).toBe(50);
        expect(roundToInRange(range, step, 312)).toBe(300);
        expect(roundToInRange(range, step, 473)).toBe(450);
        expect(roundToInRange(range, step, 129)).toBe(150);
    });
});
