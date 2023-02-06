const {serializeExperimentValue} = require.requireActual('../../experiments');

describe('experiments', () => {
    describe('serializeExperimentValue', () => {
        it('should serialize `true` to `1` string', () => {
            const value = true;
            const experiment = {type: Boolean};

            expect(serializeExperimentValue(value, experiment)).toBe('1');
        });

        it('should serialize `false` to an empty string', () => {
            const value = false;
            const experiment = {type: Boolean};

            expect(serializeExperimentValue(value, experiment)).toBe('');
        });

        it('should serialize any other value by string coercion', () => {
            const value = 1000;
            const experiment = {type: Number};

            expect(serializeExperimentValue(value, experiment)).toBe('1000');
        });
    });
});
