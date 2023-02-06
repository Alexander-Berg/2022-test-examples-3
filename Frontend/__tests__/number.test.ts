import { strictParseInt } from '../number';

describe('strictParseInt', () => {
    test('should return undefined if non-string or non-number value', () => {
        expect(strictParseInt({})).toBe(undefined);
        expect(strictParseInt([])).toBe(undefined);
        expect(strictParseInt(() => {})).toBe(undefined);
    });

    test('should return undefined if NaN value', () => {
        expect(strictParseInt(NaN)).toBe(undefined);
    });

    test('should return undefined if Infinite value', () => {
        expect(strictParseInt(Infinity)).toBe(undefined);
        expect(strictParseInt(-Infinity)).toBe(undefined);
    });

    test('should return undefined if float value', () => {
        expect(strictParseInt(4.55)).toBe(undefined);
    });

    test('should return undefined if empty string value', () => {
        expect(strictParseInt('')).toBe(undefined);
    });

    test('should return undefined if non-only-digit string value', () => {
        expect(strictParseInt('0dd')).toBe(undefined);
    });

    test('should return undefined if float-number string value', () => {
        expect(strictParseInt('9.22')).toBe(undefined);
    });

    test('should return undefined if exp-number string value', () => {
        expect(strictParseInt('1e5')).toBe(undefined);
    });

    test('should return undefined if non-trimmed string value', () => {
        expect(strictParseInt('  5  ')).toBe(undefined);
        expect(strictParseInt('5 \n ')).toBe(undefined);
    });

    test('should return integer if integer value', () => {
        expect(strictParseInt(4)).toBe(4);
        expect(strictParseInt(0)).toBe(0);
        expect(strictParseInt(-4)).toBe(-4);
    });

    test('should return integer if only-digit string value', () => {
        expect(strictParseInt('4')).toBe(4);
        expect(strictParseInt('0')).toBe(0);
        expect(strictParseInt('-4')).toBe(-4);
    });
});
