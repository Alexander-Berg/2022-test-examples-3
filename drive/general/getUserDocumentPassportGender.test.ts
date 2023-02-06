import { getUserDocumentPassportGender } from 'entities/User/helpers/getUserDocumentPassportGender/getUserDocumentPassportGender';

describe('getUserDocumentPassportGender', function () {
    it('works with empty params', function () {
        expect(getUserDocumentPassportGender(undefined)).toMatchInlineSnapshot(`"—"`);
    });

    it('works with filled params', function () {
        expect(getUserDocumentPassportGender('male')).toMatchInlineSnapshot(`"Male"`);
        expect(getUserDocumentPassportGender('his')).toMatchInlineSnapshot(`"—"`);
    });
});
