import { validateEmail } from 'shared/helpers/validateEmail/validateEmail';

describe('validateEmail', function () {
    it('works with empty params', function () {
        expect(validateEmail()(undefined)).toBeUndefined();
        expect(validateEmail()('')).toBeUndefined();
    });

    it('works with correct email', function () {
        expect(validateEmail()('test@gmail.com')).toBeUndefined();
    });

    it('works with incorrect email', function () {
        expect(validateEmail()('%$test@gmail.com')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "invalid_email",
                "message": "Invalid e-mail address",
              },
            ]
        `);

        expect(validateEmail()('%$test@gmail')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "invalid_email",
                "message": "Invalid e-mail address",
              },
            ]
        `);

        expect(validateEmail()('test@')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "invalid_email",
                "message": "Invalid e-mail address",
              },
            ]
        `);

        expect(validateEmail()('test')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "invalid_email",
                "message": "Invalid e-mail address",
              },
            ]
        `);
    });

    it('works with custom code', function () {
        expect(validateEmail('lol_kek')('%$test@gmail.com')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "lol_kek",
                "message": "Invalid e-mail address",
              },
            ]
        `);
    });
});
