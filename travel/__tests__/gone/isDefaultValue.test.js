const gone = require.requireActual('../../gone').default;

describe('testing gone filter', () => {
    describe('isDefaultValue', () => {
        it('true is default value?', () => {
            const result = gone.isDefaultValue(true);

            expect(result).toBe(false);
        });

        it('false is default value?', () => {
            const result = gone.isDefaultValue(false);

            expect(result).toBe(true);
        });
    });
});
