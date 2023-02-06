import { validatePhoneNumber } from 'shared/helpers/validatePhoneNumber/validatePhoneNumber';

describe('validatePhoneNumber', function () {
    it('works with empty params', function () {
        expect(validatePhoneNumber()(undefined)).toBeUndefined();
        expect(validatePhoneNumber()('')).toBeUndefined();
    });

    it('works with correct params', function () {
        expect(validatePhoneNumber()('+420100000000')).toMatchInlineSnapshot(`undefined`);
    });

    it('works with incorrect params', function () {
        expect(validatePhoneNumber()('+42010')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "invalid_phone_number",
                "message": "Invalid phone number format",
              },
            ]
        `);

        expect(validatePhoneNumber()('420100000000')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "invalid_phone_number",
                "message": "Invalid phone number format",
              },
            ]
        `);

        expect(validatePhoneNumber()('lolkek')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "invalid_phone_number",
                "message": "Invalid phone number format",
              },
            ]
        `);
    });

    it('works with custom code', function () {
        expect(validatePhoneNumber('lol_kek')('+42010')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "lol_kek",
                "message": "Invalid phone number format",
              },
            ]
        `);
    });
});
