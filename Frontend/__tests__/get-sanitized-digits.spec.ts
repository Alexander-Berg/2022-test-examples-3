import { getSanitizedDigits } from '../get-sanitized-digits';

describe('getSanitizedDigits', () => {
    test('should remove all non-digit characters', () => {
        expect(getSanitizedDigits('alpha1')).toBe('1');
    });

    test('should remove all unused spaces', () => {
        expect(getSanitizedDigits(' 1 ')).toBe('1');
    });
});
