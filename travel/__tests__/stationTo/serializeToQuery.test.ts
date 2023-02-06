import stationTo from '../../stationTo';

describe('stationTo', () => {
    describe('serializeToQuery', () => {
        it('Для дефолтного значения вернёт пустой массив', () => {
            const defaultValue = stationTo.getDefaultValue();

            expect(stationTo.serializeToQuery(defaultValue)).toEqual({
                stationTo: [],
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
