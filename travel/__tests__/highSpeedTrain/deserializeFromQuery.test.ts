import highSpeedTrain from '../../highSpeedTrain';

describe('highSpeedTrain.deserializeFromQuery', () => {
    it('should return default value, if query does not contain `highspeed` field', () => {
        const result = highSpeedTrain.deserializeFromQuery({});

        expect(result).toEqual(highSpeedTrain.getDefaultValue());
    });

    it('should return an array with single element if `highspeed` field in query is a known transport type key', () => {
        const result = highSpeedTrain.deserializeFromQuery({
            highSpeedTrain: 'Flash',
        });

        expect(result).toEqual(['Flash']);
    });

    it('should return an array from `highspeed` field', () => {
        const result = highSpeedTrain.deserializeFromQuery({
            highSpeedTrain: ['Flash', 'Fox'],
        });

        expect(result).toEqual(['Flash', 'Fox']);
    });

    it('should remove duplicates', () => {
        const result = highSpeedTrain.deserializeFromQuery({
            highSpeedTrain: ['Flash', 'Fox', 'Flash'],
        });

        expect(result).toEqual(['Flash', 'Fox']);
    });
});
