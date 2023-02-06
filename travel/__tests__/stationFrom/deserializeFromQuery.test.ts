import stationFrom from '../../stationFrom';

describe('stationFrom', () => {
    describe('deserializeFromQuery', () => {
        it('should return default value if query does not contain stationFrom', () => {
            const result = stationFrom.deserializeFromQuery({});

            expect(result).toEqual(stationFrom.getDefaultValue());
        });

        it('should return array if query contains "stationFrom" field with string', () => {
            const result = stationFrom.deserializeFromQuery({stationFrom: '1'});

            expect(result).toEqual(['1']);
        });

        it('should return array if query contains "stationFrom" field with array', () => {
            const result = stationFrom.deserializeFromQuery({
                stationFrom: ['1', '2'],
            });

            expect(result).toEqual(['1', '2']);
        });
    });
});
