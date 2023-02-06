import * as diff from './DateDiff';

describe('diff', () => {
    it('more than week', () => {
        expect(diff.isDiffMoreThanWeek(
            new Date(1990, 10, 1, 0, 0, 0),
            new Date(1990, 10, 1, 0, 0, 0),
        )).toBe(false);

        expect(diff.isDiffMoreThanWeek(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(1990, 10, 1, 0, 0, 0),
        )).toBe(true);

        expect(diff.isDiffMoreThanWeek(
            new Date(1990, 10, 1, 0, 0, 0),
            new Date(2018, 10, 1, 0, 0, 0),
        )).toBe(true);

        expect(diff.isDiffMoreThanWeek(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 10, 3, 0, 0, 0),
        )).toBe(false);

        expect(diff.isDiffMoreThanWeek(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 10, 7, 0, 0, 0),
        )).toBe(false);

        expect(diff.isDiffMoreThanWeek(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 10, 8, 0, 0, 0),
        )).toBe(false);

        expect(diff.isDiffMoreThanWeek(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 10, 8, 0, 0, 1),
        )).toBe(true);

        expect(diff.isDiffMoreThanWeek(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 10, 10, 0, 0, 0),
        )).toBe(true);
    });

    it('is same date', () => {
        expect(diff.isSameDate(
            new Date(1990, 10, 1, 0, 0, 0),
            new Date(1990, 10, 1, 0, 0, 0),
        )).toBe(true);

        expect(diff.isSameDate(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(1990, 10, 1, 0, 0, 0),
        )).toBe(false);

        expect(diff.isSameDate(
            new Date(1990, 10, 1, 0, 0, 0),
            new Date(2018, 10, 1, 0, 0, 0),
        )).toBe(false);

        expect(diff.isSameDate(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 10, 3, 0, 0, 0),
        )).toBe(false);

        expect(diff.isSameDate(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 10, 7, 0, 0, 0),
        )).toBe(false);

        expect(diff.isSameDate(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 11, 1, 0, 0, 0),
        )).toBe(false);

        expect(diff.isSameDate(
            new Date(2018, 1, 0, 10, 0, 0),
            new Date(2018, 1, 0, 16, 59, 59),
        )).toBe(true);
    });

    it('is before week', () => {
        expect(diff.isBeforeThenWeek(
            new Date(1990, 10, 1, 0, 0, 0),
            new Date(1990, 10, 1, 0, 0, 0),
        )).toBe(false);

        expect(diff.isBeforeThenWeek(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(1990, 10, 1, 0, 0, 0),
        )).toBe(true);

        expect(diff.isBeforeThenWeek(
            new Date(1990, 10, 1, 0, 0, 0),
            new Date(2018, 10, 1, 0, 0, 0),
        )).toBe(false);

        expect(diff.isBeforeThenWeek(
            new Date(2018, 10, 1, 0, 0, 0),
            new Date(2018, 10, 3, 0, 0, 0),
        )).toBe(false);

        expect(diff.isBeforeThenWeek(
            new Date(2018, 10, 7, 0, 0, 0),
            new Date(2018, 10, 1, 0, 0, 0),
        )).toBe(false);

        expect(diff.isBeforeThenWeek(
            new Date(2018, 10, 8, 0, 0, 0),
            new Date(2018, 10, 1, 0, 0, 0),
        )).toBe(false);

        expect(diff.isBeforeThenWeek(
            new Date(2018, 10, 8, 0, 0, 1),
            new Date(2018, 10, 1, 0, 0, 0),
        )).toBe(true);
    });
});
