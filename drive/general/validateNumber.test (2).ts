import { validateNumber } from 'shared/helpers/validateNumber/validateNumber';

/* eslint-disable @typescript-eslint/no-magic-numbers */
describe('validateNumber', function () {
    it('works with empty params', function () {
        expect(validateNumber({ min: 0 })(undefined)).toBeUndefined();
        expect(validateNumber({ min: 0 })(null)).toBeUndefined();
        expect(validateNumber({ min: 0 })('')).toBeUndefined();
    });

    it('works with 0', function () {
        expect(validateNumber({})(0)).toBeUndefined();
    });

    it('works with min, max params', function () {
        expect(validateNumber({ min: 5, max: 10 })(1)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "should_between",
                "message": "Value should be between 5 and 10",
              },
            ]
        `);
    });

    it('works with min param', function () {
        expect(validateNumber({ min: 5 })(1)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "should_more",
                "message": "Value should be more than 5",
              },
            ]
        `);
    });

    it('works with max param', function () {
        expect(validateNumber({ max: 5 })(10)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "should_less",
                "message": "Value should be less than 5",
              },
            ]
        `);
    });
});
