import highspeed from '../../highSpeedTrain';

describe('highspeed filter test', () => {
    describe('isAvailableWithOptions', () => {
        it('is not available with default options', () => {
            const result = highspeed.isAvailableWithOptions([]);

            expect(result).toBe(false);
        });

        it('is available with valid options', () => {
            const options = ['Fox'];
            const result = highspeed.isAvailableWithOptions(options);

            expect(result).toBe(true);
        });
    });
});
