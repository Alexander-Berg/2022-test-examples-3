import { getBeautifiedPhone } from '../get-beautified-phone';
import * as helpers from '../get-sanitized-digits';

describe('getBeautifiedPhone', () => {
    let getSanitizedDigits: jest.SpyInstance<string, [string]>;

    const phonePattern = {
        country: '7',
        code: '913',
    };

    beforeEach(() => {
        getSanitizedDigits = jest.spyOn(helpers, 'getSanitizedDigits');
    });

    test('should call getSanitizedDigits with phone', () => {
        const phone = '1';

        getBeautifiedPhone(phone, phonePattern);

        expect(getSanitizedDigits).toHaveBeenCalledTimes(1);
        expect(getSanitizedDigits).toHaveBeenCalledWith(phone);
    });

    describe('if getSanitizedDigits returns empty', () => {
        beforeEach(() => {
            getSanitizedDigits.mockReturnValue('');
        });

        test('should return empty string', () => {
            expect(getBeautifiedPhone('123', phonePattern)).toBe('');
        });
    });

    describe('if getSanitizedDigits returns only country', () => {
        beforeEach(() => {
            getSanitizedDigits.mockReturnValue(phonePattern.country);
        });

        test('should return country', () => {
            expect(getBeautifiedPhone('', phonePattern)).toBe(phonePattern.country);
        });
    });

    describe('if getSanitizedDigits returns country and code', () => {
        beforeEach(() => {
            getSanitizedDigits.mockReturnValue(`${phonePattern.country}${phonePattern.code}`);
        });

        test('should return country and code with spaces between', () => {
            expect(getBeautifiedPhone('', phonePattern))
                .toBe(`${phonePattern.country} ${phonePattern.code}`);
        });
    });

    describe('if getSanitizedDigits returns country, code and phone', () => {
        let phone = '111222';

        beforeEach(() => {
            getSanitizedDigits.mockReturnValue(
                `${phonePattern.country}${phonePattern.code}${phone}`
            );
        });

        test('should return country, code and phone with spaces between', () => {
            expect(getBeautifiedPhone('', phonePattern))
                .toBe(`${phonePattern.country} ${phonePattern.code} ${phone}`);
        });
    });
});
