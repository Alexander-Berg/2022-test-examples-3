jest.dontMock('../../baseFilterManager');

const stationFrom = require.requireActual('../../stationFrom').default;
const stationTo = require.requireActual('../../stationTo').default;

const segment = {
    stationFrom: {
        id: 1,
        title: 'Станция 9 и три четверти',
    },
    stationTo: {
        id: 2,
        title: 'Хогвартс',
    },
};

describe('stationFrom', () => {
    describe('apply', () => {
        it('should return true for default', () => {
            const defaultFilterValue = stationFrom.getDefaultValue();
            const result = stationFrom.apply(defaultFilterValue, segment);

            expect(result).toBe(true);
        });

        it('should return true if value contains station id', () => {
            expect(stationFrom.apply(['1'], segment)).toBe(true);
        });

        it('should return false if value does not contain station id', () => {
            expect(stationFrom.apply(['2'], segment)).toBe(false);
        });
    });
});

describe('stationTo', () => {
    describe('apply', () => {
        it('should return true for default', () => {
            const defaultFilterValue = stationTo.getDefaultValue();
            const result = stationTo.apply(defaultFilterValue, segment);

            expect(result).toBe(true);
        });

        it('should return true if value contains station id', () => {
            expect(stationTo.apply(['2'], segment)).toBe(true);
        });

        it('should return false if value does not contain station id', () => {
            expect(stationTo.apply(['1'], segment)).toBe(false);
        });
    });
});
