import { getUserDocumentDriverLicenseField } from 'entities/User/helpers/getUserDocumentDriverLicenseField/getUserDocumentDriverLicenseField';

describe('getUserDocumentDriverLicenseField', function () {
    it('works with empty params', function () {
        expect(getUserDocumentDriverLicenseField(undefined as any)).toMatchInlineSnapshot(`"—"`);
    });

    it('works with filled params', function () {
        expect(getUserDocumentDriverLicenseField('first_name')).toMatchInlineSnapshot(`"Name"`);
        expect(getUserDocumentDriverLicenseField('middle_name')).toMatchInlineSnapshot(`"—"`);
    });
});
