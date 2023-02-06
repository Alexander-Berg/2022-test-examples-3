import stationTo from '../../stationTo';

describe('stationTo', () => {
    describe('deserializeFromQuery', () => {
        it('should return default value if query does not contain "stationTo"', () => {
            const result = stationTo.deserializeFromQuery({});

            expect(result).toEqual(stationTo.getDefaultValue());
        });

        it('should return array if query contains "stationTo" field with string', () => {
            const result = stationTo.deserializeFromQuery({stationTo: '1'});

            expect(result).toEqual(['1']);
        });

        it('should return array if query contains "stationTo" field with array', () => {
            const result = stationTo.deserializeFromQuery({
                stationTo: ['1', '2'],
            });

            expect(result).toEqual(['1', '2']);
        });
    });
});
