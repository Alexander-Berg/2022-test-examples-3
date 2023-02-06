jest.dontMock('../../baseFilterManager');

const stationFrom = require.requireActual('../../stationFrom').default;
const stationTo = require.requireActual('../../stationTo').default;

describe('stationFrom', () => {
    describe('serializeToQuery', () => {
        it('should return an empty object', () => {
            const defaultValue = stationFrom.getDefaultValue();

            expect(stationFrom.serializeToQuery(defaultValue)).toEqual({});
        });

        it('should return object with filled stationFrom string', () => {
            const result = stationFrom.serializeToQuery('2');

            expect(result).toEqual({
                stationFrom: '2',
            });
        });

        it('should return object with filled stationFrom array', () => {
            const result = stationFrom.serializeToQuery(['1', '2']);

            expect(result).toEqual({
                stationFrom: ['1', '2'],
            });
        });
    });
});

describe('stationTo', () => {
    describe('serializeToQuery', () => {
        it('should return an empty object', () => {
            const defaultValue = stationTo.getDefaultValue();

            expect(stationTo.serializeToQuery(defaultValue)).toEqual({});
        });

        it('should return object with filled stationTo string', () => {
            const result = stationTo.serializeToQuery('2');

            expect(result).toEqual({
                stationTo: '2',
            });
        });

        it('should return object with filled stationTo array', () => {
            const result = stationTo.serializeToQuery(['1', '2']);

            expect(result).toEqual({
                stationTo: ['1', '2'],
            });
        });
    });
});
