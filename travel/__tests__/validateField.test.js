const validateField = require.requireActual('../utils').validateField;

describe('searchHistory/utils', () => {
    describe('validateField', () => {
        it('empty', () => {
            expect(validateField(null)).not.toBe(true);
        });

        it('empty key', () => {
            expect(validateField({title: 'text'})).not.toBe(true);
        });

        it('empty title', () => {
            expect(validateField({key: 1})).toBe(true);
        });
    });
});
