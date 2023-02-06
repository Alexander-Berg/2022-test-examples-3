import highSpeedTrain from '../../highSpeedTrain';

describe('highSpeedTrain.serializeToQuery', () => {
    it('should return an object with field `highspeed`, containing passed value', () => {
        const result = highSpeedTrain.serializeToQuery(['Flash', 'Fox']);

        expect(result).toEqual({
            highSpeedTrain: ['Flash', 'Fox'],
        });
    });

    it('Для дефолтного значения вернёт пустой массив', () => {
        const result = highSpeedTrain.serializeToQuery([]);

        expect(result).toEqual({
            highSpeedTrain: [],
        });
    });
});
