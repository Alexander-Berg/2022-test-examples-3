import {FREE_PHONE_REGEXP, getPhoneValidationRegExp} from '../validationRules';

describe('Валидация телефона в бронировании поездов', () => {
    const phoneRegex = new RegExp(FREE_PHONE_REGEXP);

    test('Проверяет наличие 10 цифр, остальное в свободном форме', () => {
        // Valid case
        expect(phoneRegex.test('9998887766')).toBe(true);
        expect(phoneRegex.test('89998887766')).toBe(true);
        expect(phoneRegex.test('+89998887766')).toBe(true);
        expect(phoneRegex.test('8 999 888 77 66')).toBe(true);
        expect(phoneRegex.test('8-999-888-77-66')).toBe(true);
        expect(phoneRegex.test('8_999_888_77_66')).toBe(true);
        expect(phoneRegex.test('+8 999 888 77 66')).toBe(true);
        expect(phoneRegex.test('+  8  999  888  77  66')).toBe(true);
        expect(phoneRegex.test('  8 999 888 77 66  ')).toBe(true);
        expect(
            phoneRegex.test(
                'Телефон у китайца на рынке брал, Galaxy-IPhone-50 (!@#$%^&*()_+-=), а номер начинается с 8999888, а потом, щас вспомню, заканчивается на 77 и ещё 66. Вот такой номер',
            ),
        ).toBe(true);

        // Invalid cases
        expect(phoneRegex.test('')).toBe(false);
        expect(phoneRegex.test('123')).toBe(false);
        expect(phoneRegex.test('899988877')).toBe(false);
        expect(phoneRegex.test('00 888 77 66')).toBe(false);
        expect(phoneRegex.test('0-888-77-66')).toBe(false);
        expect(phoneRegex.test('+008887766')).toBe(false);
        expect(phoneRegex.test('+80 888 77 66')).toBe(false);
        expect(phoneRegex.test('+008887766')).toBe(false);
        expect(phoneRegex.test('+8 8888 77 66')).toBe(false);
        expect(
            phoneRegex.test(
                'Номер, блин забыл кажется восемь восемьсот 888770000, вроде всё',
            ),
        ).toBe(false);
    });

    test('getPhoneValidationRegexp', () => {
        const limitedRegExp = new RegExp(getPhoneValidationRegExp(2, 15));
        const unlimitedRefExp = new RegExp(getPhoneValidationRegExp(2));

        expect(limitedRegExp.test('11')).toBe(true);
        expect(limitedRegExp.test('99')).toBe(true);
        expect(limitedRegExp.test('123456')).toBe(true);
        expect(limitedRegExp.test('123456789012345')).toBe(true);
        expect(limitedRegExp.test('+  8  999  888  77  66')).toBe(true);

        expect(unlimitedRefExp.test('11')).toBe(true);
        expect(
            unlimitedRefExp.test(
                '+  8  999  888  77  66 13412431234124124124    124124',
            ),
        ).toBe(true);

        expect(limitedRegExp.test('1')).toBe(false);
        expect(limitedRegExp.test('1234567890123456')).toBe(false);
        expect(limitedRegExp.test('+  9  ')).toBe(false);
        expect(limitedRegExp.test('+  123 any  45 any 67890123456  ')).toBe(
            false,
        );
        expect(unlimitedRefExp.test('1')).toBe(false);
        expect(unlimitedRefExp.test('+  9 ')).toBe(false);
    });
});
