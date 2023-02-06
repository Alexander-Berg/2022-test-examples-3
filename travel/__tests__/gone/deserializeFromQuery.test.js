import gone from '../../gone';

describe('testing gone filter', () => {
    describe('deserializeFromQuery', () => {
        it('gone: y', () => {
            const result = gone.deserializeFromQuery({gone: 'y'});

            expect(result).toBe(true);
        });

        it('gone: Y', () => {
            const result = gone.deserializeFromQuery({gone: 'Y'});

            expect(result).toBe(true);
        });

        it('gone: magic_value', () => {
            const result = gone.deserializeFromQuery({gone: 'magic_value'});

            expect(result).toBe(false);
        });

        it('no "gone" in param', () => {
            const result = gone.deserializeFromQuery({});

            expect(result).toBe(false);
        });
    });
});
