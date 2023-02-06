const {meetExperimentDependencies} = require('../../experiments');

describe('experiments', () => {
    describe('meetExperimentDependencies', () => {
        it('should return true if flags contain an appropriate value for each dependency', () => {
            const dependencies = {
                __exp1: [true],
                __exp2: ['foo', 'bar'],
            };

            const flags = {
                __exp1: true,
                __exp2: 'foo',
            };

            expect(meetExperimentDependencies(dependencies, flags)).toBe(true);
        });

        it('should return false if flags do not contain an appropriate value for at least one dependency ', () => {
            const dependencies = {
                __exp1: [true],
                __exp2: [1, 2],
            };

            const flags = {
                __exp1: true,
                __exp2: 3,
            };

            expect(meetExperimentDependencies(dependencies, flags)).toBe(false);
        });
    });
});
