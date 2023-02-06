import express from '../../express';

describe('express', () => {
    describe('isAvailableWithOptions', () => {
        it('withExpress: true, withoutExpress: true', () => {
            const available = express.isAvailableWithOptions({
                withExpress: true,
                withoutExpress: true,
            });

            expect(available).toBe(true);
        });

        it('withExpress: true, withoutExpress: false', () => {
            const available = express.isAvailableWithOptions({
                withExpress: true,
                withoutExpress: false,
            });

            expect(available).toBe(true);
        });

        it('withExpress: false, withoutExpress: true', () => {
            const available = express.isAvailableWithOptions({
                withExpress: false,
                withoutExpress: true,
            });

            expect(available).toBe(false);
        });

        it('withExpress: false, withoutExpress: false', () => {
            const available = express.isAvailableWithOptions({
                withExpress: false,
                withoutExpress: false,
            });

            expect(available).toBe(false);
        });
    });
});
