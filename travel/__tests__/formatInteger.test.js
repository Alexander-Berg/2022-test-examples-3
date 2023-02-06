const formatInteger = require.requireActual('../formatInteger').default;

describe('formatInteger', () => {
    it(`should return a string representation of a passed integer,
        if it has less than 4 digits`, () => {
        expect(formatInteger(12)).toBe('12');
        expect(formatInteger(123)).toBe('123');
    });

    it(`should return a string representation of a passed integer
        with digits grouped by 3, if it has 4 digits or more`, () => {
        expect(formatInteger(1234)).toBe('1 234');
        expect(formatInteger(123456789)).toBe('123 456 789');
    });
});
