import { getUserDocumentPassportField } from 'entities/User/helpers/getUserDocumentPassportField/getUserDocumentPassportField';

describe('getUserDocumentPassportField', function () {
    it('works with empty params', function () {
        expect(getUserDocumentPassportField(undefined as any)).toMatchInlineSnapshot(`"—"`);
    });

    it('works with filled params', function () {
        expect(getUserDocumentPassportField('first_name')).toMatchInlineSnapshot(`"Name"`);
        expect(getUserDocumentPassportField('birth_place')).toMatchInlineSnapshot(`"—"`);
    });
});
