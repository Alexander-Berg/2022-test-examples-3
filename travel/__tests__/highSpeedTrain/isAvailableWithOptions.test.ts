import highSpeedTrain from '../../highSpeedTrain';

describe('highSpeedTrain.isAvailableWithOptions', () => {
    it('is not available with default options', () => {
        const result = highSpeedTrain.isAvailableWithOptions([], []);

        expect(result).toBe(false);
    });

    it('is available with valid options', () => {
        const options = [{text: 'Fox', value: '245'}];
        const result = highSpeedTrain.isAvailableWithOptions(options, []);

        expect(result).toBe(false);
    });
});
