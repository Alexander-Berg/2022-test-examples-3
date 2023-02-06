import { validatePositiveNumber } from 'shared/helpers/validatePositiveNumber/validatePositiveNumber';

describe('validatePositiveNumber', function () {
    it('works with empty params', function () {
        expect(validatePositiveNumber()('', {})).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "required",
                "message": "Required value",
              },
            ]
        `);

        expect(validatePositiveNumber()(1, {})).toMatchInlineSnapshot(`undefined`);

        expect(validatePositiveNumber()(-1, {})).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "should_more",
                "message": "Value should be more than 0",
              },
            ]
        `);
    });

    it('works with custom message', function () {
        expect(validatePositiveNumber('lol kek')('', {})).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "required",
                "message": "lol kek",
              },
            ]
        `);
    });
});
