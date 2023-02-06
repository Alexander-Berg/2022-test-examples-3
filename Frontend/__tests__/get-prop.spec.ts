import { getStringProp, getNumberProp } from '../get-prop';

describe('getProp', () => {
    describe('getStringProp', () => {
        test('should return prop value', () => {
            expect(getStringProp('rgba(0, 0, 0, 1)')).toBe('rgba(0, 0, 0, 1)');
        });

        test('should return undefined in case of undefined prop', () => {
            expect(getStringProp(undefined)).toBe(undefined);
        });

        test('should return undefined in case of empty string', () => {
            expect(getStringProp('')).toBe(undefined);
        });
    });

    describe('getNumberProp', () => {
        test('should return prop value', () => {
            expect(getNumberProp(600)).toBe(600);
        });

        test('should return undefined in case of undefined prop', () => {
            expect(getNumberProp(undefined)).toBe(undefined);
        });

        test('should return undefined in case of NaN', () => {
            expect(getNumberProp(NaN)).toBe(undefined);
        });

        test('should return MAX_SAFE_INTEGER in case of huge value', () => {
            expect(getNumberProp(6.000023422343433e+21)).toBe(Number.MAX_SAFE_INTEGER);
        });
    });
});
