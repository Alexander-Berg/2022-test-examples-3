import hasValue from '../hasValue';

describe('hasValue', () => {
    it('Значения считаются пустыми', () => {
        expect(hasValue(null)).toBe(false);
        expect(hasValue('')).toBe(false);
        expect(hasValue(undefined)).toBe(false);
        expect(hasValue([])).toBe(false);
        expect(hasValue([null])).toBe(false);
        expect(hasValue({})).toBe(false);
    });

    it('Значения являются валидными', () => {
        expect(hasValue(0)).toBe(true);
        expect(hasValue('0')).toBe(true);
        expect(hasValue({a: 1})).toBe(true);
        expect(hasValue([1])).toBe(true);
        expect(hasValue([1, null])).toBe(true);
    });
});
