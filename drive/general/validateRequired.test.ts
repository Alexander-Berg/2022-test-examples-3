import { validateRequired } from 'shared/helpers/validateRequired/validateRequired';

describe('validateRequired', function () {
    it('should return undefined', function () {
        expect(validateRequired()(1)).toBeUndefined();
        expect(validateRequired()('test')).toBeUndefined();
        expect(validateRequired()(true)).toBeUndefined();
    });

    it('should return message', function () {
        expect(validateRequired()(null)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "required",
                "message": "Required value",
              },
            ]
        `);

        expect(validateRequired()(undefined)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "required",
                "message": "Required value",
              },
            ]
        `);

        expect(validateRequired()('')).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "required",
                "message": "Required value",
              },
            ]
        `);
    });

    it('works with default message', function () {
        expect(validateRequired()(undefined)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "required",
                "message": "Required value",
              },
            ]
        `);
    });

    it('works with custom props', function () {
        expect(validateRequired('lol kek')(undefined)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "required",
                "message": "lol kek",
              },
            ]
        `);

        expect(validateRequired('lol kek', 'lol_kek')(undefined)).toMatchInlineSnapshot(`
            Array [
              Object {
                "code": "lol_kek",
                "message": "lol kek",
              },
            ]
        `);
    });
});
