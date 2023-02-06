import boolify from 'utilities/boolean/boolify';

describe('boolify(value)', () => {
    test('Должен вернуть true', () => {
        expect(boolify(true)).toBe(true);
        expect(boolify(1)).toBe(true);
        expect(boolify(5)).toBe(true);
        expect(boolify('1')).toBe(true);
        expect(boolify('true')).toBe(true);
        expect(boolify('TRUE')).toBe(true);
        expect(boolify('foobar')).toBe(true);
        expect(boolify({})).toBe(true);
        expect(boolify({foo: 'bar'})).toBe(true);
    });

    test('Должен вернуть false', () => {
        expect(boolify(false)).toBe(false);
        expect(boolify(0)).toBe(false);
        expect(boolify('0')).toBe(false);
        expect(boolify('false')).toBe(false);
        expect(boolify('FALSE')).toBe(false);
        expect(boolify(null)).toBe(false);
        expect(boolify(undefined)).toBe(false);
    });
});
