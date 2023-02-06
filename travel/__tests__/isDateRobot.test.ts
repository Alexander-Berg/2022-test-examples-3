import isDateRobot from '../isDateRobot';

describe('isDateRobot', () => {
    it('is DateRobot', () => {
        expect(isDateRobot('2020-01-14')).toBe(true);
    });

    it('is not DateRobot', () => {
        expect(isDateRobot('2020-01-14 ')).toBe(false);
        expect(isDateRobot(' 2020-01-14')).toBe(false);
        expect(isDateRobot(' 2020-01-14 ')).toBe(false);
        expect(isDateRobot('2020.01.14')).toBe(false);
        expect(isDateRobot('2020-1-14')).toBe(false);
        expect(isDateRobot('yyyy-1-14')).toBe(false);
    });
});
