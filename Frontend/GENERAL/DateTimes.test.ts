import { timer } from './DateTimes';

describe('timer', () => {
    it('до минуты', () => {
        expect(timer(60)).toBe('1:00');
        expect(timer(59)).toBe('0:59');
        expect(timer(10)).toBe('0:10');
        expect(timer(1)).toBe('0:01');
        expect(timer(0)).toBe('0:00');
    });
});
