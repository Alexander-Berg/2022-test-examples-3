import isDateMoment from '../isDateMoment';

describe('isDateMoment', () => {
    it('is DateMoment', () => {
        expect(isDateMoment('2020-01-23T17:52:00+05:00')).toBe(true);
        expect(isDateMoment('2020-01-23T17:52:00-05:00')).toBe(true);
    });

    it('is not DateMoment', () => {
        expect(isDateMoment('2020-01-14')).toBe(false);
        expect(isDateMoment(' 2020-01-23T17:52:00+05:00')).toBe(false);
        expect(isDateMoment('2020-01-23T17:52:00+05:00 ')).toBe(false);
        expect(isDateMoment(' 2020-01-23T17:52:00+05:00 ')).toBe(false);
        expect(isDateMoment('2020.01.23T17:52:00+05:00')).toBe(false);
        expect(isDateMoment('2020-1-23T17:52:00+05:00')).toBe(false);
        expect(isDateMoment('yyyy-01-23T17:52:00+05:00')).toBe(false);
        expect(isDateMoment('2020-01-23T17:52:00')).toBe(false);
        expect(isDateMoment('2020-01-23T+05:00')).toBe(false);
    });
});
