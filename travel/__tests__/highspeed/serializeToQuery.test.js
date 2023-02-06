import highspeed from '../../highSpeedTrain';

describe('highspeed filtert test', () => {
    describe('serializeToQuery', () => {
        it('should return an object with field `highspeed`, containing passed value', () => {
            const result = highspeed.serializeToQuery(['Flash', 'Fox']);

            expect(result).toEqual({
                highSpeedTrain: ['Flash', 'Fox'],
            });
        });

        it('should return an empty object', () => {
            const result = highspeed.serializeToQuery();

            expect(result).toEqual({});
        });
    });
});
