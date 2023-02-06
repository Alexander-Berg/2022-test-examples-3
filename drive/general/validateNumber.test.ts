import { validateNumber } from './validateNumber';

describe('validateNumber', function () {
    it('works with empty params', function () {
        const validator = validateNumber({ min: 0 });

        expect(validator(undefined)).toBeUndefined();
        expect(validator(null)).toBeUndefined();
        expect(validator('')).toBeUndefined();
    });

    it('works with min, max params', function () {
        expect(validateNumber({ min: 5, max: 10 })(1)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "should_between",
                "message": "Value must be between 5 and 10",
              },
            ]
        `);
    });

    it('works with min param', function () {
        expect(validateNumber({ min: 5 })(1)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "should_more",
                "message": "Value must be more then 5",
              },
            ]
        `);
    });

    it('works with max param', function () {
        expect(validateNumber({ max: 5 })(10)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "should_less",
                "message": "Value must be less then 5",
              },
            ]
        `);
    });
});
