import { isValidNumber } from 'shared/helpers/isValidNumber/isValidNumber';

/* eslint-disable @typescript-eslint/no-magic-numbers */
describe('isValidNumber', function () {
    it('works with empty params', function () {
        expect(isValidNumber()).toBeFalsy();
        expect(isValidNumber('')).toBeFalsy();
        expect(isValidNumber('-')).toBeFalsy();
        expect(isValidNumber(undefined)).toBeFalsy();
        expect(isValidNumber(null)).toBeFalsy();
    });

    it('works with string params', function () {
        expect(isValidNumber('test')).toBeFalsy();
        expect(isValidNumber('10')).toBeTruthy();
        expect(isValidNumber('000')).toBeTruthy();
        expect(isValidNumber('-10')).toBeTruthy();
        expect(isValidNumber('10 000')).toBeTruthy();
        expect(isValidNumber('10.000')).toBeTruthy();
        expect(isValidNumber('10,000')).toBeTruthy();
    });

    it('works with number params', function () {
        expect(isValidNumber(0)).toBeTruthy();
        expect(isValidNumber(-10)).toBeTruthy();
        expect(isValidNumber(10)).toBeTruthy();
        expect(isValidNumber(10000)).toBeTruthy();
        expect(isValidNumber(10.0)).toBeTruthy();
    });
});
