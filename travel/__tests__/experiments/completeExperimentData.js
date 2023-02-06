const {completeExperimentData} = require.requireActual('../../experiments');

describe('experiments', () => {
    describe('experiments', () => {
        it('should return experiment data as is if its type is not Boolean', () => {
            const experiment = {
                type: Number,
                values: [{value: 1, percentage: 10}],
                defaultValue: 0,
            };

            const result = completeExperimentData(experiment);

            expect(result).toBe(experiment);
            expect(result).toEqual({
                type: Number,
                values: [{value: 1, percentage: 10}],
                defaultValue: 0,
            });
        });

        it('should complete experiment data with `values` and `defaultValue` fields if its type is Boolean', () => {
            const experiment = {
                type: Boolean,
                percentage: 20,
            };

            expect(completeExperimentData(experiment)).toEqual({
                type: Boolean,
                percentage: 20,
                defaultValue: false,
                values: [{value: true, percentage: 20}],
            });
        });
    });
});
