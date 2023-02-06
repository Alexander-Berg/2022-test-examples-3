import { validateUserInput } from 'features/CarBooking/helpers/validateUserInput/validateUserInput';

describe('validateUserInput', function () {
    it('works with empty params', function () {
        expect(validateUserInput()(undefined, {})).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "required",
                "message": "Enter customer details",
              },
            ]
        `);
    });

    it('works with filled params', function () {
        expect(
            validateUserInput()(
                {
                    first_name: 'test',
                    last_name: 'test',
                    phone_number: '+375291000000',
                },

                {},
            ),
        ).toMatchInlineSnapshot(`Array []`);
    });

    it('works with partial params', function () {
        expect(
            validateUserInput()(
                {
                    first_name: 'test',
                    last_name: '',
                    phone_number: '',
                },

                {},
            ),
        ).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "last_name",
                "message": "Enter customer surname",
              },
              Object {
                "code": "phone_number",
                "message": "Enter customer phone number",
              },
            ]
        `);

        expect(
            validateUserInput()(
                {
                    first_name: '',
                    last_name: 'test',
                    phone_number: '',
                },

                {},
            ),
        ).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "first_name",
                "message": "Enter customer name",
              },
              Object {
                "code": "phone_number",
                "message": "Enter customer phone number",
              },
            ]
        `);

        expect(
            validateUserInput()(
                {
                    first_name: '',
                    last_name: '',
                    phone_number: '+375291000000',
                },

                {},
            ),
        ).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "first_name",
                "message": "Enter customer name",
              },
              Object {
                "code": "last_name",
                "message": "Enter customer surname",
              },
            ]
        `);
    });

    it('works with incorrect params', function () {
        expect(
            validateUserInput()(
                {
                    first_name: 'test',
                    last_name: 'test',
                    phone_number: '+37529100000',
                    email: 'test',
                },

                {},
            ),
        ).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "phone_number",
                "message": "Invalid phone number format",
              },
              Object {
                "code": "email",
                "message": "Invalid e-mail address",
              },
            ]
        `);
    });
});
