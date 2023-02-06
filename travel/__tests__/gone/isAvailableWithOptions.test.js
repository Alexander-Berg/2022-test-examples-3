const gone = require.requireActual('../../gone').default;

describe('testing gone filter', () => {
    describe('isAvailableWithOptions', () => {
        it('gone and not gone segments exists', () => {
            const optionState = {
                isGone: 1,
                isNotGoneYet: 2,
            };
            const result = gone.isAvailableWithOptions(optionState);

            expect(result).toBe(true);
        });

        it('gone segments not exists', () => {
            const optionState = {
                isGone: 0,
                isNotGoneYet: 2,
            };
            const result = gone.isAvailableWithOptions(optionState);

            expect(result).toBe(false);
        });

        it('only gone segments', () => {
            const optionState = {
                isGone: 2,
                isNotGoneYet: 0,
            };
            const result = gone.isAvailableWithOptions(optionState);

            expect(result).toBe(false);
        });

        it('zero segments', () => {
            const optionState = {
                isGone: 0,
                isNotGoneYet: 0,
            };
            const result = gone.isAvailableWithOptions(optionState);

            expect(result).toBe(false);
        });
    });
});
