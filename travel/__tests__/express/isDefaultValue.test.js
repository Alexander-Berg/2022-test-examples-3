import express from '../../express';

describe('express', () => {
    describe('isDefaultValue', () => {
        it('show segments with express only', () => {
            const result = express.isDefaultValue(true);

            expect(result).toBe(false);
        });

        it('show all segments', () => {
            const result = express.isDefaultValue(false);

            expect(result).toBe(true);
        });
    });
});
