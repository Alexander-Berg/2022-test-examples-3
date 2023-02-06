import express from '../../express';

describe('express', () => {
    describe('deserializeFromQuery', () => {
        it('express: y', () => {
            const value = express.deserializeFromQuery({express: 'y'});

            expect(value).toBe(true);
        });

        it('express: Y', () => {
            const value = express.deserializeFromQuery({express: 'Y'});

            expect(value).toBe(true);
        });

        it('express: unknown', () => {
            const value = express.deserializeFromQuery({express: 'unknown'});

            expect(value).toBe(false);
        });

        it('no "express" param', () => {
            const value = express.deserializeFromQuery({foo: 'bar'});

            expect(value).toBe(false);
        });
    });
});
