import {getActiveFiltersCount} from './getActiveFiltersCount';

describe('getActiveFiltersCount', () => {
    test('with empty object', () => {
        expect(getActiveFiltersCount({})).toBe(0);
    });
    test('with default epmty values', () => {
        expect(
            getActiveFiltersCount({
                confidentOnly: 'false',
                raw: 'false',
                endDate: undefined,
                startDate: undefined,
                averagingPeriod: '',
            }),
        ).toBe(0);
    });

    test('with active values', () => {
        expect(
            getActiveFiltersCount({
                confidentOnly: 'true',
                raw: 'true',
                endDate: 'some_string',
                startDate: 'some_string',
                averagingPeriod: '1D',
            }),
        ).toBe(5);
    });

    test('with mixed values', () => {
        expect(
            getActiveFiltersCount({
                confidentOnly: 'true',
                raw: 'true',
                averagingPeriod: '1D',
            }),
        ).toBe(3);
    });
});
