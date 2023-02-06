import {prepareAspectNameField} from './AspectPage.utils';

describe('prepareAspectNameField', () => {
    test('format string correctly', () => {
        const expected = 'some_aspect_name';

        expect(prepareAspectNameField(expected)).toBe(expected);
        expect(prepareAspectNameField('   some aspect name  ')).toBe(expected);
        expect(prepareAspectNameField('Some Aspect Name')).toBe(expected);
        expect(prepareAspectNameField('some_AspeCt_name')).toBe(expected);
        expect(prepareAspectNameField('some_aspect name ')).toBe(expected);
        expect(prepareAspectNameField('__ somE_aspect __name __')).toBe(
            expected,
        );
        expect(prepareAspectNameField(' __ some_aspect __name __')).toBe(
            expected,
        );
        expect(prepareAspectNameField('__some_aspect __name__')).toBe(expected);
        expect(prepareAspectNameField('__some_aspect-- name__')).toBe(expected);
        expect(prepareAspectNameField('____')).toBe('');
        expect(prepareAspectNameField('    ')).toBe('');
        expect(prepareAspectNameField('__  ')).toBe('');
        expect(prepareAspectNameField('_ _')).toBe('');
        expect(prepareAspectNameField(' _')).toBe('');
    });
});
