import { hasHotFix } from './force-update';

jest.mock('./rum', () => {});

const compareCases: Array<[string, string]> = [
    ['1.0.1', '1.0.0'],
    ['1.1.0', '1.0.0'],
    ['2.0.0', '1.0.0'],
    ['2.0.0', '1.0.1'],
    ['1.1.0', '1.0.1'],
    ['1.1.1', '1.0.1'],
    ['1.1.2', '1.1.1'],
    ['1.2.1', '1.1.1'],
];

describe('hasHotFix()', () => {
    it('should return false without current version', () => {
        expect(hasHotFix(undefined, '1.0.0')).toBe(false);
        expect(hasHotFix('', '1.0.0')).toBe(false);
    });

    it('should return false without min version', () => {
        expect(hasHotFix('1.0.0', undefined)).toBe(false);
        expect(hasHotFix('1.0.0', '')).toBe(false);
    });

    it('should return false if failed to validate current version', () => {
        expect(hasHotFix('1.0.a', '1.0.0')).toBe(false);
        expect(hasHotFix('1.0.0.0', '1.0.0')).toBe(false);
        expect(hasHotFix('1.0.0-build.0', '1.0.0')).toBe(false);
        expect(hasHotFix('1.0', '1.0.0')).toBe(false);
        expect(hasHotFix('1.0.', '1.0.0')).toBe(false);
        expect(hasHotFix('100', '1.0.0')).toBe(false);
        expect(hasHotFix('100', '1.0.0')).toBe(false);
    });

    it('should throw error if failed to validate min version', () => {
        expect(() => hasHotFix('1.0.0', '1.0.0.0')).toThrowError('Invalid minimal app version format: 1.0.0.0');
        expect(() => hasHotFix('1.0.0', '1.0.0.a')).toThrowError('Invalid minimal app version format: 1.0.0.a');
        expect(() => hasHotFix('1.0.0', '1.0.0-build.1')).toThrowError(
            'Invalid minimal app version format: 1.0.0-build.1'
        );
        expect(() => hasHotFix('1.0.0', '1.0.')).toThrowError('Invalid minimal app version format: 1.0.');
        expect(() => hasHotFix('1.0.0', '1.0')).toThrowError('Invalid minimal app version format: 1.0');
        expect(() => hasHotFix('1.0.0', '100')).toThrowError('Invalid minimal app version format: 100');
    });

    const negativeCases = [['1.0.0', '1.0.0'], ...compareCases];
    it.each(negativeCases)('should return false if current version is %s and min version is %s', (current, min) => {
        expect(hasHotFix(current, min)).toBe(false);
    });

    const positiveCases = compareCases.map(item => [item[1], item[0]]);
    it.each(positiveCases)('should return true if current version is %s and min version is %s', (current, min) => {
        expect(hasHotFix(current, min)).toBe(true);
    });
});
