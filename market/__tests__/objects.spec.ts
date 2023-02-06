import {Objects} from '../objects';

describe('toCamelCase(null)', () => {
    it('should return null', () => {
        expect(Objects.toCamelCase(null)).toEqual(null);
    });
});

describe('toCamelCase(undefined)', () => {
    it('should return undefined', () => {
        expect(Objects.toCamelCase(undefined)).toEqual(undefined);
    });
});

describe('toCamelCase int literal', () => {
    it('should return int literal', () => {
        expect(Objects.toCamelCase(1)).toEqual(1);
    });
});

describe('toCamelCase string literal', () => {
    it('should return string literal', () => {
        expect(Objects.toCamelCase('abc')).toEqual('abc');
    });
});

describe('toCamelCase float literal', () => {
    const eps = 1.0e-6;

    it('should return float literal', () => {
        expect(Objects.toCamelCase(1.5)).toBeCloseTo(1.5, eps);
    });
});

describe('toCamelCase array', () => {
    it('should return array', () => {
        expect(Objects.toCamelCase([1, 'b', 2.5])).toEqual([1, 'b', 2.5]);
    });
});

describe('toCamelCase array with Objects', () => {
    it('should return array with tranformed objects', () => {
        expect(Objects.toCamelCase([1, 'b', {country_code: 123}, 2.5])).toEqual([1, 'b', {countryCode: 123}, 2.5]);
    });
});

describe('toCamelCase already camelCase', () => {
    it('should return unchanged object structure', () => {
        expect(Objects.toCamelCase({camelCaseField: 1, camelCaseAnother: 2})).toEqual({
            camelCaseField: 1,
            camelCaseAnother: 2,
        });
    });
});

describe('toCamelCase transform', () => {
    it('should return shallow transformed object', () => {
        expect(Objects.toCamelCase({not_camel_cased: 1, another_field: 2})).toEqual({
            notCamelCased: 1,
            anotherField: 2,
        });
    });
});

describe('toCamelCase deep transform', () => {
    it('should return deep transformed object', () => {
        expect(Objects.toCamelCase({not_camel_cased: {another_field: 2}})).toEqual({notCamelCased: {anotherField: 2}});
    });
});

describe('to underscore deep transform', () => {
    it('should return deep transformed object', () => {
        expect(Objects.toUnderscore({camelCased: {AnotherField: 2}})).toEqual({
            camel_cased: {another_field: 2},
        });
    });
});
