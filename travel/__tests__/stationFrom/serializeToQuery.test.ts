import stationFrom from '../../stationFrom';

describe('stationFrom', () => {
    describe('serializeToQuery', () => {
        it('Для дефолтного значения вернёт пустой массив', () => {
            const defaultValue = stationFrom.getDefaultValue();

            expect(stationFrom.serializeToQuery(defaultValue)).toEqual({
                stationFrom: [],
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
