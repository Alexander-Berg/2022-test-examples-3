import MockDate from 'mockdate';

import addYears from '../addYears';

describe('addYears(date, yearsCount)', () => {
    const testMockDate = '2018-10-11';

    beforeEach(() => {
        MockDate.set(testMockDate);
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('Return Date instance', () => {
        expect(addYears(new Date(), 2)).toBeInstanceOf(Date);
    });

    it('Equal year', () => {
        const yearCount = 0;
        const currentYear = new Date().getUTCFullYear();
        const resultYear = currentYear + yearCount;

        expect(addYears(new Date(), yearCount).getUTCFullYear()).toBe(
            resultYear,
        );
    });

    it('Up year', () => {
        const yearCount = 2;
        const currentYear = new Date().getUTCFullYear();
        const resultYear = currentYear + yearCount;

        expect(addYears(new Date(), yearCount).getUTCFullYear()).toBe(
            resultYear,
        );
    });

    it('Without months change', () => {
        const yearCount = 10;
        const currentMonth = new Date().getUTCMonth();

        expect(addYears(new Date(), yearCount).getUTCMonth()).toBe(
            currentMonth,
        );
    });
});
